export type Medal = "NONE" | "BRONZE" | "SILVER" | "GOLD";

export interface EvaluationRecord {
  id: string;
  status: string;
  updatedAt: string;
  title: string;
  gameCode?: string;
  volume?: number;
  medal?: Medal;
  durationMs?: number;
  actualSteps?: number;
  optimalSteps?: number | null;
  passed?: boolean;
  performance?: string;
  suggestions: string[];
  limitations: string[];
  generationMode?: string;
  errorCode?: string | null;
}

export interface RankingEntry {
  id: string;
  name: string;
  score: number;
  rank: number;
}

export interface AbilityValue {
  name: string;
  value: number;
}

export interface EvaluationResponse {
  id: string;
  status: string;
  updated_at: string;
  error_code?: string | null;
  report: null | {
    facts?: {
      game_code: string;
      volume_no: number;
      badge: Medal;
      duration_ms: number;
      actual_steps: number;
      optimal_steps: number | null;
      passed: boolean;
    };
    evaluation?: {
      performance: string;
      suggestions: string[];
      limitations?: string[];
    };
    generation_mode?: string;
  };
}
