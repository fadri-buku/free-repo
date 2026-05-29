"""Page object for VerifyOtpActivity."""
from appium.webdriver.common.appiumby import AppiumBy
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

from pages.base_page import BasePage, DEFAULT_TIMEOUT


class OtpPage(BasePage):
    """Interactions with VerifyOtpActivity (4-digit OTP entry screen)."""

    # Resource IDs
    TV_OTP_MESSAGE = "tv_otp_message"
    PB_VERIFY_OTP = "pb_verify_otp"
    BTN_RETRY = "btn_retry_request_otp"
    TV_TIMER = "tv_timer"
    TV_ERROR_MESSAGE = "tv_error_message"

    # OTP field IDs (nested inside an otpField compound view)
    OTP_FIELD_ONE = "tvOne"
    OTP_FIELD_TWO = "tvTwo"
    OTP_FIELD_THREE = "tvThree"
    OTP_FIELD_FOUR = "tvFour"

    def is_displayed(self, timeout: int = DEFAULT_TIMEOUT) -> bool:
        """Return True once the OTP instruction message is visible."""
        return self.is_element_visible(self.TV_OTP_MESSAGE, timeout)

    def wait_until_displayed(self, timeout: int = DEFAULT_TIMEOUT) -> None:
        """Block until the OTP screen is visible."""
        self.wait_for_element(self.TV_OTP_MESSAGE, timeout)

    def enter_otp(self, otp: str) -> None:
        """Enter a 4-digit OTP into the individual digit fields.

        The VerifyOtpActivity uses a custom OtpFieldView with four separate
        TextViews. Tapping the first field and sending all 4 digits typically
        fills them in sequence on most OTP view implementations.  If your
        version requires individual field interaction, update this method.

        Args:
            otp: 4-character string of digits, e.g. '1234'.
        """
        if len(otp) != 4 or not otp.isdigit():
            raise ValueError(f"OTP must be exactly 4 digits, got: {otp!r}")

        fields = [
            self.OTP_FIELD_ONE,
            self.OTP_FIELD_TWO,
            self.OTP_FIELD_THREE,
            self.OTP_FIELD_FOUR,
        ]
        for field_id, digit in zip(fields, otp):
            element = self.wait_for_element(field_id)
            element.click()
            element.send_keys(digit)

    def is_timer_visible(self) -> bool:
        return self.is_element_visible(self.TV_TIMER)

    def is_retry_button_visible(self) -> bool:
        return self.is_element_visible(self.BTN_RETRY, timeout=3)

    def get_otp_message_text(self) -> str:
        return self.get_text(self.TV_OTP_MESSAGE)

    def get_error_message_text(self) -> str:
        return self.get_text(self.TV_ERROR_MESSAGE)

    def tap_retry_button(self) -> None:
        self.tap_element(self.BTN_RETRY)
