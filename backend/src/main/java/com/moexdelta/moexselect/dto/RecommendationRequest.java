package com.moexdelta.moexselect.dto;

import java.util.List;

import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.enums.Experience;
import com.moexdelta.moexselect.enums.Goal;
import com.moexdelta.moexselect.enums.Horizon;
import com.moexdelta.moexselect.enums.RiskProfile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RecommendationRequest(
    @NotNull Goal goal,
    @NotNull RiskProfile riskProfile,
    @NotNull Horizon horizon,
    @DecimalMin("0.0") double budget,
    Experience experience,
    @NotEmpty List<AssetClass> assetClasses,
    @Min(1) @Max(50) Integer limit
) {
    public RecommendationRequest {
        limit = limit == null ? 10 : limit;
        experience = experience == null ? Experience.BEGINNER : experience;
        assetClasses = assetClasses == null ? List.of() : List.copyOf(assetClasses);
    }
}
