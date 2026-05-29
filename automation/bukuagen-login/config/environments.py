import os

ENVIRONMENTS = {
    "dev": {
        "app_package": "com.bukuwarung.bukuagen.dev",
        "app_activity": "com.bukuwarung.edc.SplashActivity",
        "appium_url": os.environ.get("APPIUM_URL", "http://localhost:4723"),
        "device_name": os.environ.get("DEVICE_NAME", "emulator-5554"),
        "platform_version": os.environ.get("PLATFORM_VERSION", "12"),
        "phone_number": os.environ.get("TEST_PHONE_NUMBER", ""),
        # In dev, OTP can be supplied via env var (e.g. a fixed test OTP from the backend).
        # In staging/prod, leave TEST_OTP unset and retrieve the code from a real SMS.
        "test_otp": os.environ.get("TEST_OTP", ""),
    },
    "staging": {
        "app_package": "com.bukuwarung.bukuagen.staging",
        "app_activity": "com.bukuwarung.edc.SplashActivity",
        "appium_url": os.environ.get("APPIUM_URL", "http://localhost:4723"),
        "device_name": os.environ.get("DEVICE_NAME", "emulator-5554"),
        "platform_version": os.environ.get("PLATFORM_VERSION", "12"),
        "phone_number": os.environ.get("TEST_PHONE_NUMBER", ""),
        # Staging uses real SMS; TEST_OTP should be retrieved via your SMS-gateway/API.
        "test_otp": os.environ.get("TEST_OTP", ""),
    },
}


def get_env_config(env_name: str) -> dict:
    """Return configuration dict for the requested environment.

    Args:
        env_name: One of 'dev' or 'staging'.

    Raises:
        ValueError: If env_name is not recognised.
    """
    if env_name not in ENVIRONMENTS:
        raise ValueError(
            f"Unknown environment '{env_name}'. Valid options: {list(ENVIRONMENTS.keys())}"
        )
    return ENVIRONMENTS[env_name]
