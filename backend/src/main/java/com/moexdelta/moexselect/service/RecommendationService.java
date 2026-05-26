package com.moexdelta.moexselect.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moexdelta.moexselect.dto.PublicInstrumentRecommendation;
import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.dto.RecommendationResponse;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.enums.Experience;
import com.moexdelta.moexselect.enums.Goal;
import com.moexdelta.moexselect.enums.RiskProfile;
import com.moexdelta.moexselect.enums.UserProfileType;
import com.moexdelta.moexselect.model.Instrument;

@Service
public class RecommendationService {

    public static final String DISCLAIMER = "Информация не является индивидуальной инвестиционной рекомендацией. "
        + "Подбор основан на выбранных пользователем параметрах и открытых рыночных данных MOEX ISS.";

    private final ScoringService scoringService;
    private final ExplanationService explanationService;

    public RecommendationService(ScoringService scoringService, ExplanationService explanationService) {
        this.scoringService = scoringService;
        this.explanationService = explanationService;
    }

    public RecommendationResponse recommend(
        List<Instrument> instruments,
        RecommendationRequest request,
        boolean debug
    ) {
        var profile = detectUserProfile(request);
        var recommendations = instruments.stream()
            .filter(instrument -> request.assetClasses().contains(instrument.assetClass()))
            .filter(instrument -> shouldKeepInstrument(instrument, request))
            .map(instrument -> rankInstrument(instrument, request, profile, debug))
            .sorted(Comparator.comparingDouble(RankedInstrument::rank).reversed())
            .limit(request.limit())
            .map(RankedInstrument::recommendation)
            .toList();
        return new RecommendationResponse(profile, DISCLAIMER, recommendations);
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

    private RankedInstrument rankInstrument(
        Instrument instrument,
        RecommendationRequest request,
        UserProfileType profile,
        boolean debug
    ) {
        var scores = scoringService.calculate(instrument, request, profile);
        var publicRecommendation = new PublicInstrumentRecommendation(
            instrument.ticker(),
            instrument.name(),
            instrument.assetClass(),
            instrument.price(),
            instrument.currency(),
            instrument.yieldValue(),
            instrument.maturityDate(),
            scoringService.impliedRisk(instrument),
            scoringService.liquidityLevel(scores.liquidityScore()),
            scores.fitScore() > 80,
            explanationService.explanations(instrument, request, scores),
            explanationService.warnings(instrument),
            moexUrl(instrument),
            debug ? scores : null
        );
        return new RankedInstrument(publicRecommendation, scores.finalScore());
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

    private record RankedInstrument(PublicInstrumentRecommendation recommendation, double rank) {
    }
}
