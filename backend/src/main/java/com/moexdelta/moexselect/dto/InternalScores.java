package com.moexdelta.moexselect.dto;

public record InternalScores(
    double finalScore,
    double liquidityScore,
    double yieldScore,
    double riskScore,
    double creditQualityScore,
    double fitScore
) {
}
