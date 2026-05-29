"""Base page object — shared helpers for all page classes."""
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException

DEFAULT_TIMEOUT = 15  # seconds


class BasePage:
    """Base class for all Page Object classes."""

    def __init__(self, driver, app_package: str):
        self.driver = driver
        self.app_package = app_package

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _id(self, resource_id: str) -> tuple:
        """Build a full (By, value) locator tuple from a bare resource-id name."""
        full_id = f"{self.app_package}:id/{resource_id}"
        return (AppiumBy.ID, full_id)

    def _wait(self, timeout: int = DEFAULT_TIMEOUT) -> WebDriverWait:
        return WebDriverWait(self.driver, timeout)

    # ------------------------------------------------------------------
    # Public helpers
    # ------------------------------------------------------------------

    def wait_for_element(self, resource_id: str, timeout: int = DEFAULT_TIMEOUT):
        """Wait until element is visible and return it."""
        return self._wait(timeout).until(
            EC.visibility_of_element_located(self._id(resource_id))
        )

    def wait_for_element_present(self, resource_id: str, timeout: int = DEFAULT_TIMEOUT):
        """Wait until element is present in DOM (not necessarily visible)."""
        return self._wait(timeout).until(
            EC.presence_of_element_located(self._id(resource_id))
        )

    def is_element_visible(self, resource_id: str, timeout: int = 5) -> bool:
        """Return True if element becomes visible within *timeout* seconds."""
        try:
            self._wait(timeout).until(
                EC.visibility_of_element_located(self._id(resource_id))
            )
            return True
        except TimeoutException:
            return False

    def find_element(self, resource_id: str):
        """Find element without waiting (use after an explicit wait)."""
        return self.driver.find_element(*self._id(resource_id))

    def tap_element(self, resource_id: str, timeout: int = DEFAULT_TIMEOUT):
        """Wait for element then tap it."""
        element = self.wait_for_element(resource_id, timeout)
        element.click()
        return element

    def send_keys_to(self, resource_id: str, text: str, timeout: int = DEFAULT_TIMEOUT):
        """Wait for element then clear & send keys."""
        element = self.wait_for_element(resource_id, timeout)
        element.clear()
        element.send_keys(text)
        return element

    def get_text(self, resource_id: str, timeout: int = DEFAULT_TIMEOUT) -> str:
        """Return the visible text of an element."""
        return self.wait_for_element(resource_id, timeout).text

    def is_element_enabled(self, resource_id: str, timeout: int = DEFAULT_TIMEOUT) -> bool:
        """Return True if the element is enabled."""
        return self.wait_for_element_present(resource_id, timeout).is_enabled()
