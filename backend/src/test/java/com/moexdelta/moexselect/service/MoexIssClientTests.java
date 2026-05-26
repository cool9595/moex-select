package com.moexdelta.moexselect.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moexdelta.moexselect.enums.AssetClass;

class MoexIssClientTests {

    @Test
    void convertsIssTablesToMaps() throws Exception {
        var client = new MoexIssClient(WebClient.create(), new ObjectMapper(), new InstrumentNormalizationService());
        var table = new ObjectMapper().readTree("""
            {
              "columns": ["SECID", "LAST", "CURRENCYID"],
              "data": [["SBER", 315.4, "RUB"]]
            }
            """);

        var result = client.tableToDicts(table);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("SECID", "SBER");
        assertThat(result.get(0)).containsEntry("LAST", 315.4);
    }

    @Test
    void rejectsPlaceholderMaturityDatesFromIss() {
        var normalizationService = new InstrumentNormalizationService();

        assertThat(normalizationService.normalizeMaturityDate("0000-00-00")).isNull();
        assertThat(normalizationService.normalizeMaturityDate("2029-08-15")).isEqualTo("2029-08-15");
    }

    @Test
    void omitsAnomalousYieldValuesFromMarketPayloads() {
        var normalizationService = new InstrumentNormalizationService();

        assertThat(normalizationService.normalizeYield(14.6)).isEqualTo(14.6);
        assertThat(normalizationService.normalizeYield(658.86)).isNull();
    }

    @Test
    void prefersDailyLiquidityTotalsToLastTradeValues() {
        var normalizationService = new InstrumentNormalizationService();
        var instrument = normalizationService.normalizeInstrument(
            Map.of("SECID", "SBER", "VOLUME", 1, "VOLTODAY", 1500, "VALUE", 320, "VALTODAY", 480000),
            AssetClass.STOCK
        );

        assertThat(instrument.volume()).isEqualTo(1500d);
        assertThat(instrument.turnover()).isEqualTo(480000d);
    }
}
