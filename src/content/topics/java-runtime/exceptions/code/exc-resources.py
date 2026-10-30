import json


class ConfigError(Exception):
    pass


def read_config(path: str) -> dict:
    try:
        with open(path) as f:            # context manager: closes the file even on exceptions
            return json.load(f)
    except FileNotFoundError:
        raise                            # let callers decide
    except json.JSONDecodeError as e:
        raise ConfigError(f"invalid config {path}") from e   # 'from e' keeps the cause chain
