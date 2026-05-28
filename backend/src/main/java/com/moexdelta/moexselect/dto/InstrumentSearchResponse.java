package com.moexdelta.moexselect.dto;

import java.util.List;

public record InstrumentSearchResponse(
    List<InstrumentDto> items,
    int page,
    int limit,
    long total,
    int totalPages
) {
}
