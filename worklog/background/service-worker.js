const ALARM_NAME = "worklog-timer-end";
const STATE_KEY = "worklogState";

async function getState() {
  const { [STATE_KEY]: state } = await chrome.storage.local.get(STATE_KEY);
  return state || {};
}

async function setState(patch) {
  const current = await getState();
  const next = { ...current, ...patch };
  await chrome.storage.local.set({ [STATE_KEY]: next });
  return next;
}

async function openLockedTab() {
  const url = chrome.runtime.getURL("locked/locked.html");
  const tab = await chrome.tabs.create({ url, active: true });
  await setState({ lockedTabId: tab.id });
  if (tab.windowId != null) {
    try { await chrome.windows.update(tab.windowId, { focused: true }); } catch {}
  }
  return tab;
}

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  (async () => {
    try {
      if (msg?.type === "START_TIMER") {
        const minutes = Number(msg.minutes);
        if (!Number.isFinite(minutes) || minutes <= 0) {
          sendResponse({ ok: false, error: "Duration must be a positive number." });
          return;
        }
        const endTime = Date.now() + minutes * 60 * 1000;
        const title = typeof msg.title === "string" ? msg.title.trim() : "";
        await chrome.alarms.clear(ALARM_NAME);
        await chrome.alarms.create(ALARM_NAME, { when: endTime });
        await setState({ running: true, endTime, minutes, title, awaitingAck: false });
        sendResponse({ ok: true, endTime });
      } else if (msg?.type === "CANCEL_TIMER") {
        await chrome.alarms.clear(ALARM_NAME);
        await setState({ running: false, endTime: null, awaitingAck: false });
        sendResponse({ ok: true });
      } else if (msg?.type === "GET_STATE") {
        sendResponse(await getState());
      } else if (msg?.type === "ACKNOWLEDGED") {
        await setState({ running: false, endTime: null, awaitingAck: false, lockedTabId: null });
        sendResponse({ ok: true });
      } else {
        sendResponse({ ok: false, error: "Unknown message" });
      }
    } catch (err) {
      sendResponse({ ok: false, error: String(err?.message || err) });
    }
  })();
  return true;
});

chrome.alarms.onAlarm.addListener(async (alarm) => {
  if (alarm.name !== ALARM_NAME) return;
  await setState({ running: false, awaitingAck: true });
  try {
    await chrome.notifications.create("worklog-end", {
      type: "basic",
      iconUrl: chrome.runtime.getURL("icons/icon128.png"),
      title: "Worklog timer finished",
      message: "Time's up. Click to record what you worked on.",
      priority: 2,
      requireInteraction: true
    });
  } catch {}
  await openLockedTab();
});

chrome.notifications.onClicked.addListener(async (id) => {
  if (id !== "worklog-end") return;
  chrome.notifications.clear(id);
  const { lockedTabId } = await getState();
  if (lockedTabId != null) {
    try {
      const tab = await chrome.tabs.get(lockedTabId);
      await chrome.tabs.update(tab.id, { active: true });
      if (tab.windowId != null) {
        try { await chrome.windows.update(tab.windowId, { focused: true }); } catch {}
      }
      return;
    } catch {}
  }
  await openLockedTab();
});

chrome.tabs.onRemoved.addListener(async (tabId) => {
  const state = await getState();
  if (state.awaitingAck && state.lockedTabId === tabId) {
    await openLockedTab();
  }
});
