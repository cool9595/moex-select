package com.moexdelta.moexselect.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.model.Instrument;

@Service
public class MoexIssClient {

    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(9);
    private static final Map<AssetClass, String> ISS_ENDPOINTS = new EnumMap<>(AssetClass.class);

    static {
        ISS_ENDPOINTS.put(AssetClass.STOCK, "/iss/engines/stock/markets/shares/boards/TQBR/securities.json?iss.meta=off");
        ISS_ENDPOINTS.put(AssetClass.BOND, "/iss/engines/stock/markets/bonds/securities.json?iss.meta=off");
        ISS_ENDPOINTS.put(AssetClass.FUTURE, "/iss/engines/futures/markets/forts/securities.json?iss.meta=off");
        ISS_ENDPOINTS.put(AssetClass.OPTION, "/iss/engines/futures/markets/options/securities.json?iss.meta=off");
    }

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final InstrumentNormalizationService normalizationService;

    public MoexIssClient(
        WebClient moexIssWebClient,
        ObjectMapper objectMapper,
        InstrumentNormalizationService normalizationService
    ) {
        this.webClient = moexIssWebClient;
        this.objectMapper = objectMapper;
        this.normalizationService = normalizationService;
    }

    public List<Instrument> fetchAssetClass(AssetClass assetClass) {
        try {
            JsonNode payload = webClient.get()
                .uri(ISS_ENDPOINTS.get(assetClass))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(RESPONSE_TIMEOUT);
            if (payload == null) {
                return List.of();
            }
            return normalizationService.normalizeRows(
                mergeBySecid(tableToDicts(payload.get("securities")), tableToDicts(payload.get("marketdata"))),
                assetClass
            );
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    List<Map<String, Object>> tableToDicts(JsonNode table) {
        if (table == null || !table.has("columns") || !table.has("data")) {
            return List.of();
        }
        var columns = table.get("columns");
        var rows = new ArrayList<Map<String, Object>>();
        for (JsonNode values : table.get("data")) {
            if (!values.isArray()) {
                continue;
            }
            var row = new LinkedHashMap<String, Object>();
            for (int index = 0; index < columns.size() && index < values.size(); index++) {
                row.put(columns.get(index).asText(), objectMapper.convertValue(values.get(index), Object.class));
            }
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> mergeBySecid(
        List<Map<String, Object>> securities,
        List<Map<String, Object>> marketData
    ) {
        var marketBySecid = new HashMap<String, Map<String, Object>>();
        for (var row : marketData) {
            if (row.get("SECID") != null) {
                marketBySecid.put(row.get("SECID").toString(), row);
            }
        }
        if (securities.isEmpty()) {
            return marketData;
        }
        var result = new ArrayList<Map<String, Object>>();
        for (var security : securities) {
            var merged = new LinkedHashMap<>(security);
            var marketRow = marketBySecid.get(String.valueOf(security.get("SECID")));
            if (marketRow != null) {
                merged.putAll(marketRow);
            }
            result.add(merged);
        }
        return result;
    }
}
