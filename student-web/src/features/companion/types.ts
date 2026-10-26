export interface CompanionGame {
  code: string;
  name: string;
}

export interface Citation {
  kind: "prompt" | "text" | "image" | "report";
  source_id: string;
  title: string;
  version?: string | null;
}

export interface ChatPayload {
  request_id: string;
  conversation_id?: string;
  game_code?: string;
  evaluation_id?: string;
  question: string;
}

export interface ChatAnswer {
  request_id: string;
  conversation_id: string;
  game_code?: string | null;
  answer: string;
  citations: Citation[];
  scene?: "GENERAL_CHAT" | "GAME_QA" | "REPORT_QA";
  needs_clarification?: boolean;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  text: string;
  citations?: Citation[];
  preview?: boolean;
}

export interface CompanionPreferences {
  name: string;
  avatar: string;
  voice: "gentle" | "energetic" | "clear";
}

export interface TutorReport {
  id: string;
  status: string;
  updated_at: string;
  report: {
    facts: {
      game_code: string;
      volume_no: number;
      badge: string;
      passed: boolean;
      duration_ms: number;
      actual_steps: number;
    };
    evaluation: {
      performance: string;
      suggestions: string[];
      limitations: string[];
    };
  } | null;
}
