/**
 * 微信实时日志管理器。
 * 在微信小程序管理后台 → 开发管理 → 运维中心 → 实时日志 查看。
 * 非微信环境自动降级为 console.log。
 *
 * 使用示例：
 *   logger.setScene("login");          // 设置搜索标签（覆盖前一个）
 *   logger.info("LOGIN", "登录成功", { userId: "xxx" });
 *   logger.flush();                    // 强制上传（页面跳转前调用）
 */

type LogLevel = "info" | "warn" | "error" | "debug";

let realtimeLogManager: any = null;
let inited = false;
const registeredTags = new Set<string>();

function ensureInit() {
  if (inited) return;
  inited = true;
  try {
    realtimeLogManager = wx.getRealtimeLogManager ? wx.getRealtimeLogManager() : null;
  } catch {
    // 非微信环境忽略
  }
}

/** 注册筛选标签（最多 5 个不同的 tag，多余的静默忽略） */
function ensureTag(tag: string) {
  if (!realtimeLogManager || registeredTags.has(tag)) return;
  if (registeredTags.size >= 5) {
    console.warn("[LOGGER] 筛选标签已达上限（5个），无法添加:", tag);
    return;
  }
  registeredTags.add(tag);
  realtimeLogManager.addFilterMsg(tag);
}

function log(level: LogLevel, tag: string, msg: string, data?: any) {
  ensureInit();
  const payload = data ? `${msg} ${JSON.stringify(data)}` : msg;
  const line = `[${tag}] ${payload}`;

  // 微信实时日志
  if (realtimeLogManager) {
    ensureTag(tag);
    try {
      switch (level) {
        case "info":  realtimeLogManager.info(line);  break;
        case "warn":  realtimeLogManager.warn(line);  break;
        case "error": realtimeLogManager.error(line); break;
        case "debug": realtimeLogManager.debug(line); break;
      }
    } catch (e) {
      console.warn("[LOGGER] 实时日志写入失败，降级到 console", e);
    }
  }

  // 同时输出到 vConsole / DevTools
  switch (level) {
    case "info":  console.log(line,  data || ""); break;
    case "warn":  console.warn(line, data || ""); break;
    case "error": console.error(line, data || ""); break;
    case "debug": console.debug(line, data || ""); break;
  }
}

export const logger = {
  /** 设置当前搜索场景（管理后台可根据此值筛选） */
  setScene(scene: string) {
    ensureInit();
    if (realtimeLogManager) {
      try { realtimeLogManager.setFilterMsg(scene); } catch {}
    }
  },

  /** 强制上传日志缓冲（页面跳转 / 操作完成前调用，避免日志丢失） */
  flush() {
    ensureInit();
    if (realtimeLogManager && realtimeLogManager.flush) {
      try { realtimeLogManager.flush(); } catch {}
    }
  },

  info(tag: string, msg: string, data?: any) { log("info", tag, msg, data); },
  warn(tag: string, msg: string, data?: any) { log("warn", tag, msg, data); },
  error(tag: string, msg: string, data?: any) { log("error", tag, msg, data); },
  debug(tag: string, msg: string, data?: any) { log("debug", tag, msg, data); },
};
