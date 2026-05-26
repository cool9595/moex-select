package com.moexdelta.moexselect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.moexdelta.moexselect.dto.InstrumentDto;
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
    public List<InstrumentDto> instruments(
        @RequestParam(required = false) String assetClass,
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "ticker") String sortBy
    ) {
        if (limit < 1 || limit > 200 || page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        var parsedAssetClass = parseAssetClass(assetClass);
        return instrumentService.search(parsedAssetClass, query, limit, page, sortBy).stream()
            .map(InstrumentDto::fromInstrument)
            .toList();
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
