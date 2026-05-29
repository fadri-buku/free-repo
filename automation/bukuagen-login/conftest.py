"""pytest configuration and fixtures for BukuAgen Appium test suite."""
import pytest

from config.environments import get_env_config
from utils.appium_helper import create_driver, quit_driver


# ---------------------------------------------------------------------------
# CLI options
# ---------------------------------------------------------------------------

def pytest_addoption(parser: pytest.Parser) -> None:
    """Register custom CLI options."""
    parser.addoption(
        "--env",
        action="store",
        default="staging",
        choices=["dev", "staging"],
        help="Target environment: 'dev' or 'staging' (default: staging)",
    )


# ---------------------------------------------------------------------------
# Session-scoped fixtures
# ---------------------------------------------------------------------------

@pytest.fixture(scope="session")
def env(request: pytest.FixtureRequest) -> str:
    """Return the --env CLI value for the current session."""
    return request.config.getoption("--env")


@pytest.fixture(scope="session")
def config(env: str) -> dict:
    """Return the environment configuration dict."""
    return get_env_config(env)


# ---------------------------------------------------------------------------
# Function-scoped driver fixture
# ---------------------------------------------------------------------------

@pytest.fixture(scope="function")
def driver(config: dict):
    """Create an Appium driver, yield it, then quit it after each test."""
    _driver = create_driver(config)
    yield _driver
    quit_driver(_driver)


# ---------------------------------------------------------------------------
# Convenience fixture: page-object factories pre-wired with driver + package
# ---------------------------------------------------------------------------

@pytest.fixture(scope="function")
def app_package(config: dict) -> str:
    """Return the app package name for the active environment."""
    return config["app_package"]
