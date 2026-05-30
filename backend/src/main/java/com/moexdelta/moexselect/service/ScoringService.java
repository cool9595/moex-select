package com.moexdelta.moexselect.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moexdelta.moexselect.dto.InternalScores;
import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.enums.ConfidenceLevel;
import com.moexdelta.moexselect.enums.Experience;
import com.moexdelta.moexselect.enums.Horizon;
import com.moexdelta.moexselect.enums.InvestmentScenario;
import com.moexdelta.moexselect.enums.Level;
import com.moexdelta.moexselect.enums.RiskProfile;
import com.moexdelta.moexselect.enums.UserProfileType;
import com.moexdelta.moexselect.model.Instrument;

@Service
public class ScoringService {

    private static final Map<UserProfileType, Weights> PROFILE_WEIGHTS = Map.of(
        UserProfileType.CONSERVATIVE, new Weights(0.25, 0.15, 0.25, 0.25, 0.10),
        UserProfileType.BALANCED, new Weights(0.25, 0.25, 0.20, 0.15, 0.15),
        UserProfileType.AGGRESSIVE, new Weights(0.20, 0.35, 0.15, 0.10, 0.20),
        UserProfileType.PROFESSIONAL, new Weights(0.30, 0.25, 0.15, 0.05, 0.25)
    );

    private static final Map<InvestmentScenario, ScenarioWeights> SCENARIO_WEIGHTS = Map.of(
        InvestmentScenario.CAPITAL_PRESERVATION, new ScenarioWeights(0.20, 0.05, 0.30, 0.30, 0.10, 0.05),
        InvestmentScenario.STABLE_INCOME, new ScenarioWeights(0.15, 0.30, 0.15, 0.25, 0.10, 0.05),
        InvestmentScenario.CAPITAL_GROWTH, new ScenarioWeights(0.15, 0.30, 0.15, 0.05, 0.25, 0.10),
        InvestmentScenario.SHORT_TERM_LIQUIDITY, new ScenarioWeights(0.40, 0.05, 0.20, 0.05, 0.15, 0.15),
        InvestmentScenario.SPECULATION, new ScenarioWeights(0.15, 0.35, 0.10, 0.05, 0.25, 0.10)
    );

    public InternalScores calculate(
        Instrument instrument,
        RecommendationRequest request,
        UserProfileType profile,
        InvestmentScenario scenario
    ) {
        var liquidity = liquidityScore(instrument);
        var yield = yieldScore(instrument);
        var risk = riskScore(instrument);
        var creditQuality = creditQualityScore(instrument);
        var fit = fitScore(instrument, request.riskProfile());
        var horizon = horizonScore(instrument, request.horizon(), liquidity, yield, risk);

        if (request.riskProfile() == RiskProfile.LOW && isDerivative(instrument)) {
            risk = Math.min(risk, 20);
            fit = Math.min(fit, 20);
        }

        // The scenario score ranks results internally; it is never a forecast or promised return.
        var weights = SCENARIO_WEIGHTS.get(scenario);
        var scenarioScore = liquidity * weights.liquidityWeight()
            + yield * weights.yieldWeight()
            + risk * weights.riskWeight()
            + creditQuality * weights.creditQualityWeight()
            + fit * weights.fitWeight()
            + horizon * weights.horizonWeight();
        var penalty = penalty(instrument, request, liquidity, risk, creditQuality, fit);
        var finalScore = Math.max(0, scenarioScore - penalty);
        return new InternalScores(
            rounded(finalScore),
            rounded(liquidity),
            rounded(yield),
            rounded(risk),
            rounded(creditQuality),
            rounded(fit),
            rounded(horizon),
            rounded(penalty)
        );
    }

    public InternalScores calculate(Instrument instrument, RecommendationRequest request, UserProfileType profile) {
        var weights = PROFILE_WEIGHTS.get(profile);
        var liquidity = liquidityScore(instrument);
        var yield = yieldScore(instrument);
        var risk = riskScore(instrument);
        var creditQuality = creditQualityScore(instrument);
        var fit = fitScore(instrument, request.riskProfile());
        var horizon = horizonScore(instrument, request.horizon(), liquidity, yield, risk);
        var finalScore = liquidity * weights.liquidityWeight()
            + yield * weights.yieldWeight()
            + risk * weights.riskWeight()
            + creditQuality * weights.creditQualityWeight()
            + fit * weights.fitWeight();
        return new InternalScores(rounded(finalScore), rounded(liquidity), rounded(yield), rounded(risk),
            rounded(creditQuality), rounded(fit), rounded(horizon), 0);
    }

    public double liquidityScore(Instrument instrument) {
        Double turnover = instrument.turnover() == null ? null : Math.log(instrument.turnover() + 1);
        Double volume = instrument.volume() == null ? null : Math.log(instrument.volume() + 1);
        Double turnoverScore = turnover == null ? null : normalize(turnover, 10, 23);
        Double volumeScore = volume == null ? null : normalize(volume, 6, 18);
        if (turnoverScore != null && volumeScore != null) {
            return rounded(0.6 * turnoverScore + 0.4 * volumeScore);
        }
        return turnoverScore != null ? turnoverScore : volumeScore != null ? volumeScore : 50;
    }

    public double horizonFit(Instrument instrument, Horizon horizon) {
        if (instrument.assetClass() == AssetClass.STOCK) {
            return horizon == Horizon.SHORT ? 60 : 100;
        }
        if (isDerivative(instrument)) {
            return horizon == Horizon.SHORT ? 100 : horizon == Horizon.MEDIUM ? 60 : 20;
        }
        if (instrument.maturityDate() == null) {
            return 50;
        }
        try {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(instrument.maturityDate()));
            if (days <= 0) {
                return 0;
            }
            int instrumentHorizon = days <= 365 ? 1 : days <= 1095 ? 2 : 3;
            int difference = Math.abs(instrumentHorizon - horizonPosition(horizon));
            return difference == 0 ? 100 : difference == 1 ? 60 : 20;
        } catch (DateTimeParseException exception) {
            return 50;
        }
    }

    public double horizonScore(
        Instrument instrument,
        Horizon horizon,
        double liquidityScore,
        double yieldScore,
        double riskScore
    ) {
        if (instrument.assetClass() == AssetClass.STOCK) {
            return switch (horizon) {
                case SHORT -> rounded(0.6 * liquidityScore + 0.4 * riskScore);
                case MEDIUM -> rounded(0.4 * liquidityScore + 0.4 * riskScore + 0.2 * yieldScore);
                case LONG -> rounded(0.3 * liquidityScore + 0.3 * riskScore + 0.4 * yieldScore);
            };
        }
        if (instrument.assetClass() == AssetClass.BOND) {
            return bondHorizonScore(instrument, horizon);
        }
        return derivativeHorizonScore(instrument, horizon);
    }

    public Level impliedRisk(Instrument instrument) {
        if (instrument.assetClass() == AssetClass.BOND) {
            return creditQualityScore(instrument) >= 70 ? Level.LOW : Level.MEDIUM;
        }
        return instrument.assetClass() == AssetClass.STOCK ? Level.MEDIUM : Level.HIGH;
    }

    public Level liquidityLevel(double score) {
        return score >= 70 ? Level.HIGH : score < 35 ? Level.LOW : Level.MEDIUM;
    }

    public boolean isDerivative(Instrument instrument) {
        return instrument.assetClass() == AssetClass.FUTURE || instrument.assetClass() == AssetClass.OPTION;
    }

    public ConfidenceLevel confidenceLevel(Instrument instrument) {
        int score = 100;
        if (instrument.yieldValue() == null) {
            score -= 10;
        }
        if (instrument.turnover() == null && instrument.volume() == null) {
            score -= 15;
        }
        if (instrument.volatility() == null) {
            score -= 15;
        }
        if (instrument.assetClass() == AssetClass.BOND && instrument.creditRating() == null) {
            score -= 20;
        }
        if (instrument.assetClass() == AssetClass.BOND && instrument.maturityDate() == null) {
            score -= 20;
        }
        if (instrument.price() == null) {
            score -= 10;
        }
        if (score >= 80) {
            return ConfidenceLevel.HIGH;
        }
        return score >= 50 ? ConfidenceLevel.MEDIUM : ConfidenceLevel.LOW;
    }

    public double yieldScore(Instrument instrument) {
        // Published yield is a comparative input for ranking, not a guaranteed outcome.
        return normalize(instrument.yieldValue(), 0, 25);
    }

    public double riskScore(Instrument instrument) {
        if (instrument.volatility() == null) {
            return switch (instrument.assetClass()) {
                case STOCK -> 60;
                case BOND -> 75;
                case FUTURE -> 35;
                case OPTION -> 25;
            };
        }
        // Lower observed volatility produces a higher internal risk suitability value.
        return rounded(100 - normalize(instrument.volatility(), 5, 65));
    }

    public double creditQualityScore(Instrument instrument) {
        if (instrument.creditRating() == null) {
            return instrument.assetClass() == AssetClass.BOND ? 40 : 60;
        }
        var rating = instrument.creditRating().toUpperCase(Locale.ROOT);
        if (rating.startsWith("AAA")) {
            return 100;
        }
        if (rating.startsWith("AA")) {
            return 90;
        }
        if (rating.startsWith("A")) {
            return 80;
        }
        if (rating.startsWith("BBB")) {
            return 65;
        }
        if (rating.startsWith("BB")) {
            return 45;
        }
        return 25;
    }

    public double fitScore(Instrument instrument, RiskProfile riskProfile) {
        var difference = Math.abs(riskPosition(riskProfile) - riskPosition(impliedRisk(instrument)));
        return difference == 0 ? 100 : difference == 1 ? 60 : 20;
    }

    private double normalize(Double value, double minimum, double maximum) {
        if (value == null) {
            return 50;
        }
        return Math.max(0, Math.min(100, (value - minimum) / (maximum - minimum) * 100));
    }

    private int riskPosition(RiskProfile riskProfile) {
        return switch (riskProfile) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    private int riskPosition(Level riskLevel) {
        return switch (riskLevel) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    private int horizonPosition(Horizon horizon) {
        return switch (horizon) {
            case SHORT -> 1;
            case MEDIUM -> 2;
            case LONG -> 3;
        };
    }

    private double penalty(
        Instrument instrument,
        RecommendationRequest request,
        double liquidity,
        double risk,
        double creditQuality,
        double fit
    ) {
        double penalty = 0;
        if (liquidity < 35) {
            penalty += 8;
        }
        if (instrument.price() == null || instrument.turnover() == null && instrument.volume() == null) {
            penalty += 5;
        }
        if (fit < 50 || risk < 35 && request.riskProfile() != RiskProfile.HIGH) {
            penalty += 12;
        }
        if (instrument.assetClass() == AssetClass.BOND && creditQuality < 50) {
            penalty += 10;
        }
        if (request.experience() == Experience.BEGINNER && isDerivative(instrument)) {
            penalty += 20;
        }
        return penalty;
    }

    private double bondHorizonScore(Instrument instrument, Horizon horizon) {
        if (instrument.maturityDate() == null) {
            return 50;
        }
        try {
            double years = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(instrument.maturityDate())) / 365.25;
            if (years <= 0) {
                return 0;
            }
            return switch (horizon) {
                case SHORT -> years <= 1.5 ? 100 : years <= 3.5 ? 60 : 25;
                case MEDIUM -> years >= 1 && years <= 3.5 ? 100 : years < 1 ? 55 : 65;
                case LONG -> years >= 3 ? 100 : years >= 1.5 ? 65 : 30;
            };
        } catch (DateTimeParseException exception) {
            return 50;
        }
    }

    private double derivativeHorizonScore(Instrument instrument, Horizon horizon) {
        var expirationDate = firstRawDate(instrument, "LASTTRADEDATE", "EXPIRATIONDATE", "MATDATE", "MaturityDate");
        if (expirationDate != null) {
            try {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(expirationDate));
                int instrumentHorizon = days <= 90 ? 1 : days <= 365 ? 2 : 3;
                int difference = Math.abs(instrumentHorizon - horizonPosition(horizon));
                return difference == 0 ? 100 : difference == 1 ? 60 : 25;
            } catch (DateTimeParseException exception) {
                return derivativeDefaultHorizonScore(instrument, horizon);
            }
        }
        return derivativeDefaultHorizonScore(instrument, horizon);
    }

    private double derivativeDefaultHorizonScore(Instrument instrument, Horizon horizon) {
        if (instrument.assetClass() == AssetClass.FUTURE) {
            return switch (horizon) {
                case SHORT -> 65;
                case MEDIUM -> 45;
                case LONG -> 25;
            };
        }
        return switch (horizon) {
            case SHORT -> 55;
            case MEDIUM -> 35;
            case LONG -> 20;
        };
    }

    private String firstRawDate(Instrument instrument, String... fields) {
        if (instrument.raw() == null) {
            return null;
        }
        for (var field : fields) {
            var value = instrument.raw().get(field);
            if (value != null && !value.toString().isBlank() && !"0000-00-00".equals(value.toString())) {
                return value.toString();
            }
        }
        return null;
    }

    private double rounded(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record Weights(
        double liquidityWeight,
        double yieldWeight,
        double riskWeight,
        double creditQualityWeight,
        double fitWeight
    ) {
    }

    private record ScenarioWeights(
        double liquidityWeight,
        double yieldWeight,
        double riskWeight,
        double creditQualityWeight,
        double fitWeight,
        double horizonWeight
    ) {
    }
}
