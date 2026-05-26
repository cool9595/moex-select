package com.moexdelta.moexselect.dto;

import java.util.Map;

import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.model.Instrument;

public record InstrumentDto(
    String ticker,
    String name,
    AssetClass assetClass,
    Double price,
    String currency,
    Double yieldValue,
    Double volume,
    Double turnover,
    Double volatility,
    String creditRating,
    String maturityDate,
    String board,
    Map<String, Object> raw
) {
    public static InstrumentDto fromInstrument(Instrument instrument) {
        return new InstrumentDto(
            instrument.ticker(),
            instrument.name(),
            instrument.assetClass(),
            instrument.price(),
            instrument.currency(),
            instrument.yieldValue(),
            instrument.volume(),
            instrument.turnover(),
            instrument.volatility(),
            instrument.creditRating(),
            instrument.maturityDate(),
            instrument.board(),
            instrument.raw()
        );
    }
}
