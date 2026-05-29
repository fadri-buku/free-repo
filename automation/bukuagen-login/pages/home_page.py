"""Page object for HomePageActivity."""
from pages.base_page import BasePage, DEFAULT_TIMEOUT


class HomePage(BasePage):
    """Interactions with HomePageActivity (the main dashboard after login)."""

    # Resource IDs — update these to match actual HomePageActivity layout IDs.
    # The IDs below are common BukuWarung/BukuAgen naming conventions; adjust
    # if the source reveals different names.
    TV_BALANCE = "tv_balance"          # account balance widget
    TV_USER_NAME = "tv_user_name"      # logged-in user name label
    BTN_MENU = "btn_menu"             # hamburger / navigation menu
    BOTTOM_NAV = "bottom_navigation"  # bottom navigation bar

    def is_displayed(self, timeout: int = DEFAULT_TIMEOUT) -> bool:
        """Return True when any home-screen landmark element becomes visible.

        We try several candidate IDs so the check is robust against minor
        layout changes. If none match, return False.
        """
        for candidate in (self.BOTTOM_NAV, self.TV_BALANCE, self.BTN_MENU):
            if self.is_element_visible(candidate, timeout=3):
                return True
        return False

    def wait_until_displayed(self, timeout: int = DEFAULT_TIMEOUT) -> None:
        """Block until the home screen is confirmed visible."""
        # Try each landmark in sequence; raise on overall timeout
        import time
        deadline = time.time() + timeout
        while time.time() < deadline:
            if self.is_displayed(timeout=2):
                return
        raise TimeoutError("HomePageActivity did not appear within the timeout.")
