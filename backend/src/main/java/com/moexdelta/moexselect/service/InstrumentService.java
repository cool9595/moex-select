package com.moexdelta.moexselect.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moexdelta.moexselect.data.ReserveInstrumentCatalog;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.model.Instrument;

@Service
public class InstrumentService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final MoexIssClient moexIssClient;
    private List<Instrument> cachedInstruments;
    private Instant cacheUpdatedAt;

    public InstrumentService(MoexIssClient moexIssClient) {
        this.moexIssClient = moexIssClient;
    }

    public synchronized List<Instrument> findAll() {
        if (cachedInstruments != null && cacheUpdatedAt.plus(CACHE_TTL).isAfter(Instant.now())) {
            return cachedInstruments;
        }
        var instruments = new ArrayList<Instrument>();
        for (var assetClass : AssetClass.values()) {
            var loaded = moexIssClient.fetchAssetClass(assetClass);
            if (loaded.isEmpty()) {
                instruments.addAll(reserveFor(assetClass));
            } else {
                instruments.addAll(loaded);
            }
        }
        cachedInstruments = List.copyOf(instruments.isEmpty() ? ReserveInstrumentCatalog.INSTRUMENTS : instruments);
        cacheUpdatedAt = Instant.now();
        return cachedInstruments;
    }

    public List<Instrument> search(AssetClass assetClass, String query, int limit, int page, String sortBy) {
        var search = query == null ? null : query.toLowerCase(Locale.ROOT);
        var comparator = switch (sortBy == null ? "" : sortBy.toLowerCase(Locale.ROOT)) {
            case "name" -> Comparator.comparing(Instrument::name, String.CASE_INSENSITIVE_ORDER);
            case "price" -> Comparator.comparing(Instrument::price, Comparator.nullsLast(Double::compareTo));
            default -> Comparator.comparing(Instrument::ticker, String.CASE_INSENSITIVE_ORDER);
        };
        return findAll().stream()
            .filter(instrument -> assetClass == null || instrument.assetClass() == assetClass)
            .filter(instrument -> search == null || search.isBlank()
                || instrument.ticker().toLowerCase(Locale.ROOT).contains(search)
                || instrument.name().toLowerCase(Locale.ROOT).contains(search))
            .sorted(comparator)
            .skip((long) page * limit)
            .limit(limit)
            .toList();
    }

    public Optional<Instrument> findByTicker(String ticker) {
        return findAll().stream()
            .filter(instrument -> instrument.ticker().equalsIgnoreCase(ticker))
            .findFirst();
    }

    private List<Instrument> reserveFor(AssetClass assetClass) {
        return ReserveInstrumentCatalog.INSTRUMENTS.stream()
            .filter(instrument -> instrument.assetClass() == assetClass)
            .toList();
    }
}
