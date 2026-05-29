# BukuAgen Login — Appium Python Automation

End-to-end mobile UI tests for the BukuAgen Android app login flow, built with
[Appium 2](https://appium.io/) + [pytest](https://pytest.org/).

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Python | 3.9+ | |
| Appium | 2.x | `npm install -g appium` |
| UiAutomator2 driver | latest | `appium driver install uiautomator2` |
| Android SDK / platform-tools | latest | `adb` must be on your PATH |
| AVD **or** real device | Android 9+ | connected and recognised by `adb devices` |
| BukuAgen APK | staging / dev build | installed on device before running |

---

## Installation

```bash
cd automation/bukuagen-login
python -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

---

## Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `TEST_PHONE_NUMBER` | Yes (for nav/full-flow tests) | Test phone number (local format, e.g. `81234567890`) |
| `TEST_OTP` | Yes (for full-flow test only) | 4-digit OTP. For **dev**, a fixed backend code; for **staging**, retrieve from SMS. See `utils/otp_helper.py` for hook details. |
| `DEVICE_NAME` | No | AVD name or device serial (default `emulator-5554`) |
| `PLATFORM_VERSION` | No | Android version (default `12`) |
| `APPIUM_URL` | No | Appium server URL (default `http://localhost:4723`) |

---

## Running the tests

### Start Appium first

```bash
appium
```

### Run against **staging** (default)

```bash
export TEST_PHONE_NUMBER=81234567890
export TEST_OTP=1234          # only needed for test_full_login_flow

pytest tests/ --env staging
```

### Run against **dev**

```bash
export TEST_PHONE_NUMBER=81234567890
export TEST_OTP=1234

pytest tests/ --env dev
```

### Run a single test

```bash
pytest tests/test_login_flow.py::TestLoginScreenUI::test_login_screen_renders_correctly --env staging
```

### HTML report

After a run, open `report.html` in your browser. It is generated automatically
via `pytest-html` (configured in `pytest.ini`).

---

## Project structure

```
automation/bukuagen-login/
├── README.md
├── requirements.txt
├── pytest.ini
├── conftest.py               # driver fixture + --env CLI option
├── config/
│   └── environments.py       # per-env capability config
├── pages/
│   ├── base_page.py          # shared Appium helpers, explicit waits
│   ├── welcome_page.py       # WelcomeActivity
│   ├── login_page.py         # LoginActivity
│   ├── otp_page.py           # VerifyOtpActivity
│   └── home_page.py          # HomePageActivity
├── tests/
│   └── test_login_flow.py    # all test cases
└── utils/
    ├── appium_helper.py       # driver setup / teardown
    └── otp_helper.py         # OTP retrieval stub (hook your SMS API here)
```

---

## App package names

| Environment | Package |
|-------------|---------|
| `dev` | `com.bukuwarung.bukuagen.dev` |
| `staging` | `com.bukuwarung.bukuagen.staging` |
| prod | `com.bukuwarung.bukuagen` |

The entry activity for all variants is `com.bukuwarung.edc.SplashActivity`.

---

## Plugging in a real SMS gateway (for staging / prod)

Open `utils/otp_helper.py` and follow the instructions in the module docstring.
The `get_otp()` function is the single hook point — replace the `TEST_OTP` env-var
read with a call to your SMS API (Twilio, Firebase, or your own backend endpoint).
