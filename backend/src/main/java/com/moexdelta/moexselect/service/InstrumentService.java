package com.moexdelta.moexselect.service;

import java.time.LocalDate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moexdelta.moexselect.data.ReserveInstrumentCatalog;
import com.moexdelta.moexselect.dto.InstrumentSearchCriteria;
import com.moexdelta.moexselect.dto.InstrumentSearchResponse;
import com.moexdelta.moexselect.dto.InstrumentDto;
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

    public InstrumentSearchResponse search(InstrumentSearchCriteria criteria) {
        var filtered = findAll().stream()
            .filter(instrument -> matches(instrument, criteria))
            .sorted(comparator(criteria.sortBy(), criteria.sortDirection()))
            .toList();
        var total = filtered.size();
        var fromIndex = Math.min(criteria.page() * criteria.limit(), total);
        var toIndex = Math.min(fromIndex + criteria.limit(), total);
        var items = filtered.subList(fromIndex, toIndex).stream()
            .map(InstrumentDto::fromInstrument)
            .toList();
        var totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / criteria.limit());
        return new InstrumentSearchResponse(items, criteria.page(), criteria.limit(), total, totalPages);
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

    private boolean matches(Instrument instrument, InstrumentSearchCriteria criteria) {
        var search = criteria.query() == null ? null : criteria.query().toLowerCase(Locale.ROOT);
        return (criteria.assetClass() == null || instrument.assetClass() == criteria.assetClass())
            && (search == null || search.isBlank()
                || instrument.ticker().toLowerCase(Locale.ROOT).contains(search)
                || instrument.name().toLowerCase(Locale.ROOT).contains(search))
            && between(instrument.price(), criteria.minPrice(), criteria.maxPrice())
            && between(instrument.yieldValue(), criteria.minYield(), criteria.maxYield())
            && between(instrument.volume(), criteria.minVolume(), criteria.maxVolume())
            && between(instrument.turnover(), criteria.minTurnover(), criteria.maxTurnover())
            && between(instrument.volatility(), criteria.minVolatility(), criteria.maxVolatility())
            && between(instrument.marketCap(), criteria.minMarketCap(), criteria.maxMarketCap())
            && between(instrument.strikePrice(), criteria.minStrikePrice(), criteria.maxStrikePrice())
            && dateBetween(instrument.maturityDate(), criteria.maturityFrom(), criteria.maturityTo())
            && (criteria.optionType() == null || criteria.optionType().isBlank()
                || criteria.optionType().equalsIgnoreCase(instrument.optionType()));
    }

    private Comparator<Instrument> comparator(String sortBy, String sortDirection) {
        var descending = "desc".equalsIgnoreCase(sortDirection);
        Comparator<Instrument> comparator = switch (sortBy == null ? "" : sortBy.toLowerCase(Locale.ROOT)) {
            case "name" -> Comparator.comparing(Instrument::name, String.CASE_INSENSITIVE_ORDER);
            case "price" -> comparingNullableDouble(Instrument::price, descending);
            case "yield" -> comparingNullableDouble(Instrument::yieldValue, descending);
            case "volume" -> comparingNullableDouble(Instrument::volume, descending);
            case "turnover", "liquidity" -> comparingNullableDouble(Instrument::turnover, descending);
            case "maturity" -> comparingNullableDate(descending);
            case "volatility" -> comparingNullableDouble(Instrument::volatility, descending);
            case "marketcap", "capitalization" -> comparingNullableDouble(Instrument::marketCap, descending);
            case "strike", "strikeprice" -> comparingNullableDouble(Instrument::strikePrice, descending);
            default -> Comparator.comparing(Instrument::ticker, String.CASE_INSENSITIVE_ORDER);
        };
        return descending && isTextSort(sortBy) ? comparator.reversed() : comparator;
    }

    private Comparator<Instrument> comparingNullableDouble(
        java.util.function.Function<Instrument, Double> extractor,
        boolean descending
    ) {
        var valueComparator = descending ? Comparator.<Double>nullsLast(Comparator.reverseOrder()) : Comparator.nullsLast(Double::compareTo);
        return Comparator.comparing(extractor, valueComparator);
    }

    private Comparator<Instrument> comparingNullableDate(boolean descending) {
        var valueComparator = descending ? Comparator.<LocalDate>nullsLast(Comparator.reverseOrder()) : Comparator.nullsLast(LocalDate::compareTo);
        return Comparator.comparing(instrument -> parseDate(instrument.maturityDate()), valueComparator);
    }

    private boolean isTextSort(String sortBy) {
        var normalized = sortBy == null ? "" : sortBy.toLowerCase(Locale.ROOT);
        return normalized.isBlank() || "ticker".equals(normalized) || "name".equals(normalized);
    }

    private boolean between(Double value, Double minimum, Double maximum) {
        return (minimum == null || value != null && value >= minimum)
            && (maximum == null || value != null && value <= maximum);
    }

    private boolean dateBetween(String value, LocalDate from, LocalDate to) {
        var date = parseDate(value);
        return (from == null || date != null && !date.isBefore(from))
            && (to == null || date != null && !date.isAfter(to));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
