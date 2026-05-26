package com.moexdelta.moexselect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Goal {
    CAPITAL_PRESERVATION, STABLE_INCOME, CAPITAL_GROWTH, SPECULATION;

    @JsonValue
    public String value() {
        return name();
    }

    @JsonCreator
    public static Goal fromValue(String value) {
        return value == null ? null : valueOf(value.toUpperCase());
    }
}
