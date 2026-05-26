package com.moexdelta.moexselect.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.model.Instrument;

@Service
public class InstrumentNormalizationService {

    public List<Instrument> normalizeRows(List<Map<String, Object>> rows, AssetClass assetClass) {
        return rows.stream()
            .map(row -> normalizeInstrument(row, assetClass))
            .filter(instrument -> instrument != null)
            .toList();
    }

    Instrument normalizeInstrument(Map<String, Object> row, AssetClass assetClass) {
        var ticker = firstString(row, "SECID");
        if (ticker == null) {
            return null;
        }
        return new Instrument(
            ticker,
            defaultValue(firstString(row, "SHORTNAME", "SECNAME", "NAME"), ticker),
            assetClass,
            firstDouble(row, "LAST", "MARKETPRICE", "PREVPRICE", "LCURRENTPRICE"),
            normalizeCurrency(firstString(row, "CURRENCYID", "FACEUNIT", "CURRENCY")),
            normalizeYield(firstDouble(row, "YIELD", "YIELDATPREVWAPRICE", "EFFECTIVEYIELD")),
            firstDouble(row, "VOLTODAY", "VOLUME"),
            firstDouble(row, "VALTODAY", "TURNOVER", "VALUE"),
            null,
            upper(firstString(row, "CREDITRATING", "RATING", "RATINGGROUP")),
            normalizeMaturityDate(firstString(row, "MATDATE", "MATURITYDATE", "MATUREDATE")),
            firstString(row, "BOARDID"),
            row
        );
    }

    String normalizeMaturityDate(String maturityDate) {
        if (maturityDate == null || maturityDate.startsWith("0000-")) {
            return null;
        }
        try {
            return LocalDate.parse(maturityDate).toString();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    Double normalizeYield(Double yieldValue) {
        if (yieldValue == null || yieldValue < 0 || yieldValue > 100) {
            return null;
        }
        return yieldValue;
    }

    private String firstString(Map<String, Object> row, String... keys) {
        for (var key : keys) {
            var value = row.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    private Double firstDouble(Map<String, Object> row, String... keys) {
        for (var key : keys) {
            var value = row.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value != null) {
                try {
                    return Double.valueOf(value.toString());
                } catch (NumberFormatException ignored) {
                    // MOEX ISS uses different optional fields for different markets.
                }
            }
        }
        return null;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null) {
            return null;
        }
        return switch (currency.toUpperCase(Locale.ROOT)) {
            case "SUR", "RUR" -> "RUB";
            default -> currency.toUpperCase(Locale.ROOT);
        };
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private String defaultValue(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
