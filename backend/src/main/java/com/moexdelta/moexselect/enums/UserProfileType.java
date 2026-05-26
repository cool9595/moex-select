package com.moexdelta.moexselect.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserProfileType {
    CONSERVATIVE, BALANCED, AGGRESSIVE, PROFESSIONAL;

    @JsonValue
    public String value() {
        return name();
    }
}
