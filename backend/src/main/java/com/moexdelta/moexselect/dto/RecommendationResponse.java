package com.moexdelta.moexselect.dto;

import java.util.List;

import com.moexdelta.moexselect.enums.UserProfileType;

public record RecommendationResponse(
    UserProfileType userProfile,
    String disclaimer,
    String profileSummary,
    List<PublicInstrumentRecommendation> recommendations
) {
}
