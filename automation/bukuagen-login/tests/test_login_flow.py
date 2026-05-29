"""BukuAgen login flow test cases.

Run all tests against staging (default)::

    pytest tests/test_login_flow.py --env staging

Run against dev::

    pytest tests/test_login_flow.py --env dev
"""
import os

import pytest

from pages.login_page import LoginPage
from pages.otp_page import OtpPage
from pages.home_page import HomePage
from utils.otp_helper import get_otp

# ---------------------------------------------------------------------------
# Shared helper
# ---------------------------------------------------------------------------

VALID_PHONE = os.environ.get("TEST_PHONE_NUMBER", "81234567890")


def _login_page(driver, app_package: str) -> LoginPage:
    page = LoginPage(driver, app_package)
    page.navigate()
    return page


# ---------------------------------------------------------------------------
# Test cases
# ---------------------------------------------------------------------------

class TestLoginScreenUI:
    """Verify that the login screen renders all expected UI elements."""

    def test_login_screen_renders_correctly(self, driver, app_package):
        """Banner image, logo, country-code label, and (disabled) login button
        must all be present on first load.
        """
        page = _login_page(driver, app_package)

        assert page.is_banner_visible(), "Login banner image should be visible"
        assert page.is_logo_visible(), "Login banner logo should be visible"

        country_code = page.get_country_code_text()
        assert "+62" in country_code, f"Country code should show '+62', got: {country_code!r}"

        assert not page.is_login_button_enabled(), (
            "Login button should be DISABLED before a phone number is entered"
        )

    def test_login_button_enables_after_valid_phone(self, driver, app_package):
        """Entering a valid Indonesian mobile number should enable the login button."""
        page = _login_page(driver, app_package)
        page.enter_phone_number(VALID_PHONE)

        assert page.is_login_button_enabled(), (
            f"Login button should be ENABLED after entering phone '{VALID_PHONE}'"
        )

    def test_login_button_disabled_for_short_phone(self, driver, app_package):
        """Entering an obviously too-short number should keep the button disabled."""
        page = _login_page(driver, app_package)
        page.enter_phone_number("123")

        assert not page.is_login_button_enabled(), (
            "Login button should remain DISABLED for a 3-digit phone number"
        )


class TestLoginNavigation:
    """Verify navigation between login screens."""

    def test_login_navigates_to_otp_screen(self, driver, app_package, config):
        """Entering a valid phone and tapping login should bring up the OTP screen."""
        page = _login_page(driver, app_package)

        phone = config.get("phone_number") or VALID_PHONE
        page.enter_phone_number(phone)
        page.tap_login_button()

        otp_page = OtpPage(driver, app_package)
        assert otp_page.is_displayed(timeout=20), (
            "OTP screen (VerifyOtpActivity) should appear after submitting a valid phone number"
        )

    def test_otp_screen_renders_correctly(self, driver, app_package, config):
        """OTP screen must show the instruction message and countdown timer;
        the retry button should be hidden until the timer expires.
        """
        # Navigate to OTP screen first
        page = _login_page(driver, app_package)
        phone = config.get("phone_number") or VALID_PHONE
        page.enter_phone_number(phone)
        page.tap_login_button()

        otp_page = OtpPage(driver, app_package)
        otp_page.wait_until_displayed(timeout=20)

        assert otp_page.is_element_visible(otp_page.TV_OTP_MESSAGE), (
            "OTP instruction message should be visible"
        )
        assert otp_page.is_timer_visible(), (
            "Countdown timer should be visible on the OTP screen"
        )
        assert not otp_page.is_retry_button_visible(), (
            "Retry OTP button should be hidden while the timer is still running"
        )
        # Verify individual digit fields are present
        for field_id in (
            otp_page.OTP_FIELD_ONE,
            otp_page.OTP_FIELD_TWO,
            otp_page.OTP_FIELD_THREE,
            otp_page.OTP_FIELD_FOUR,
        ):
            assert otp_page.is_element_visible(field_id), (
                f"OTP digit field '{field_id}' should be visible"
            )


@pytest.mark.skipif(
    not os.environ.get("TEST_OTP"),
    reason=(
        "Skipping full login flow: TEST_OTP env var is not set. "
        "Set TEST_OTP to the 4-digit OTP and re-run to execute this test."
    ),
)
class TestFullLoginFlow:
    """End-to-end login flow — requires TEST_OTP to be set."""

    def test_full_login_flow(self, driver, app_package, config):
        """Enter phone, submit, enter OTP, verify home screen is reached."""
        phone = config.get("phone_number")
        if not phone:
            pytest.skip(
                "TEST_PHONE_NUMBER env var is not set; skipping full login flow."
            )

        # Step 1 — login screen
        login_page = _login_page(driver, app_package)
        login_page.enter_phone_number(phone)
        login_page.tap_login_button()

        # Step 2 — OTP screen
        otp_page = OtpPage(driver, app_package)
        otp_page.wait_until_displayed(timeout=20)

        otp_code = get_otp(phone)
        otp_page.enter_otp(otp_code)

        # Step 3 — home screen
        home_page = HomePage(driver, app_package)
        home_page.wait_until_displayed(timeout=30)

        assert home_page.is_displayed(), (
            "HomePageActivity should be displayed after successful OTP verification"
        )
