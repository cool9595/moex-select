package com.moexdelta.moexselect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Horizon {
    SHORT, MEDIUM, LONG;

    @JsonValue
    public String value() {
        return name();
    }

    @JsonCreator
    public static Horizon fromValue(String value) {
        return value == null ? null : valueOf(value.toUpperCase());
    }
}
