package com.moexdelta.moexselect.controller;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.moexdelta.moexselect.dto.InstrumentDto;
import com.moexdelta.moexselect.dto.InstrumentSearchCriteria;
import com.moexdelta.moexselect.dto.InstrumentSearchResponse;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.service.InstrumentService;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public InstrumentSearchResponse instruments(
        @RequestParam(required = false) String assetClass,
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "6") int limit,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "ticker") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDirection,
        @RequestParam(required = false) Double minPrice,
        @RequestParam(required = false) Double maxPrice,
        @RequestParam(required = false) Double minYield,
        @RequestParam(required = false) Double maxYield,
        @RequestParam(required = false) Double minVolume,
        @RequestParam(required = false) Double maxVolume,
        @RequestParam(required = false) Double minTurnover,
        @RequestParam(required = false) Double maxTurnover,
        @RequestParam(required = false) Double minVolatility,
        @RequestParam(required = false) Double maxVolatility,
        @RequestParam(required = false) LocalDate maturityFrom,
        @RequestParam(required = false) LocalDate maturityTo,
        @RequestParam(required = false) Double minMarketCap,
        @RequestParam(required = false) Double maxMarketCap,
        @RequestParam(required = false) String optionType,
        @RequestParam(required = false) Double minStrikePrice,
        @RequestParam(required = false) Double maxStrikePrice
    ) {
        if (limit < 1 || limit > 200 || page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        var parsedAssetClass = parseAssetClass(assetClass);
        var criteria = new InstrumentSearchCriteria(
            parsedAssetClass,
            query,
            limit,
            page,
            sortBy,
            sortDirection,
            minPrice,
            maxPrice,
            minYield,
            maxYield,
            minVolume,
            maxVolume,
            minTurnover,
            maxTurnover,
            minVolatility,
            maxVolatility,
            maturityFrom,
            maturityTo,
            minMarketCap,
            maxMarketCap,
            optionType,
            minStrikePrice,
            maxStrikePrice
        );
        return instrumentService.search(criteria);
    }

    @GetMapping("/{ticker}")
    public InstrumentDto instrument(@PathVariable String ticker) {
        return instrumentService.findByTicker(ticker)
            .map(InstrumentDto::fromInstrument)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
    }

    private AssetClass parseAssetClass(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AssetClass.fromValue(value);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported asset class");
        }
    }
}
