package com.moexdelta.moexselect.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moexdelta.moexselect.dto.InternalScores;
import com.moexdelta.moexselect.dto.PublicInstrumentRecommendation;
import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.dto.RecommendationResponse;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.enums.Experience;
import com.moexdelta.moexselect.enums.Goal;
import com.moexdelta.moexselect.enums.InvestmentScenario;
import com.moexdelta.moexselect.enums.RiskProfile;
import com.moexdelta.moexselect.enums.UserProfileType;
import com.moexdelta.moexselect.model.Instrument;

@Service
public class RecommendationService {

    public static final String DISCLAIMER = "Информация не является индивидуальной инвестиционной рекомендацией. "
        + "Подбор основан на выбранных пользователем параметрах и открытых рыночных данных.";

    private final ScoringService scoringService;
    private final ExplanationService explanationService;
    private final ExplanationGenerationService explanationGenerationService;
    private final ScenarioDetectionService scenarioDetectionService;

    public RecommendationService(
        ScoringService scoringService,
        ExplanationService explanationService,
        ExplanationGenerationService explanationGenerationService,
        ScenarioDetectionService scenarioDetectionService
    ) {
        this.scoringService = scoringService;
        this.explanationService = explanationService;
        this.explanationGenerationService = explanationGenerationService;
        this.scenarioDetectionService = scenarioDetectionService;
    }

    public RecommendationResponse recommend(
        List<Instrument> instruments,
        RecommendationRequest request,
        boolean debug
    ) {
        var profile = detectUserProfile(request);
        var scenario = scenarioDetectionService.detect(request);
        var recommendations = instruments.stream()
            .filter(instrument -> request.assetClasses().contains(instrument.assetClass()))
            .filter(instrument -> shouldKeepInstrument(instrument, request))
            .map(instrument -> rankInstrument(instrument, request, profile, scenario))
            .sorted(Comparator.comparingDouble(RankedCandidate::rank).reversed())
            .limit(request.limit())
            .map(candidate -> toRecommendation(candidate, request, scenario, debug))
            .toList();
        var profileSummary = explanationGenerationService.generateProfileSummary(
            request,
            profile,
            scenario,
            recommendations.size()
        );
        return new RecommendationResponse(profile, DISCLAIMER, profileSummary, recommendations);
    }

    public UserProfileType detectUserProfile(RecommendationRequest request) {
        boolean derivativesSelected = request.assetClasses().stream()
            .anyMatch(assetClass -> assetClass == AssetClass.FUTURE || assetClass == AssetClass.OPTION);
        if (request.experience() == Experience.ADVANCED && derivativesSelected) {
            return UserProfileType.PROFESSIONAL;
        }
        if (request.riskProfile() == RiskProfile.LOW
            && (request.goal() == Goal.STABLE_INCOME || request.goal() == Goal.CAPITAL_PRESERVATION)) {
            return UserProfileType.CONSERVATIVE;
        }
        if (request.riskProfile() == RiskProfile.HIGH
            && (request.goal() == Goal.CAPITAL_GROWTH || request.goal() == Goal.SPECULATION)) {
            return UserProfileType.AGGRESSIVE;
        }
        return UserProfileType.BALANCED;
    }

    private RankedCandidate rankInstrument(
        Instrument instrument,
        RecommendationRequest request,
        UserProfileType profile,
        InvestmentScenario scenario
    ) {
        var scores = scoringService.calculate(instrument, request, profile, scenario);
        return new RankedCandidate(instrument, scores, scores.finalScore());
    }

    private PublicInstrumentRecommendation toRecommendation(
        RankedCandidate candidate,
        RecommendationRequest request,
        InvestmentScenario scenario,
        boolean debug
    ) {
        var instrument = candidate.instrument();
        var scores = candidate.scores();
        var confidenceLevel = scoringService.confidenceLevel(instrument);
        var warnings = explanationService.warnings(instrument);
        return new PublicInstrumentRecommendation(
            instrument.ticker(),
            instrument.name(),
            instrument.assetClass(),
            instrument.price(),
            instrument.currency(),
            instrument.yieldValue(),
            instrument.maturityDate(),
            scoringService.impliedRisk(instrument),
            scoringService.liquidityLevel(scores.liquidityScore()),
            confidenceLevel,
            scenario,
            null,
            scores.fitScore() > 80,
            List.of(),
            warnings,
            moexUrl(instrument),
            debug ? scores : null
        );
    }

    private boolean shouldKeepInstrument(Instrument instrument, RecommendationRequest request) {
        boolean selectedOnlyDerivatives = request.assetClasses().stream()
            .allMatch(assetClass -> assetClass == AssetClass.FUTURE || assetClass == AssetClass.OPTION);
        if (request.experience() == Experience.BEGINNER
            && scoringService.isDerivative(instrument)
            && !selectedOnlyDerivatives) {
            return false;
        }
        return request.riskProfile() != RiskProfile.LOW || scoringService.liquidityScore(instrument) >= 25;
    }

    private String moexUrl(Instrument instrument) {
        var ticker = encoded(instrument.ticker());
        if (instrument.board() != null && !instrument.board().isBlank()) {
            return "https://www.moex.com/ru/issue.aspx?board=" + encoded(instrument.board()) + "&code=" + ticker;
        }
        return "https://www.moex.com/ru/issue.aspx?code=" + ticker;
    }

    private String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record RankedCandidate(Instrument instrument, InternalScores scores, double rank) {
    }
}
