package com.moexdelta.moexselect.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moexdelta.moexselect.dto.InternalScores;
import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.enums.ConfidenceLevel;
import com.moexdelta.moexselect.enums.Experience;
import com.moexdelta.moexselect.enums.InvestmentScenario;
import com.moexdelta.moexselect.enums.ReasonCode;
import com.moexdelta.moexselect.model.Instrument;

@Service
public class ExplanationService {

    private final ScoringService scoringService;

    public ExplanationService(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    public List<String> explanations(
        Instrument instrument,
        RecommendationRequest request,
        InternalScores scores,
        InvestmentScenario scenario,
        ConfidenceLevel confidenceLevel
    ) {
        return reasonCodes(instrument, request, scores, scenario, confidenceLevel).stream()
            .map(this::textFor)
            .distinct()
            .limit(4)
            .toList();
    }

    public List<ReasonCode> reasonCodes(
        Instrument instrument,
        RecommendationRequest request,
        InternalScores scores,
        InvestmentScenario scenario,
        ConfidenceLevel confidenceLevel
    ) {
        var result = new ArrayList<ReasonCode>();
        if (scores.liquidityScore() > 70) {
            result.add(ReasonCode.HIGH_LIQUIDITY);
        }
        if (scores.fitScore() > 80) {
            result.add(ReasonCode.MATCHES_RISK_PROFILE);
        }
        if (scores.horizonScore() > 70) {
            result.add(ReasonCode.MATCHES_HORIZON);
        }
        if (instrument.assetClass() == AssetClass.BOND && scores.creditQualityScore() > 70) {
            result.add(ReasonCode.GOOD_CREDIT_QUALITY);
        }
        if (scores.yieldScore() > 70) {
            result.add(ReasonCode.ATTRACTIVE_YIELD);
        }
        switch (scenario) {
            case SHORT_TERM_LIQUIDITY -> result.add(ReasonCode.SHORT_TERM_LIQUIDITY_MATCH);
            case CAPITAL_PRESERVATION -> result.add(ReasonCode.CAPITAL_PRESERVATION_MATCH);
            case STABLE_INCOME -> result.add(ReasonCode.STABLE_INCOME_MATCH);
            case CAPITAL_GROWTH -> result.add(ReasonCode.GROWTH_SCENARIO_MATCH);
            case SPECULATION -> {
                if (scoringService.isDerivative(instrument)) {
                    result.add(ReasonCode.COMPLEX_INSTRUMENT);
                }
            }
        }
        if (scoringService.isDerivative(instrument) && request.experience() == Experience.ADVANCED) {
            result.add(ReasonCode.COMPLEX_INSTRUMENT);
        }
        if (confidenceLevel == ConfidenceLevel.LOW) {
            result.add(ReasonCode.LOW_DATA_CONFIDENCE);
        }
        if (result.size() < 2) {
            result.add(ReasonCode.MATCHES_RISK_PROFILE);
            result.add(ReasonCode.MATCHES_HORIZON);
        }
        return result;
    }

    public List<String> warnings(Instrument instrument) {
        return scoringService.isDerivative(instrument)
            ? List.of("Инструмент относится к сложным финансовым инструментам и подходит только пользователям с соответствующим опытом.")
            : List.of();
    }

    public String textFor(ReasonCode reasonCode) {
        return switch (reasonCode) {
            case HIGH_LIQUIDITY -> "Имеет достаточную ликвидность для частного инвестора.";
            case MATCHES_RISK_PROFILE -> "Соответствует выбранному уровню риска.";
            case MATCHES_HORIZON -> "Подходит под выбранный горизонт инвестирования.";
            case GOOD_CREDIT_QUALITY -> "Кредитное качество соответствует консервативному профилю.";
            case ATTRACTIVE_YIELD -> "Доходность находится среди более привлекательных в выбранной группе инструментов.";
            case SHORT_TERM_LIQUIDITY_MATCH -> "Учитывает сценарий краткосрочной ликвидности.";
            case CAPITAL_PRESERVATION_MATCH -> "Подходит для сценария сохранения капитала.";
            case STABLE_INCOME_MATCH -> "Связан со сценарием стабильного дохода.";
            case GROWTH_SCENARIO_MATCH -> "Соответствует сценарию роста капитала.";
            case COMPLEX_INSTRUMENT -> "Инструмент отображается только для пользователей с соответствующим опытом.";
            case LOW_DATA_CONFIDENCE -> "Для инструмента доступен ограниченный набор рыночных данных.";
        };
    }
}
