import { ref, nextTick } from "vue";
import { defineStore } from "pinia";

export const useBlogChatStore = defineStore("blogChat", () => {
  const input = ref("");
  const messages = ref<{ role: string; content: string }[]>([]);
  const loading = ref(false);
  const chatId = ref(Date.now().toString(36) + Math.random().toString(36).slice(2, 8));
  let chatMsgsRef: HTMLElement | null = null;

  function setScrollRef(el: HTMLElement | null) {
    chatMsgsRef = el;
  }

  function scroll() {
    nextTick(() => {
      if (chatMsgsRef) chatMsgsRef.scrollTop = chatMsgsRef.scrollHeight;
    });
  }

  async function send() {
    const msg = input.value.trim();
    if (!msg || loading.value) return;
    input.value = "";
    messages.value.push({ role: "user", content: msg });
    messages.value.push({ role: "assistant", content: "" });
    scroll();
    loading.value = true;
    try {
      const resp = await fetch("/api/v1/public/chat/stream", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: msg, model: "deepseek", chat_id: chatId.value }),
      });
      const reader = resp.body?.getReader();
      const decoder = new TextDecoder();
      let buffer = "";
      let isFirstLine = true;
      const lastMsg = messages.value[messages.value.length - 1];
      while (reader) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() || "";
        for (const line of lines) {
          if (line.startsWith("data:")) {
            const data = line.slice(5).replace(/^ /, "");
            if (data === "[DONE]") continue;
            if (!isFirstLine) lastMsg.content += "\n";
            lastMsg.content += data;
            isFirstLine = false;
          } else if (line === "") {
            isFirstLine = true;
          }
        }
        scroll();
      }
    } catch {
      messages.value[messages.value.length - 1].content = "请求失败，请重试";
    }
    loading.value = false;
  }

  return { input, messages, loading, chatId, send, setScrollRef };
});
