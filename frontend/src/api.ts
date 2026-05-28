import type { InstrumentSearchParams, InstrumentSearchResponse, RecommendationRequest, RecommendationResponse } from './types';

const API_BASE_URL = 'http://localhost:8000/api';

async function requestJson<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!response.ok) {
    throw new Error(`API error ${response.status}: ${response.statusText}`);
  }

  return response.json() as Promise<T>;
}

export function getRecommendations(payload: RecommendationRequest): Promise<RecommendationResponse> {
  return requestJson<RecommendationResponse>('/recommendations', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function searchInstruments(params: InstrumentSearchParams): Promise<InstrumentSearchResponse> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value));
    }
  });
  return requestJson<InstrumentSearchResponse>(`/instruments?${query.toString()}`);
}
