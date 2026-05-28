package com.moexdelta.moexselect.model;

import java.util.Map;

import com.moexdelta.moexselect.enums.AssetClass;

public record Instrument(
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
    Map<String, Object> raw
) {
}
