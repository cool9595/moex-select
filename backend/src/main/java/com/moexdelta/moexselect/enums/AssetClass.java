package com.moexdelta.moexselect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssetClass {
    STOCK, BOND, FUTURE, OPTION;

    @JsonValue
    public String value() {
        return name();
    }

    @JsonCreator
    public static AssetClass fromValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "STOCK", "STOCKS" -> STOCK;
            case "BOND", "BONDS" -> BOND;
            case "FUTURE", "FUTURES" -> FUTURE;
            case "OPTION", "OPTIONS" -> OPTION;
            default -> throw new IllegalArgumentException("Unsupported asset class: " + value);
        };
    }
}
