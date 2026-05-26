package com.moexdelta.moexselect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Experience {
    BEGINNER, INTERMEDIATE, ADVANCED;

    @JsonValue
    public String value() {
        return name();
    }

    @JsonCreator
    public static Experience fromValue(String value) {
        return value == null ? null : valueOf(value.toUpperCase());
    }
}
