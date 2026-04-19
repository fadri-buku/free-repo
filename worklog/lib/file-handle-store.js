const DB_NAME = "worklog";
const DB_VERSION = 1;
const STORE = "handles";
const KEY = "worklogFile";

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

export async function ensurePermission(handle, mode = "readwrite") {
  if (!handle) return false;
  const opts = { mode };
  if ((await handle.queryPermission(opts)) === "granted") return true;
  return (await handle.requestPermission(opts)) === "granted";
}

export async function appendToFile(handle, text) {
  const existing = await handle.getFile();
  const prior = await existing.text();
  const sep = prior && !prior.endsWith("\n") ? "\n" : "";
  const writable = await handle.createWritable({ keepExistingData: false });
  await writable.write(prior + sep + text);
  await writable.close();
}
