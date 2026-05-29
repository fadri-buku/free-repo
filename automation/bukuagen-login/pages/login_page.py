"""Page object for LoginActivity."""
from pages.base_page import BasePage
from pages.welcome_page import WelcomePage


class LoginPage(BasePage):
    """Interactions with LoginActivity (phone-number entry screen)."""

    # Resource IDs
    IV_BANNER = "iv_login_banner"
    IV_BANNER_LOGO = "iv_login_banner_logo"
    TV_TITLE = "tv_login_title"
    TV_SUBTITLE = "tv_login_subtitle"
    TV_COUNTRY_CODE = "tv_country_code"
    ET_PHONE = "et_login_phone"
    BTN_LOGIN = "btn_login"
    PB_LOGIN = "pb_login"
    TV_WARNING = "tv_warning"

    def navigate(self) -> None:
        """Ensure we are on LoginActivity.

        If WelcomeActivity is shown first, tap its login button to proceed.
        """
        welcome = WelcomePage(self.driver, self.app_package)
        if welcome.is_displayed(timeout=5):
            welcome.tap_login()
        # Wait for the phone-number input to confirm we are on LoginActivity
        self.wait_for_element(self.ET_PHONE)

    def enter_phone_number(self, phone: str) -> None:
        """Clear the phone field and type *phone*."""
        self.send_keys_to(self.ET_PHONE, phone)

    def tap_login_button(self) -> None:
        """Tap the login CTA button."""
        self.tap_element(self.BTN_LOGIN)

    def is_login_button_enabled(self) -> bool:
        """Return True if the login button is enabled."""
        return self.is_element_enabled(self.BTN_LOGIN)

    def is_banner_visible(self) -> bool:
        return self.is_element_visible(self.IV_BANNER)

    def is_logo_visible(self) -> bool:
        return self.is_element_visible(self.IV_BANNER_LOGO)

    def get_country_code_text(self) -> str:
        return self.get_text(self.TV_COUNTRY_CODE)

    def get_warning_text(self) -> str:
        return self.get_text(self.TV_WARNING)
