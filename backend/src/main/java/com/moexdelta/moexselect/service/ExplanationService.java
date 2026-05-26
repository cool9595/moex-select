package com.moexdelta.moexselect.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moexdelta.moexselect.dto.InternalScores;
import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.enums.AssetClass;
import com.moexdelta.moexselect.enums.Experience;
import com.moexdelta.moexselect.model.Instrument;

@Service
public class ExplanationService {

    private final ScoringService scoringService;

    public ExplanationService(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    public List<String> explanations(
        Instrument instrument,
        RecommendationRequest request,
        InternalScores scores
    ) {
        var result = new ArrayList<String>();
        if (scores.fitScore() > 80) {
            result.add("Соответствует выбранному уровню риска.");
        } else {
            result.add("Учитывает выбранный уровень риска в составе подборки.");
        }
        if (scoringService.horizonFit(instrument, request.horizon()) > 80) {
            result.add("Подходит под выбранный горизонт инвестирования.");
        }
        if (scores.liquidityScore() > 70) {
            result.add("Имеет достаточную ликвидность для частного инвестора.");
        }
        if (scores.yieldScore() > 70) {
            result.add("Доходность находится среди более привлекательных в выбранной группе инструментов.");
        }
        if (instrument.assetClass() == AssetClass.BOND && scores.creditQualityScore() > 70) {
            result.add("Кредитное качество соответствует консервативному профилю.");
        }
        if (scoringService.isDerivative(instrument) && request.experience() == Experience.ADVANCED) {
            result.add("Инструмент отображается только для пользователей с продвинутым опытом.");
        }
        if (result.size() < 2) {
            result.add("Включен в подборку по совокупности выбранных параметров.");
        }
        return result.stream().limit(4).toList();
    }

    public List<String> warnings(Instrument instrument) {
        return scoringService.isDerivative(instrument)
            ? List.of("Инструмент относится к сложным финансовым инструментам и подходит только пользователям с соответствующим опытом.")
            : List.of();
    }
}
