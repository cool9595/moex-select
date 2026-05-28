package com.moexdelta.moexselect.dto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.model.Instrument;

public record InstrumentDto(
    String ticker,
    String name,
    AssetClass assetClass,
    Double price,
    String currency,
    Double yieldValue,
    Double volume,
    Double turnover,
    Double volatility,
    String creditRating,
    String maturityDate,
    String board,
    Double marketCap,
    String optionType,
    Double strikePrice,
    String moexUrl,
    Map<String, Object> raw
) {
    public static InstrumentDto fromInstrument(Instrument instrument) {
        return new InstrumentDto(
            instrument.ticker(),
            instrument.name(),
            instrument.assetClass(),
            instrument.price(),
            instrument.currency(),
            instrument.yieldValue(),
            instrument.volume(),
            instrument.turnover(),
            instrument.volatility(),
            instrument.creditRating(),
            instrument.maturityDate(),
            instrument.board(),
            instrument.marketCap(),
            instrument.optionType(),
            instrument.strikePrice(),
            moexUrl(instrument),
            instrument.raw()
        );
    }

    private static String moexUrl(Instrument instrument) {
        var ticker = encoded(instrument.ticker());
        if (instrument.board() != null && !instrument.board().isBlank()) {
            return "https://www.moex.com/ru/issue.aspx?board=" + encoded(instrument.board()) + "&code=" + ticker;
        }
        return "https://www.moex.com/ru/issue.aspx?code=" + ticker;
    }

    private static String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
