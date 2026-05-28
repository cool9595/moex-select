package com.moexdelta.moexselect.dto;

import java.time.LocalDate;

import com.moexdelta.moexselect.enums.AssetClass;

public record InstrumentSearchCriteria(
    AssetClass assetClass,
    String query,
    int limit,
    int page,
    String sortBy,
    String sortDirection,
    Double minPrice,
    Double maxPrice,
    Double minYield,
    Double maxYield,
    Double minVolume,
    Double maxVolume,
    Double minTurnover,
    Double maxTurnover,
    Double minVolatility,
    Double maxVolatility,
    LocalDate maturityFrom,
    LocalDate maturityTo,
    Double minMarketCap,
    Double maxMarketCap,
    String optionType,
    Double minStrikePrice,
    Double maxStrikePrice
) {
}
