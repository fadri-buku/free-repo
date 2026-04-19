// Persists the last-opened markdown file handle across sessions. Mirrors the
// shape of worklog/lib/file-handle-store.js — same IndexedDB pattern, just
// scoped to this extension and "read" permission only.

const DB_NAME = "markdown-preview";
const DB_VERSION = 1;
const STORE = "handles";
const KEY = "lastFile";

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => req.result.createObjectStore(STORE);
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

function request(store, action) {
  return new Promise((resolve, reject) => {
    const req = action(store);
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

export async function saveFileHandle(handle) {
  const db = await openDb();
  const t = db.transaction(STORE, "readwrite");
  await request(t.objectStore(STORE), (s) => s.put(handle, KEY));
}

export async function loadFileHandle() {
  const db = await openDb();
  const t = db.transaction(STORE, "readonly");
  return request(t.objectStore(STORE), (s) => s.get(KEY));
}

export async function clearFileHandle() {
  const db = await openDb();
  const t = db.transaction(STORE, "readwrite");
  await request(t.objectStore(STORE), (s) => s.delete(KEY));
}

export async function ensurePermission(handle, mode = "read") {
  if (!handle) return false;
  const opts = { mode };
  if ((await handle.queryPermission(opts)) === "granted") return true;
  return (await handle.requestPermission(opts)) === "granted";
}

export async function readFile(handle) {
  const file = await handle.getFile();
  return file.text();
}
