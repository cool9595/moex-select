export type AssetClass = 'STOCK' | 'BOND' | 'FUTURE' | 'OPTION';
export type Goal = 'CAPITAL_PRESERVATION' | 'STABLE_INCOME' | 'CAPITAL_GROWTH' | 'SPECULATION';
export type RiskProfile = 'LOW' | 'MEDIUM' | 'HIGH';
export type Horizon = 'SHORT' | 'MEDIUM' | 'LONG';
export type Experience = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export type DisplayLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface RecommendationRequest {
  goal: Goal;
  riskProfile: RiskProfile;
  horizon: Horizon;
  budget: number;
  experience: Experience;
  assetClasses: AssetClass[];
  limit: number;
}

export interface Recommendation {
  ticker: string;
  name: string;
  assetClass: AssetClass;
  price?: number | null;
  currency?: string | null;
  riskLevel: DisplayLevel;
  liquidityLevel: DisplayLevel;
  profileMatch: boolean;
  yieldValue?: number | null;
  maturityDate?: string | null;
  explanation: string[];
  warnings: string[];
  moexUrl: string;
}

export interface RecommendationResponse {
  userProfile: 'CONSERVATIVE' | 'BALANCED' | 'AGGRESSIVE' | 'PROFESSIONAL';
  disclaimer: string;
  recommendations: Recommendation[];
}
