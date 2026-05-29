"""Appium driver setup and teardown helpers."""
from appium import webdriver
from appium.options import UiAutomator2Options


def create_driver(config: dict) -> webdriver.Remote:
    """Initialise and return an Appium WebDriver instance.

    Args:
        config: Environment config dict produced by config.environments.get_env_config().

    Returns:
        A connected Appium Remote WebDriver.
    """
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.device_name = config["device_name"]
    options.platform_version = config["platform_version"]
    options.app_package = config["app_package"]
    options.app_activity = config["app_activity"]
    options.no_reset = True          # keep app state between runs; set False to reinstall
    options.auto_grant_permissions = True

    driver = webdriver.Remote(config["appium_url"], options=options)
    driver.implicitly_wait(0)        # explicit waits only; keep implicit wait at 0
    return driver


def quit_driver(driver: webdriver.Remote) -> None:
    """Safely quit the Appium driver session."""
    if driver is not None:
        try:
            driver.quit()
        except Exception:  # noqa: BLE001
            pass
