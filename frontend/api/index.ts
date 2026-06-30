import { request } from "@/utils/request";
import type {
  OCRResponse,
  CardGroupResponse,
  CardCreateRequest,
  ReviewCard,
  NotebookItem,
  TestQuestion,
  TestResult,
  CheckinWord,
  WordBank,
  CheckinCalendar,
  CheckinStats,
  UserSettings,
} from "./types";
import { __types_runtime } from "./types";
void __types_runtime;

import { getApiBaseUrl } from "@/config";

export const api = {
  // ===== OCR =====
  recognizeImage(filePath: string): Promise<OCRResponse> {
    const url = `${getApiBaseUrl()}/ocr/recognize`;
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url,
        filePath,
        name: "image",
        success: (res: any) => {
          const data = JSON.parse(res.data);
          if (res.statusCode === 200) {
            resolve(data as OCRResponse);
          } else {
            reject(new Error("识别失败"));
          }
        },
        fail: (err) => {
          reject(new Error(err.errMsg || "上传失败"));
        },
      });
    });
  },

  // ===== 卡片组 =====
  createCardGroup(data: CardCreateRequest): Promise<CardGroupResponse> {
    return request<CardGroupResponse>({ url: "/card-groups", method: "POST", data });
  },

  listCardGroups(includeCompleted = false): Promise<any[]> {
    return request<any[]>({ url: "/card-groups", data: { includeCompleted: includeCompleted } });
  },

  getCardGroup(id: string): Promise<CardGroupResponse> {
    return request<CardGroupResponse>({ url: `/card-groups/${id}` });
  },

  deleteCardGroup(id: string): Promise<void> {
    return request<void>({ url: `/card-groups/${id}`, method: "DELETE" });
  },

  moveCard(cardId: string, targetGroupId?: string, newGroupTitle?: string): Promise<any> {
    return request<any>({
      url: `/card-groups/cards/${cardId}/move`,
      method: "POST",
      data: {
        target_group_id: targetGroupId || undefined,
        new_group_title: newGroupTitle || undefined,
      },
    });
  },

  // ===== v2 卡片组学习流程 =====
  startLearning(groupId: string): Promise<{ status: string }> {
    return request({ url: `/card-groups/${groupId}/start-learning`, method: "POST" });
  },

  markCard(cardId: string, mastered: boolean): Promise<{ card_status: string; group_status: string }> {
    return request({ url: `/card-groups/cards/${cardId}/mark?mastered=${mastered}`, method: "POST" });
  },

  getLearnStatus(groupId: string): Promise<{ total_cards: number; mastered: number; relearn: number; unlearned: number }> {
    return request({ url: `/card-groups/${groupId}/learn-status` });
  },

  // ===== 测验 =====
  startTest(groupId: string): Promise<{ questions: TestQuestion[]; total: number }> {
    return request({ url: `/test/groups/${groupId}/start`, method: "POST" });
  },

  submitTest(groupId: string, answers: { questionId: string; userAnswer: string }[]): Promise<{
    total: number; correct: number; all_correct: boolean; round: number; error_card_ids: string[];
  }> {
    return request({ url: "/test/submit", method: "POST", data: { group_id: groupId, answers } });
  },

  getTestResult(groupId: string): Promise<TestResult> {
    return request({ url: `/test/groups/${groupId}/result` });
  },

  retryTest(groupId: string): Promise<{ questions: TestQuestion[]; total: number }> {
    return request({ url: `/test/groups/${groupId}/retry`, method: "POST" });
  },

  getTestErrors(groupId: string): Promise<{ errors: any[]; total: number }> {
    return request({ url: `/test/groups/${groupId}/errors` });
  },

  // ===== 每日打卡 =====
  getTodayCheckin(bankId: string): Promise<{
    bank_id: string; bank_name: string;
    new_words: CheckinWord[]; review_words: CheckinWord[];
  }> {
    return request({ url: "/checkin/today", data: { bankId } });
  },

  markCheckinWord(poolId: string, mark: string): Promise<{
    interval_days: number; next_review_at: string; status: string;
  }> {
    return request({ url: "/checkin/mark", method: "POST", data: { poolId, mark } });
  },

  completeCheckin(bankId: string, counts: { newCount: number; reviewCount: number; knownCount: number; fuzzyCount: number; unknownCount: number }): Promise<any> {
    return request({ url: "/checkin/complete", method: "POST", data: { bankId, ...counts } });
  },

  getCheckinCalendar(year: number, month: number): Promise<CheckinCalendar> {
    return request({ url: "/checkin/calendar", data: { year, month } });
  },

  getCheckinStats(): Promise<CheckinStats> {
    return request({ url: "/checkin/stats" });
  },

  // ===== 词库 =====
  listWordBanks(): Promise<{ banks: WordBank[] }> {
    return request({ url: "/checkin/banks" });
  },

  createWordBank(name: string, description?: string): Promise<{ bank: WordBank }> {
    return request({ url: "/checkin/banks", method: "POST", data: { name, description } });
  },

  getWordBank(bankId: string): Promise<{ bank: WordBank; words: any[] }> {
    return request({ url: `/checkin/banks/${bankId}` });
  },

  deleteWordBank(bankId: string): Promise<any> {
    return request({ url: `/checkin/banks/${bankId}`, method: "DELETE" });
  },

  addWordsToBank(bankId: string, words: string[]): Promise<any> {
    return request({ url: `/checkin/banks/${bankId}/add-words`, method: "POST", data: { words } });
  },

  importGroupToBank(bankId: string, groupId: string): Promise<any> {
    return request({ url: "/checkin/banks/import-from-group", method: "POST", data: { bankId, groupId } });
  },

  wordsToGroup(wordTexts: string[]): Promise<{ group: CardGroupResponse }> {
    return request({ url: "/checkin/words-to-group", method: "POST", data: { wordTexts } });
  },

  // ===== 用户设置 =====
  getSettings(): Promise<{ settings: UserSettings }> {
    return request({ url: "/checkin/settings" });
  },

  updateSettings(settings: Partial<UserSettings>): Promise<{ settings: UserSettings }> {
    return request({ url: "/checkin/settings", method: "PUT", data: settings });
  },

  // ===== v1 deprecated =====
  /** @deprecated v2 — use getTodayCheckin */
  getTodayReview(): Promise<{ cards: ReviewCard[]; total: number }> {
    return request({ url: "/review/today" });
  },

  /** @deprecated v2 — use markCheckinWord */
  submitReview(cardId: string, quality: number): Promise<void> {
    return request<void>({ url: "/review/submit", method: "POST", data: { card_id: cardId, quality } });
  },

  /** @deprecated v2 — use getCheckinStats */
  listNotebook(params?: { status?: string; page?: number; page_size?: number }): Promise<{
    items: NotebookItem[]; page: number; page_size: number;
  }> {
    return request({ url: "/notebook", data: params });
  },

  /** @deprecated v2 */
  removeFromNotebook(id: string): Promise<void> {
    return request<void>({ url: `/notebook/${id}`, method: "DELETE" });
  },
};
