package com.moexdelta.moexselect.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.enums.Goal;
import com.moexdelta.moexselect.enums.Horizon;
import com.moexdelta.moexselect.enums.InvestmentScenario;
import com.moexdelta.moexselect.enums.RiskProfile;
import com.moexdelta.moexselect.enums.UserProfileType;

@Service
public class ExplanationGenerationService {

    private static final int MAX_LENGTH = 520;
    private static final List<String> FORBIDDEN_PHRASES = List.of(
        "купите",
        "продавайте",
        "гарантированно",
        "точно вырастет",
        "лучший инструмент",
        "рекомендуем",
        "советуем",
        "инвестиционная рекомендация",
        "покуп",
        "продаж"
    );

    private final WebClient.Builder webClientBuilder;
    private final boolean llmEnabled;
    private final String llmProvider;
    private final String llmModel;
    private final String llmBaseUrl;
    private final int llmTimeoutSeconds;

    public ExplanationGenerationService(
        WebClient.Builder webClientBuilder,
        ExplanationService explanationService,
        @Value("${LLM_EXPLANATIONS_ENABLED:false}") boolean llmEnabled,
        @Value("${LLM_PROVIDER:ollama}") String llmProvider,
        @Value("${LLM_MODEL:qwen2.5:3b-instruct}") String llmModel,
        @Value("${LLM_BASE_URL:http://localhost:11434}") String llmBaseUrl,
        @Value("${LLM_TIMEOUT_SECONDS:8}") int llmTimeoutSeconds
    ) {
        this.webClientBuilder = webClientBuilder;
        this.llmEnabled = llmEnabled;
        this.llmProvider = llmProvider;
        this.llmModel = llmModel;
        this.llmBaseUrl = llmBaseUrl;
        this.llmTimeoutSeconds = Math.max(1, llmTimeoutSeconds);
    }

    public String generateProfileSummary(
        RecommendationRequest request,
        UserProfileType profile,
        InvestmentScenario scenario,
        int instrumentsCount
    ) {
        if (llmEnabled) {
            var llmSummary = switch (llmProvider.toLowerCase(Locale.ROOT)) {
                case "mock" -> profileTemplate(request, profile, scenario);
                case "ollama" -> ollamaProfileSummary(request, profile, scenario, instrumentsCount);
                default -> null;
            };
            llmSummary = sanitize(llmSummary);
            if (isSafe(llmSummary)) {
                return trimToLimit(llmSummary.strip());
            }
        }
        return profileTemplate(request, profile, scenario);
    }

    private String ollamaProfileSummary(
        RecommendationRequest request,
        UserProfileType profile,
        InvestmentScenario scenario,
        int instrumentsCount
    ) {
        try {
            var client = webClientBuilder.baseUrl(llmBaseUrl).build();
            var response = client.post()
                .uri("/api/generate")
                .bodyValue(Map.of(
                    "model", llmModel,
                    "prompt", profilePrompt(request, profile, scenario, instrumentsCount),
                    "stream", false,
                    "options", Map.of(
                        "temperature", 0.35,
                        "num_predict", 95,
                        "num_ctx", 768
                    )
                ))
                .retrieve()
                .bodyToMono(OllamaGenerateResponse.class)
                .timeout(Duration.ofSeconds(llmTimeoutSeconds))
                .block();
            return response == null ? null : response.response();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String profilePrompt(
        RecommendationRequest request,
        UserProfileType profile,
        InvestmentScenario scenario,
        int instrumentsCount
    ) {
        return """
            Ты работаешь в составе сервиса MOEX Select.
            Кратко опиши инвестиционный профиль пользователя на основе анкеты.

            Не перечисляй параметры напрямую.
            Не повторяй слова пользователя буквально.
            Не пиши "вы выбрали".
            Интерпретируй смысл параметров и опиши инвестиционный стиль.
            Без инвестиционных советов, прогнозов и обещаний доходности.
            Не используй слова: купите, продавайте, гарантированно, точно вырастет, лучший инструмент, рекомендуем.
            Верни только итоговый текст без заголовков.
            Язык: русский. Длина: 2-3 коротких предложения.

            Хорошие примеры:
            Сформирован профиль инвестора, ориентированного на сохранение капитала и получение стабильного денежного потока. Приоритет отдается более предсказуемым инструментам и долгосрочному подходу к управлению средствами.
            Подбор ориентирован на долгосрочное увеличение стоимости капитала при умеренном уровне риска. Основное внимание уделяется инструментам, способным поддерживать устойчивую динамику на продолжительном временном горизонте.
            Сформирован агрессивный инвестиционный профиль, ориентированный на поиск краткосрочных рыночных возможностей. Основное внимание уделяется инструментам, быстро реагирующим на изменения рыночной конъюнктуры.

            Входные данные:
            investmentGoal=%s
            riskProfile=%s
            investmentHorizon=%s
            userProfile=%s
            scenario=%s
            budgetBand=%s
            selectedAssetClasses=%s
            matchedInstruments=%d
            """.formatted(
            goalLabel(request.goal()),
            riskLabel(request.riskProfile()),
            horizonLabel(request.horizon()),
            profileLabel(profile),
            scenarioLabel(scenario),
            budgetBand(request.budget()),
            assetClassLabels(request.assetClasses()),
            instrumentsCount
        );
    }

    private String profileTemplate(
        RecommendationRequest request,
        UserProfileType profile,
        InvestmentScenario scenario
    ) {
        if (scenario == InvestmentScenario.STABLE_INCOME || request.goal() == Goal.STABLE_INCOME) {
            if (request.riskProfile() == RiskProfile.LOW) {
                return "Сформирован профиль инвестора, ориентированного на устойчивый денежный поток и аккуратное управление капиталом. Приоритет отдается более предсказуемым инструментам и снижению влияния резких рыночных колебаний.";
            }
            return "Профиль отражает стремление к регулярному доходу при готовности принимать умеренные рыночные колебания. Основной акцент сделан на инструментах, где важны понятная структура выплат и достаточная ликвидность.";
        }
        if (scenario == InvestmentScenario.CAPITAL_PRESERVATION || request.goal() == Goal.CAPITAL_PRESERVATION) {
            return "Сформирован осторожный профиль, в котором ключевую роль играет сохранность капитала. Подбор ориентирован на инструменты с более предсказуемым поведением и меньшей чувствительностью к резким изменениям рынка.";
        }
        if (scenario == InvestmentScenario.SPECULATION || request.goal() == Goal.SPECULATION) {
            if (request.assetClasses().stream().anyMatch(assetClass -> assetClass == AssetClass.FUTURE || assetClass == AssetClass.OPTION)) {
                return "Сформирован агрессивный профиль, ориентированный на краткосрочные рыночные возможности и высокую динамику цены. Такой стиль предполагает повышенное внимание к ликвидности, волатильности и скорости изменения рыночной ситуации.";
            }
            return "Профиль отражает поиск активных рыночных идей с повышенной чувствительностью к краткосрочным движениям цены. В таком подходе особенно важны ликвидность инструмента и готовность к заметным колебаниям стоимости.";
        }
        if (request.goal() == Goal.CAPITAL_GROWTH && request.riskProfile() == RiskProfile.MEDIUM && request.horizon() == Horizon.LONG) {
            return "Подбор ориентирован на долгосрочное увеличение стоимости капитала при умеренном уровне риска. Основное внимание уделяется инструментам, способным поддерживать устойчивую динамику на продолжительном временном горизонте.";
        }
        if (request.goal() == Goal.CAPITAL_GROWTH && request.riskProfile() == RiskProfile.HIGH) {
            return "Сформирован профиль роста с готовностью к выраженным рыночным колебаниям. Акцент смещен в сторону инструментов с более высокой динамикой цены и потенциалом активного изменения стоимости.";
        }
        if (profile == UserProfileType.CONSERVATIVE) {
            return "Сформирован консервативный профиль, где важны стабильность, понятная структура инструмента и контроль риска. Такой подход делает акцент на предсказуемости поведения портфеля и постепенном движении к цели.";
        }
        return "Сформирован сбалансированный профиль, сочетающий стремление к росту капитала и контроль рыночного риска. Подбор ориентирован на инструменты, которые могут сохранять устойчивость без чрезмерной концентрации на одном типе риска.";
    }

    private boolean isSafe(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            return false;
        }
        var stripped = value.strip();
        if (!stripped.endsWith(".") && !stripped.endsWith("!") && !stripped.endsWith("?")) {
            return false;
        }
        var normalized = stripped.toLowerCase(Locale.ROOT);
        return FORBIDDEN_PHRASES.stream().noneMatch(normalized::contains);
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value
            .replace("предлагает доходность", "имеет показатели доходности")
            .replace("предлагает более привлекательную доходность", "имеет более привлекательные показатели доходности")
            .replace("\"", "")
            .strip();
    }

    private String trimToLimit(String value) {
        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH - 1).strip() + ".";
    }

    private String scenarioLabel(InvestmentScenario scenario) {
        return switch (scenario) {
            case CAPITAL_PRESERVATION -> "сохранение капитала";
            case STABLE_INCOME -> "стабильный доход";
            case CAPITAL_GROWTH -> "рост капитала";
            case SHORT_TERM_LIQUIDITY -> "краткосрочная ликвидность";
            case SPECULATION -> "спекулятивные идеи";
        };
    }

    private String profileLabel(UserProfileType profile) {
        return switch (profile) {
            case CONSERVATIVE -> "консервативный";
            case BALANCED -> "сбалансированный";
            case AGGRESSIVE -> "агрессивный";
            case PROFESSIONAL -> "профессиональный";
        };
    }

    private String goalLabel(Goal goal) {
        return switch (goal) {
            case CAPITAL_PRESERVATION -> "сохранение капитала";
            case STABLE_INCOME -> "стабильный доход";
            case CAPITAL_GROWTH -> "рост капитала";
            case SPECULATION -> "поиск краткосрочных рыночных возможностей";
        };
    }

    private String riskLabel(RiskProfile riskProfile) {
        return switch (riskProfile) {
            case LOW -> "низкий";
            case MEDIUM -> "средний";
            case HIGH -> "высокий";
        };
    }

    private String horizonLabel(Horizon horizon) {
        return switch (horizon) {
            case SHORT -> "краткосрочный";
            case MEDIUM -> "среднесрочный";
            case LONG -> "долгосрочный";
        };
    }

    private String budgetBand(double budget) {
        if (budget < 50_000) {
            return "небольшой";
        }
        if (budget < 500_000) {
            return "средний";
        }
        return "крупный";
    }

    private String assetClassLabels(List<AssetClass> assetClasses) {
        return assetClasses.stream()
            .map(assetClass -> switch (assetClass) {
                case STOCK -> "акции";
                case BOND -> "облигации";
                case FUTURE -> "фьючерсы";
                case OPTION -> "опционы";
            })
            .toList()
            .toString();
    }

    private record OllamaGenerateResponse(String response) {
    }
}
