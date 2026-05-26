package com.moexdelta.moexselect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RiskProfile {
    LOW, MEDIUM, HIGH;

    @JsonValue
    public String value() {
        return name();
    }

    @JsonCreator
    public static RiskProfile fromValue(String value) {
        return value == null ? null : valueOf(value.toUpperCase());
    }
}
