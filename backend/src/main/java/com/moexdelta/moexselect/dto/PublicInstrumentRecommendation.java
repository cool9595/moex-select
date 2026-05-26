package com.moexdelta.moexselect.dto;

import java.util.List;

import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.enums.Level;

public record PublicInstrumentRecommendation(
    String ticker,
    String name,
    AssetClass assetClass,
    Double price,
    String currency,
    Double yieldValue,
    String maturityDate,
    Level riskLevel,
    Level liquidityLevel,
    boolean profileMatch,
    List<String> explanation,
    List<String> warnings,
    String moexUrl,
    InternalScores internalScores
) {
}
