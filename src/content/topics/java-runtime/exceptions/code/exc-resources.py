import json


class ConfigError(Exception):
    pass


def read_config(path: str) -> dict:
    try:
        # context manager: closes the file even on exceptions
        with open(path) as f:
            return json.load(f)
    except FileNotFoundError:
        raise                            # let callers decide
    except json.JSONDecodeError as e:
        # 'from e' keeps the cause chain
        raise ConfigError(f"invalid config {path}") from e
