package com.moexdelta.moexselect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.moexdelta.moexselect.data.ReserveInstrumentCatalog;
import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.enums.Experience;
import com.moexdelta.moexselect.enums.Goal;
import com.moexdelta.moexselect.enums.Horizon;
import com.moexdelta.moexselect.enums.RiskProfile;
import com.moexdelta.moexselect.enums.UserProfileType;
import com.moexdelta.moexselect.model.Instrument;

class RecommendationServiceTests {

    private final ScoringService scoringService = new ScoringService();
    private final RecommendationService service =
        new RecommendationService(scoringService, new ExplanationService(scoringService));

    @Test
    void createsPublicRecommendationsWithoutInternalScores() {
        var response = service.recommend(ReserveInstrumentCatalog.INSTRUMENTS, standardRequest(), false);

        assertThat(response.userProfile()).isEqualTo(UserProfileType.BALANCED);
        assertThat(response.recommendations()).isNotEmpty();
        assertThat(response.recommendations()).allMatch(item -> item.moexUrl().contains(item.ticker()));
        assertThat(response.recommendations()).allMatch(item -> !item.explanation().isEmpty());
        assertThat(response.recommendations()).allMatch(item -> item.internalScores() == null);
    }

    @Test
    void includesInternalScoresOnlyForDebugResponse() {
        var response = service.recommend(ReserveInstrumentCatalog.INSTRUMENTS, standardRequest(), true);

        assertThat(response.recommendations()).allMatch(item -> item.internalScores() != null);
        assertThat(response.recommendations()).allMatch(item -> item.internalScores().finalScore() >= 0);
    }

    @Test
    void includesDerivativeWarningForAdvancedUsers() {
        var request = new RecommendationRequest(
            Goal.SPECULATION,
            RiskProfile.HIGH,
            Horizon.SHORT,
            100_000,
            Experience.ADVANCED,
            List.of(AssetClass.FUTURE, AssetClass.OPTION),
            3
        );

        var response = service.recommend(ReserveInstrumentCatalog.INSTRUMENTS, request, false);

        assertThat(response.userProfile()).isEqualTo(UserProfileType.PROFESSIONAL);
        assertThat(response.recommendations()).allMatch(item -> !item.warnings().isEmpty());
    }

    @Test
    void explainsMatchingBondHorizon() {
        var longBond = new Instrument(
            "LONG", "Длинная облигация", AssetClass.BOND, 100d, "RUB", 14d,
            100_000d, 10_000_000d, 8d, "AAA", LocalDate.now().plusYears(5).toString(), "TQOB", Map.of()
        );
        var request = new RecommendationRequest(
            Goal.CAPITAL_PRESERVATION,
            RiskProfile.LOW,
            Horizon.LONG,
            100_000,
            Experience.BEGINNER,
            List.of(AssetClass.BOND),
            2
        );

        var response = service.recommend(List.of(longBond), request, false);

        assertThat(response.recommendations().get(0).explanation())
            .contains("Подходит под выбранный горизонт инвестирования.");
    }

    private RecommendationRequest standardRequest() {
        return new RecommendationRequest(
            Goal.CAPITAL_GROWTH,
            RiskProfile.MEDIUM,
            Horizon.LONG,
            100_000,
            Experience.BEGINNER,
            List.of(AssetClass.STOCK, AssetClass.BOND),
            10
        );
    }
}
