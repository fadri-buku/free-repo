"""OTP retrieval helper.

This module provides a single entry-point `get_otp()` that the test suite calls
whenever it needs the most-recently delivered OTP for the test phone number.

Current implementation: reads from the ``TEST_OTP`` environment variable.

To hook in a real SMS gateway / API:
    1. Delete (or guard) the env-var path below.
    2. Call your SMS API (e.g. Twilio Verify, Firebase, or your own backend
       endpoint) and parse the 4-digit code from the response.
    3. Add retry logic / polling so the code is collected as soon as it arrives
       (typically within 30 seconds).

Example skeleton for a custom backend endpoint::

    import requests, time

    def get_otp(phone_number: str, timeout: int = 60) -> str:
        deadline = time.time() + timeout
        while time.time() < deadline:
            resp = requests.get(
                "https://your-backend/api/test/latest-otp",
                params={"phone": phone_number},
                headers={"Authorization": "Bearer " + os.environ["OTP_API_TOKEN"]},
                timeout=10,
            )
            if resp.ok and resp.json().get("otp"):
                return resp.json()["otp"]
            time.sleep(3)
        raise TimeoutError(f"OTP not received within {timeout}s for {phone_number}")
"""
import os


def get_otp(phone_number: str = "") -> str:  # noqa: ARG001
    """Return the OTP for the given phone number.

    Args:
        phone_number: The test phone number (E.164 or local format). Currently
            unused — all envs read from ``TEST_OTP``. Pass it through when you
            implement a real SMS-gateway lookup.

    Returns:
        The 4-digit OTP string.

    Raises:
        RuntimeError: If ``TEST_OTP`` is not set and no other source is configured.
    """
    otp = os.environ.get("TEST_OTP", "")
    if not otp:
        raise RuntimeError(
            "TEST_OTP environment variable is not set. "
            "Set it to the 4-digit OTP, or implement a real SMS lookup in "
            "utils/otp_helper.py."
        )
    return otp
