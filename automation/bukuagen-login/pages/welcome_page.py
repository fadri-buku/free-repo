"""Page object for WelcomeActivity.

BukuAgen shows a WelcomeActivity before routing the user to LoginActivity.
If the app skips this screen (e.g. user data already present), the tests still
work because LoginPage.navigate() waits directly for the phone-number field.
"""
from pages.base_page import BasePage


class WelcomePage(BasePage):
    """Interactions with WelcomeActivity."""

    # Resource IDs (bare names — base_page._id() prepends the package prefix)
    BTN_LOGIN = "btn_login"        # "Masuk" / login CTA on the welcome screen
    BTN_REGISTER = "btn_register"  # registration CTA (not used in login tests)

    def tap_login(self) -> None:
        """Tap the login / masuk button on the welcome screen."""
        self.tap_element(self.BTN_LOGIN)

    def is_displayed(self, timeout: int = 5) -> bool:
        """Return True if the welcome screen is currently shown."""
        return self.is_element_visible(self.BTN_LOGIN, timeout)
