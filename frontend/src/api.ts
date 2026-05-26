import type { RecommendationRequest, RecommendationResponse } from './types';

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
