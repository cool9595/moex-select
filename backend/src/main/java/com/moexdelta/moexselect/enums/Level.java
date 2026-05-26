package com.moexdelta.moexselect.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Level {
    LOW, MEDIUM, HIGH;

    @JsonValue
    public String value() {
        return name();
    }
}
