export type AssetClass = 'STOCK' | 'BOND' | 'FUTURE' | 'OPTION';
export type Goal = 'CAPITAL_PRESERVATION' | 'STABLE_INCOME' | 'CAPITAL_GROWTH' | 'SPECULATION';
export type RiskProfile = 'LOW' | 'MEDIUM' | 'HIGH';
export type Horizon = 'SHORT' | 'MEDIUM' | 'LONG';
export type DisplayLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type ConfidenceLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type InvestmentScenario =
  | 'CAPITAL_PRESERVATION'
  | 'STABLE_INCOME'
  | 'CAPITAL_GROWTH'
  | 'SHORT_TERM_LIQUIDITY'
  | 'SPECULATION';
export type SortBy =
  | 'ticker'
  | 'name'
  | 'price'
  | 'yield'
  | 'volume'
  | 'turnover'
  | 'liquidity'
  | 'maturity'
  | 'volatility'
  | 'marketCap'
  | 'strikePrice';
export type SortDirection = 'asc' | 'desc';

export interface RecommendationRequest {
  goal: Goal;
  riskProfile: RiskProfile;
  horizon: Horizon;
  budget: number;
  experience?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
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
  confidenceLevel: ConfidenceLevel;
  scenario: InvestmentScenario;
  summary: string;
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

export interface Instrument {
  ticker: string;
  name: string;
  assetClass: AssetClass;
  price?: number | null;
  currency?: string | null;
  yieldValue?: number | null;
  volume?: number | null;
  turnover?: number | null;
  volatility?: number | null;
  creditRating?: string | null;
  maturityDate?: string | null;
  board?: string | null;
  marketCap?: number | null;
  optionType?: string | null;
  strikePrice?: number | null;
  moexUrl: string;
}

export interface InstrumentSearchParams {
  assetClass?: AssetClass | '';
  query: string;
  page: number;
  limit: number;
  sortBy: SortBy;
  sortDirection: SortDirection;
  minPrice?: number | '';
  maxPrice?: number | '';
  minYield?: number | '';
  maxYield?: number | '';
  minVolume?: number | '';
  maxVolume?: number | '';
  minTurnover?: number | '';
  maxTurnover?: number | '';
  minVolatility?: number | '';
  maxVolatility?: number | '';
  maturityFrom?: string;
  maturityTo?: string;
  minMarketCap?: number | '';
  maxMarketCap?: number | '';
  optionType?: string;
  minStrikePrice?: number | '';
  maxStrikePrice?: number | '';
}

export interface InstrumentSearchResponse {
  items: Instrument[];
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}
