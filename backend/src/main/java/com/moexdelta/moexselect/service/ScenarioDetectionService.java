package com.moexdelta.moexselect.service;

import org.springframework.stereotype.Service;

import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.enums.Goal;
import com.moexdelta.moexselect.enums.Horizon;
import com.moexdelta.moexselect.enums.InvestmentScenario;
import com.moexdelta.moexselect.enums.RiskProfile;

@Service
public class ScenarioDetectionService {

    public InvestmentScenario detect(RecommendationRequest request) {
        if (request.goal() == Goal.SPECULATION) {
            return InvestmentScenario.SPECULATION;
        }
        if (request.horizon() == Horizon.SHORT
            && (request.riskProfile() == RiskProfile.LOW || request.riskProfile() == RiskProfile.MEDIUM)) {
            return InvestmentScenario.SHORT_TERM_LIQUIDITY;
        }
        if (request.goal() == Goal.CAPITAL_PRESERVATION) {
            return InvestmentScenario.CAPITAL_PRESERVATION;
        }
        if (request.goal() == Goal.STABLE_INCOME) {
            return InvestmentScenario.STABLE_INCOME;
        }
        return InvestmentScenario.CAPITAL_GROWTH;
    }
}
