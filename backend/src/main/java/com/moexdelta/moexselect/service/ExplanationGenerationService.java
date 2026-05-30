package com.moexdelta.moexselect.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.moexdelta.moexselect.dto.InternalScores;
import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.enums.ConfidenceLevel;
import com.moexdelta.moexselect.enums.InvestmentScenario;
import com.moexdelta.moexselect.enums.ReasonCode;
import com.moexdelta.moexselect.model.Instrument;

@Service
public class ExplanationGenerationService {

    private static final int MAX_LENGTH = 450;
    private static final List<String> FORBIDDEN_PHRASES = List.of(
        "купите",
        "продавайте",
        "гарантированно",
        "точно вырастет",
        "лучший инструмент",
        "советуем купить",
        "инвестиционная рекомендация"
    );

    private final WebClient.Builder webClientBuilder;
    private final ExplanationService explanationService;
    private final boolean llmEnabled;
    private final String llmProvider;
    private final String llmModel;
    private final String llmBaseUrl;

    public ExplanationGenerationService(
        WebClient.Builder webClientBuilder,
        ExplanationService explanationService,
        @Value("${LLM_EXPLANATIONS_ENABLED:false}") boolean llmEnabled,
        @Value("${LLM_PROVIDER:ollama}") String llmProvider,
        @Value("${LLM_MODEL:llama3.1:8b}") String llmModel,
        @Value("${LLM_BASE_URL:http://localhost:11434}") String llmBaseUrl
    ) {
        this.webClientBuilder = webClientBuilder;
        this.explanationService = explanationService;
        this.llmEnabled = llmEnabled;
        this.llmProvider = llmProvider;
        this.llmModel = llmModel;
        this.llmBaseUrl = llmBaseUrl;
    }

    public String generateSummary(
        Instrument instrument,
        RecommendationRequest request,
        InvestmentScenario scenario,
        ConfidenceLevel confidenceLevel,
        List<ReasonCode> reasonCodes,
        List<String> warnings,
        InternalScores scores
    ) {
        if (llmEnabled) {
            var llmSummary = switch (llmProvider.toLowerCase(Locale.ROOT)) {
                case "mock" -> mockSummary(instrument, scenario, reasonCodes, confidenceLevel);
                case "ollama" -> ollamaSummary(instrument, request, scenario, confidenceLevel, reasonCodes, warnings, scores);
                default -> null;
            };
            if (isSafe(llmSummary)) {
                return llmSummary.strip();
            }
        }
        return templateSummary(instrument, scenario, confidenceLevel, reasonCodes);
    }

    private String ollamaSummary(
        Instrument instrument,
        RecommendationRequest request,
        InvestmentScenario scenario,
        ConfidenceLevel confidenceLevel,
        List<ReasonCode> reasonCodes,
        List<String> warnings,
        InternalScores scores
    ) {
        try {
            var client = webClientBuilder
                .baseUrl(llmBaseUrl)
                .build();
            var response = client.post()
                .uri("/api/generate")
                .bodyValue(Map.of(
                    "model", llmModel,
                    "prompt", systemPrompt() + "\n\n" + userPrompt(instrument, request, scenario, confidenceLevel, reasonCodes, warnings, scores),
                    "stream", false
                ))
                .retrieve()
                .bodyToMono(OllamaGenerateResponse.class)
                .timeout(Duration.ofSeconds(3))
                .block();
            return response == null ? null : response.response();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String mockSummary(
        Instrument instrument,
        InvestmentScenario scenario,
        List<ReasonCode> reasonCodes,
        ConfidenceLevel confidenceLevel
    ) {
        return templateSummary(instrument, scenario, confidenceLevel, reasonCodes);
    }

    private String templateSummary(
        Instrument instrument,
        InvestmentScenario scenario,
        ConfidenceLevel confidenceLevel,
        List<ReasonCode> reasonCodes
    ) {
        var reasons = reasonCodes.stream()
            .limit(2)
            .map(explanationService::textFor)
            .toList();
        var first = "Инструмент " + instrument.ticker() + " попал в подборку по сценарию "
            + scenarioLabel(scenario).toLowerCase(Locale.ROOT) + ".";
        var second = reasons.isEmpty()
            ? "Ранжирование учитывает выбранные параметры пользователя и доступные данные MOEX ISS."
            : String.join(" ", reasons);
        var third = "Полнота данных для подбора: " + confidenceLabel(confidenceLevel).toLowerCase(Locale.ROOT) + ".";
        return trimToLimit(first + " " + second + " " + third);
    }

    private String systemPrompt() {
        return """
            Ты генерируешь краткие объяснения для сервиса подбора финансовых инструментов MOEX Select.
            Ты не даешь инвестиционных рекомендаций.
            Ты не советуешь покупать или продавать.
            Ты используешь только переданные данные.
            Не выдумывай доходность, рейтинг, цену, риск или прогноз.
            Пиши на русском языке.
            Стиль: нейтральный, профессиональный, понятный частному инвестору.
            Длина: 2-3 коротких предложения.
            Запрещенные слова и фразы: купите, продавайте, гарантированно, точно вырастет, лучший инструмент, советуем купить.
            """;
    }

    private String userPrompt(
        Instrument instrument,
        RecommendationRequest request,
        InvestmentScenario scenario,
        ConfidenceLevel confidenceLevel,
        List<ReasonCode> reasonCodes,
        List<String> warnings,
        InternalScores scores
    ) {
        return """
            Данные инструмента:
            ticker: %s
            name: %s
            assetClass: %s
            price: %s
            currency: %s
            yieldValue: %s
            maturityDate: %s

            Параметры пользователя:
            goal: %s
            riskProfile: %s
            horizon: %s
            experience: %s

            Расчетный результат:
            investmentScenario: %s
            riskLevel: %s
            liquidityLevel: %s
            confidenceLevel: %s
            reasonCodes: %s
            warnings: %s

            Сформулируй только объяснение карточки. Не меняй ранжирование и не добавляй новых фактов.
            """.formatted(
            instrument.ticker(),
            instrument.name(),
            instrument.assetClass(),
            instrument.price(),
            instrument.currency(),
            instrument.yieldValue(),
            instrument.maturityDate(),
            request.goal(),
            request.riskProfile(),
            request.horizon(),
            request.experience(),
            scenario,
            scores.riskScore(),
            scores.liquidityScore(),
            confidenceLevel,
            reasonCodes,
            warnings
        );
    }

    private boolean isSafe(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            return false;
        }
        var normalized = value.toLowerCase(Locale.ROOT);
        return FORBIDDEN_PHRASES.stream().noneMatch(normalized::contains);
    }

    private String trimToLimit(String value) {
        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH - 1).strip() + ".";
    }

    private String scenarioLabel(InvestmentScenario scenario) {
        return switch (scenario) {
            case CAPITAL_PRESERVATION -> "сохранения капитала";
            case STABLE_INCOME -> "стабильного дохода";
            case CAPITAL_GROWTH -> "роста капитала";
            case SHORT_TERM_LIQUIDITY -> "краткосрочной ликвидности";
            case SPECULATION -> "спекулятивных идей";
        };
    }

    private String confidenceLabel(ConfidenceLevel confidenceLevel) {
        return switch (confidenceLevel) {
            case HIGH -> "высокая";
            case MEDIUM -> "средняя";
            case LOW -> "ограниченная";
        };
    }

    private record OllamaGenerateResponse(String response) {
    }
}
