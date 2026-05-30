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
        "инвестиционная рекомендация",
        "покуп",
        "продаж",
        "совет"
    );

    private final WebClient.Builder webClientBuilder;
    private final ExplanationService explanationService;
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
        @Value("${LLM_TIMEOUT_SECONDS:12}") int llmTimeoutSeconds
    ) {
        this.webClientBuilder = webClientBuilder;
        this.explanationService = explanationService;
        this.llmEnabled = llmEnabled;
        this.llmProvider = llmProvider;
        this.llmModel = llmModel;
        this.llmBaseUrl = llmBaseUrl;
        this.llmTimeoutSeconds = Math.max(1, llmTimeoutSeconds);
    }

    public String generateSummary(
        Instrument instrument,
        RecommendationRequest request,
        InvestmentScenario scenario,
        ConfidenceLevel confidenceLevel,
        List<ReasonCode> reasonCodes,
        List<String> warnings,
        InternalScores scores,
        boolean allowLlm
    ) {
        if (llmEnabled && allowLlm) {
            var llmSummary = switch (llmProvider.toLowerCase(Locale.ROOT)) {
                case "mock" -> mockSummary(instrument, scenario, reasonCodes);
                case "ollama" -> ollamaSummary(instrument, request, scenario, reasonCodes, warnings, scores);
                default -> null;
            };
            llmSummary = sanitize(llmSummary);
            if (isSafe(llmSummary)) {
                return trimToLimit(llmSummary.strip());
            }
        }
        return templateSummary(instrument, scenario, reasonCodes);
    }

    private String ollamaSummary(
        Instrument instrument,
        RecommendationRequest request,
        InvestmentScenario scenario,
        List<ReasonCode> reasonCodes,
        List<String> warnings,
        InternalScores scores
    ) {
        try {
            var client = webClientBuilder.baseUrl(llmBaseUrl).build();
            var response = client.post()
                .uri("/api/generate")
                .bodyValue(Map.of(
                    "model", llmModel,
                    "prompt", prompt(instrument, request, scenario, reasonCodes, warnings, scores),
                    "stream", false,
                    "options", Map.of(
                        "temperature", 0.15,
                        "num_predict", 45
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

    private String mockSummary(
        Instrument instrument,
        InvestmentScenario scenario,
        List<ReasonCode> reasonCodes
    ) {
        return templateSummary(instrument, scenario, reasonCodes);
    }

    private String templateSummary(
        Instrument instrument,
        InvestmentScenario scenario,
        List<ReasonCode> reasonCodes
    ) {
        var reasons = reasonCodes.stream()
            .limit(2)
            .map(explanationService::textFor)
            .toList();
        var first = "Инструмент " + instrument.ticker() + " попал в подборку по сценарию "
            + scenarioLabel(scenario).toLowerCase(Locale.ROOT) + ".";
        var second = reasons.isEmpty()
            ? "Ранжирование учитывает выбранные параметры пользователя и доступные рыночные данные."
            : String.join(" ", reasons);
        return trimToLimit(first + " " + second);
    }

    private String prompt(
        Instrument instrument,
        RecommendationRequest request,
        InvestmentScenario scenario,
        List<ReasonCode> reasonCodes,
        List<String> warnings,
        InternalScores scores
    ) {
        var reasons = reasonCodes.stream()
            .limit(3)
            .map(explanationService::textFor)
            .toList();
        return """
            Напиши одно короткое пояснение на русском для карточки MOEX Select.
            Это не инвестиционная рекомендация. Не используй слова: купить, покупать, продать, продавать, совет.
            Не обещай доходность и не делай прогнозов. Не раскрывай числовые score.

            Инструмент: %s, %s, %s.
            Сценарий: %s. Риск пользователя: %s. Горизонт: %s.
            Причины: %s.
            Предупреждения: %s.

            Ответь одним нейтральным предложением.
            """.formatted(
            instrument.ticker(),
            instrument.name(),
            instrument.assetClass(),
            scenarioLabel(scenario),
            request.riskProfile(),
            request.horizon(),
            reasons,
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

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value
            .replace("предлагает доходность", "имеет показатели доходности")
            .replace("предлагает более привлекательную доходность", "имеет более привлекательные показатели доходности");
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

    private record OllamaGenerateResponse(String response) {
    }
}
