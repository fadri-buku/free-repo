const WORK_ALARM  = "worklog-work-end";
const BREAK_ALARM = "worklog-break-end";
const BADGE_ALARM = "worklog-badge-tick";
const STATE_KEY   = "worklogState";
const CYCLES_KEY  = "worklogCycles";

// ── Persistence ───────────────────────────────────────────────────────────

async function getState() {
  const { [STATE_KEY]: s } = await chrome.storage.local.get(STATE_KEY);
  return s || {};
}

async function setState(patch) {
  const next = { ...(await getState()), ...patch };
  await chrome.storage.local.set({ [STATE_KEY]: next });
  return next;
}

async function getCycles() {
  const { [CYCLES_KEY]: n } = await chrome.storage.local.get(CYCLES_KEY);
  return typeof n === "number" ? n : 0;
}

async function incrementCycles() {
  const n = (await getCycles()) + 1;
  await chrome.storage.local.set({ [CYCLES_KEY]: n });
  return n;
}

// ── Badge ─────────────────────────────────────────────────────────────────

function updateBadge(state) {
  if (!state?.phase) {
    chrome.action.setBadgeText({ text: "" });
    return;
  }
  const ms = state.paused
    ? (state.remainingMs || 0)
    : Math.max(0, (state.endTime || 0) - Date.now());
  const mins = Math.ceil(ms / 60000);
  chrome.action.setBadgeText({ text: String(mins > 0 ? mins : 0) });
  chrome.action.setBadgeBackgroundColor({
    color: state.phase === "work" ? [210, 80, 60, 255] : [46, 160, 67, 255]
  });
}

// ── Alarm helpers ─────────────────────────────────────────────────────────

async function clearAll() {
  await Promise.all([
    chrome.alarms.clear(WORK_ALARM),
    chrome.alarms.clear(BREAK_ALARM),
    chrome.alarms.clear(BADGE_ALARM)
  ]);
}

async function armBreak(cycles, silent = false) {
  const isLong    = cycles % 4 === 0;
  const minutes   = isLong ? 15 : 5;
  const endTime   = Date.now() + minutes * 60 * 1000;
  await chrome.alarms.create(BREAK_ALARM, { when: endTime });
  await chrome.alarms.create(BADGE_ALARM, { periodInMinutes: 1 });
  const next = await setState({
    running: true, paused: false,
    phase: isLong ? "long-break" : "break",
    endTime, minutes, title: "",
    remainingMs: null, awaitingAck: false, lockedTabId: null,
    silent
  });
  updateBadge(next);
  if (!silent) {
    try {
      await chrome.notifications.create("worklog-break-start", {
        type: "basic",
        iconUrl: chrome.runtime.getURL("icons/icon128.png"),
        title: isLong ? "Long break — 15 min" : "Short break — 5 min",
        message: "Step away, stretch. Break timer is running.",
        priority: 1
      });
    } catch {}
  }
}

async function openLockedTab() {
  const url = chrome.runtime.getURL("locked/locked.html");
  const tab = await chrome.tabs.create({ url, active: true });
  await setState({ lockedTabId: tab.id });
  if (tab.windowId != null) {
    try { await chrome.windows.update(tab.windowId, { focused: true }); } catch {}
  }
}

// ── Message handler ───────────────────────────────────────────────────────

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  (async () => {
    try {
      switch (msg?.type) {

        case "START_TIMER": {
          const minutes = Number(msg.minutes);
          if (!Number.isFinite(minutes) || minutes <= 0) {
            sendResponse({ ok: false, error: "Duration must be a positive number." }); return;
          }
          await clearAll();
          const endTime = Date.now() + minutes * 60 * 1000;
          const title   = typeof msg.title === "string" ? msg.title.trim() : "";
          const silent  = !!msg.silent;
          await chrome.alarms.create(WORK_ALARM, { when: endTime });
          await chrome.alarms.create(BADGE_ALARM, { periodInMinutes: 1 });
          const next = await setState({
            running: true, paused: false, phase: "work",
            endTime, minutes, title, remainingMs: null,
            awaitingAck: false, lockedTabId: null,
            silent
          });
          updateBadge(next);
          sendResponse({ ok: true, endTime });
          break;
        }

        case "PAUSE_TIMER": {
          const state = await getState();
          if (!state.running || state.paused) { sendResponse({ ok: false }); return; }
          await chrome.alarms.clear(state.phase === "work" ? WORK_ALARM : BREAK_ALARM);
          await chrome.alarms.clear(BADGE_ALARM);
          const remainingMs = Math.max(0, state.endTime - Date.now());
          const next = await setState({ running: false, paused: true, remainingMs });
          updateBadge(next);
          sendResponse({ ok: true });
          break;
        }

        case "RESUME_TIMER": {
          const state = await getState();
          if (!state.paused) { sendResponse({ ok: false }); return; }
          const endTime  = Date.now() + state.remainingMs;
          const alarm    = state.phase === "work" ? WORK_ALARM : BREAK_ALARM;
          await chrome.alarms.create(alarm, { when: endTime });
          await chrome.alarms.create(BADGE_ALARM, { periodInMinutes: 1 });
          const next = await setState({ running: true, paused: false, endTime, remainingMs: null });
          updateBadge(next);
          sendResponse({ ok: true });
          break;
        }

        case "SKIP_BREAK":
        case "CANCEL_TIMER": {
          await clearAll();
          const next = await setState({
            running: false, paused: false, phase: null,
            endTime: null, remainingMs: null, awaitingAck: false,
            silent: false
          });
          updateBadge(next);
          sendResponse({ ok: true });
          break;
        }

        case "GET_STATE": {
          const state  = await getState();
          const cycles = await getCycles();
          sendResponse({ ...state, cycles });
          break;
        }

        case "ACKNOWLEDGED": {
          const prev = await getState();
          const silent = !!prev.silent;
          await clearAll();
          const cycles = await incrementCycles();
          const next   = await setState({
            running: false, paused: false, phase: null,
            endTime: null, remainingMs: null, awaitingAck: false, lockedTabId: null
          });
          updateBadge(next);
          await armBreak(cycles, silent);
          sendResponse({ ok: true, cycles });
          break;
        }

        default:
          sendResponse({ ok: false, error: "Unknown message" });
      }
    } catch (err) {
      sendResponse({ ok: false, error: String(err?.message || err) });
    }
  })();
  return true;
});

// ── Alarm events ──────────────────────────────────────────────────────────

chrome.alarms.onAlarm.addListener(async (alarm) => {
  if (alarm.name === WORK_ALARM) {
    await clearAll();
    const next = await setState({ running: false, paused: false, awaitingAck: true });
    updateBadge(next);
    if (!next.silent) {
      try {
        await chrome.notifications.create("worklog-end", {
          type: "basic",
          iconUrl: chrome.runtime.getURL("icons/icon128.png"),
          title: "Work session finished",
          message: "Time to log what you worked on.",
          priority: 2,
          requireInteraction: true
        });
      } catch {}
    }
    await openLockedTab();

  } else if (alarm.name === BREAK_ALARM) {
    const prev = await getState();
    await clearAll();
    const next = await setState({ running: false, paused: false, phase: null });
    updateBadge(next);
    if (!prev.silent) {
      try {
        await chrome.notifications.create("worklog-break-end", {
          type: "basic",
          iconUrl: chrome.runtime.getURL("icons/icon128.png"),
          title: "Break's over!",
          message: "Ready for your next focus session?",
          priority: 2,
          requireInteraction: true
        });
      } catch {}
    }

  } else if (alarm.name === BADGE_ALARM) {
    updateBadge(await getState());
  }
});

// ── Notification clicks ───────────────────────────────────────────────────

chrome.notifications.onClicked.addListener(async (id) => {
  chrome.notifications.clear(id);
  if (id === "worklog-end") {
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
  }
});

// ── Re-open locked tab if closed before acknowledge ───────────────────────

chrome.tabs.onRemoved.addListener(async (tabId) => {
  const state = await getState();
  if (state.awaitingAck && state.lockedTabId === tabId) {
    await openLockedTab();
  }
});
