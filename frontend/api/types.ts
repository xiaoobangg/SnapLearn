// ===== v1 types (kept for backward compat) =====

export interface OCRResponse {
  text: string;
  words: string[];
}

export interface CardResponse {
  id: string;
  word: string;
  general_meaning?: string;
  extended_meaning?: string;
  example_sentence?: string;
  memory_tip?: string;
  pos?: string;
  pronunciation?: string;
  created_at: string;
}

export interface CardGroupResponse {
  id: string;
  title?: string;
  source_image?: string;
  source_text: string;
  created_at: string;
  cards: CardResponse[];
}

export interface CardCreateRequest {
  source_image?: string;
  source_text: string;
  selected_words: string[];
}

export interface ReviewCard {
  notebook_id: string;
  card_id: string;
  word: string;
  general_meaning?: string;
  extended_meaning?: string;
  example_sentence?: string;
  memory_tip?: string;
  pronunciation?: string;
  pos?: string;
}

/** @deprecated v2 — use PoolWord instead */
export interface NotebookItem {
  id: string;
  card_id: string;
  word: string;
  general_meaning?: string;
  pos?: string;
  status: string;
  ease_factor: number;
  interval_days: number;
  next_review_at: string;
  last_review_at?: string;
}

// ===== v2 new types =====

export interface KnowledgePoint {
  id: string;
  word_id: string;
  card_id: string;
  point_type: string; // pronunciation / pos / general_meaning / extended_meaning / example_sentence / memory_tip
  content: string;
  sort_order: number;
  status: string; // unshown / shown / confirmed
}

export interface TestQuestion {
  id: string;
  group_id: string;
  card_id: string;
  question_type: string; // meaning_select / word_select / collocation / spelling
  question_text: string;
  options: string; // JSON array string
  correct_answer: string;
  sort_order: number;
}

export interface TestResult {
  questions: TestResultItem[];
  total: number;
  correct: number;
  all_correct: boolean;
  group_status: string;
}

export interface TestResultItem {
  question_id: string;
  card_id: string;
  word: string;
  type: string;
  question: string;
  options: string;
  correct_answer: string;
  user_answer: string;
  is_correct: boolean;
}

export interface CheckinWord {
  pool_id: string;
  word_id: string;
  word_text: string;
  pronunciation?: string;
  pos?: string;
  general_meaning?: string;
  pool_status: string;
  review_count: number;
}

export interface WordBank {
  id: string;
  name: string;
  type: string; // preset / user
  description?: string;
  created_by: string;
  created_at: string;
}

export interface CheckinCalendar {
  year: number;
  month: number;
  checkin_days: string[];
  total_days: number;
  day_stats: Record<string, { new_words: number; review_words: number }>;
}

export interface CheckinStats {
  total_checkin_days: number;
  consecutive_days: number;
  total_pool_words: number;
  mastered_count: number;
}

export interface UserSettings {
  id: string;
  user_id: string;
  daily_new_words: number;
  daily_review_words: number;
  checkin_reminder: boolean;
  reminder_time?: string;
  chat_mode?: string;
  chat_model?: string;
  chat_stream?: boolean;
}

// 确保编译后生成 JS 文件，避免小程序模块加载失败
export const __types_runtime = 1;
