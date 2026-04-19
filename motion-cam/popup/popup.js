const DEFAULT_SENSITIVITY = 40;

const slider = document.getElementById("sensitivity");
const sliderVal = document.getElementById("sensitivity-val");
const openBtn = document.getElementById("open");
const clearBtn = document.getElementById("clear");
const eventCount = document.getElementById("event-count");

async function load() {
  const { motionCamSensitivity, motionCamEvents } =
    await chrome.storage.local.get(["motionCamSensitivity", "motionCamEvents"]);
  const sens = motionCamSensitivity ?? DEFAULT_SENSITIVITY;
  slider.value = String(sens);
  sliderVal.textContent = String(sens);
  eventCount.textContent = String((motionCamEvents ?? []).length);
}

slider.addEventListener("input", () => {
  sliderVal.textContent = slider.value;
});

slider.addEventListener("change", async () => {
  await chrome.storage.local.set({
    motionCamSensitivity: Number(slider.value),
  });
});

openBtn.addEventListener("click", async () => {
  const url = chrome.runtime.getURL("tracker/tracker.html");
  await chrome.tabs.create({ url });
  window.close();
});

clearBtn.addEventListener("click", async () => {
  await chrome.storage.local.set({ motionCamEvents: [] });
  eventCount.textContent = "0";
});

chrome.storage.onChanged.addListener((changes, area) => {
  if (area !== "local") return;
  if (changes.motionCamEvents) {
    eventCount.textContent = String(
      (changes.motionCamEvents.newValue ?? []).length,
    );
  }
});

load();
