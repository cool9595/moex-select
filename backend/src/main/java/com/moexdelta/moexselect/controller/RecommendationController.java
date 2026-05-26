package com.moexdelta.moexselect.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moexdelta.moexselect.dto.RecommendationRequest;
import com.moexdelta.moexselect.dto.RecommendationResponse;
import com.moexdelta.moexselect.service.InstrumentService;
import com.moexdelta.moexselect.service.RecommendationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final InstrumentService instrumentService;
    private final RecommendationService recommendationService;

    public RecommendationController(
        InstrumentService instrumentService,
        RecommendationService recommendationService
    ) {
        this.instrumentService = instrumentService;
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public RecommendationResponse recommend(
        @Valid @RequestBody RecommendationRequest request,
        @RequestParam(defaultValue = "false") boolean debug
    ) {
        return recommendationService.recommend(instrumentService.findAll(), request, debug);
    }
}
