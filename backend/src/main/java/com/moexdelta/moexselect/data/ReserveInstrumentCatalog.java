package com.moexdelta.moexselect.data;

import java.util.List;
import java.util.Map;

import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.model.Instrument;

public final class ReserveInstrumentCatalog {

    private static final Map<String, Object> RAW_FIELDS = Map.of();

    public static final List<Instrument> INSTRUMENTS = List.of(
        instrument("SBER", "Сбербанк", AssetClass.STOCK, 315.4, "RUB", 8.2, 18_500_000, 5_820_000_000d, 26d, null, null),
        instrument("GAZP", "Газпром", AssetClass.STOCK, 142.1, "RUB", 7.1, 13_200_000, 1_870_000_000d, 31d, null, null),
        instrument("LKOH", "Лукойл", AssetClass.STOCK, 7460d, "RUB", 9.4, 760_000, 5_660_000_000d, 24d, null, null),
        instrument("AFLT", "Аэрофлот", AssetClass.STOCK, 62.8, "RUB", null, 5_200_000, 326_000_000d, 42d, null, null),
        instrument("MGNT", "Магнит", AssetClass.STOCK, 6180d, "RUB", 6.8, 140_000, 865_000_000d, 22d, null, null),
        instrument("RU000A1014L8", "ОФЗ 26235", AssetClass.BOND, 94.7, "RUB", 13.8, 1_200_000, 113_600_000d, 7d, "AAA", "2031-03-12"),
        instrument("RU000A1038V6", "ОФЗ 26238", AssetClass.BOND, 77.2, "RUB", 14.4, 820_000, 63_300_000d, 9d, "AAA", "2041-05-15"),
        instrument("RU000A105EX7", "Корпоративная облигация А", AssetClass.BOND, 99.1, "RUB", 15.2, 260_000, 25_700_000d, 11d, "AA", "2028-09-21"),
        instrument("RU000A106Z38", "Корпоративная облигация BBB", AssetClass.BOND, 96.4, "RUB", 18.7, 110_000, 10_600_000d, 15d, "BBB", "2027-11-02"),
        instrument("RU000A1079C8", "Муниципальная облигация", AssetClass.BOND, 98.8, "RUB", 14.1, 70_000, 6_910_000d, 8d, "A", "2029-06-18"),
        instrument("RU000A108JV8", "Высокодоходная облигация", AssetClass.BOND, 91.5, "RUB", 22.3, 45_000, 4_110_000d, 22d, "BB", "2026-12-04"),
        instrument("IMOEXFUT", "Фьючерс на индекс МосБиржи", AssetClass.FUTURE, 348_500d, "RUB", null, 220_000, 7_660_000_000d, 37d, null, null),
        instrument("SiM6", "Фьючерс USD/RUB", AssetClass.FUTURE, 91_250d, "RUB", null, 180_000, 16_400_000_000d, 33d, null, null),
        instrument("BRM6", "Фьючерс Brent", AssetClass.FUTURE, 82.4, "USD", null, 96_000, 790_000_000d, 45d, null, null),
        instrument("SBER-6.26-C320", "Call-опцион на SBER", AssetClass.OPTION, 12.5, "RUB", null, 21_000, 26_250_000d, 55d, null, null),
        instrument("GAZP-6.26-P140", "Put-опцион на GAZP", AssetClass.OPTION, 8.2, "RUB", null, 9_500, 7_790_000d, 61d, null, null),
        instrument("IMOEX-6.26-C3600", "Call-опцион на индекс МосБиржи", AssetClass.OPTION, 145d, "RUB", null, 7_800, 1_131_000d, 48d, null, null)
    );

    private ReserveInstrumentCatalog() {
    }

    private static Instrument instrument(
        String ticker,
        String name,
        AssetClass assetClass,
        Double price,
        String currency,
        Double yieldValue,
        double volume,
        double turnover,
        Double volatility,
        String creditRating,
        String maturityDate
    ) {
        var board = switch (assetClass) {
            case STOCK -> "TQBR";
            case BOND -> "TQOB";
            case FUTURE, OPTION -> null;
        };
        Double marketCap = switch (assetClass) {
            case STOCK -> turnover * 1200;
            default -> null;
        };
        String optionType = ticker.contains("-C") || name.toLowerCase().contains("call") ? "CALL"
            : ticker.contains("-P") || name.toLowerCase().contains("put") ? "PUT" : null;
        Double strikePrice = optionType == null ? null : extractStrike(ticker);
        return new Instrument(
            ticker, name, assetClass, price, currency, yieldValue, volume, turnover,
            volatility, creditRating, maturityDate, board, marketCap, optionType, strikePrice, RAW_FIELDS
        );
    }

    private static Double extractStrike(String ticker) {
        var index = ticker.lastIndexOf('-');
        if (index < 0 || index == ticker.length() - 1) {
            return null;
        }
        try {
            return Double.valueOf(ticker.substring(index + 1).replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
