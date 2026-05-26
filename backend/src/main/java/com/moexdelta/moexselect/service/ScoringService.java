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
import com.moexdelta.moexselect.enums.Horizon;
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

    public InternalScores calculate(Instrument instrument, RecommendationRequest request, UserProfileType profile) {
        var liquidity = liquidityScore(instrument);
        var yield = yieldScore(instrument);
        var risk = riskScore(instrument);
        var creditQuality = creditQualityScore(instrument);
        var fit = fitScore(instrument, request.riskProfile());

        if (request.riskProfile() == RiskProfile.LOW && isDerivative(instrument)) {
            risk = Math.min(risk, 20);
            fit = Math.min(fit, 20);
        }

        var weights = PROFILE_WEIGHTS.get(profile);
        // The final value ranks results internally; it is never a forecast or promised return.
        var finalScore = liquidity * weights.liquidityWeight()
            + yield * weights.yieldWeight()
            + risk * weights.riskWeight()
            + creditQuality * weights.creditQualityWeight()
            + fit * weights.fitWeight();
        return new InternalScores(
            rounded(finalScore),
            rounded(liquidity),
            rounded(yield),
            rounded(risk),
            rounded(creditQuality),
            rounded(fit)
        );
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

    private double yieldScore(Instrument instrument) {
        // Published yield is a comparative input for ranking, not a guaranteed outcome.
        return normalize(instrument.yieldValue(), 0, 25);
    }

    private double riskScore(Instrument instrument) {
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

    private double creditQualityScore(Instrument instrument) {
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

    private double fitScore(Instrument instrument, RiskProfile riskProfile) {
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
}
