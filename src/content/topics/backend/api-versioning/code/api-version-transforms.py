from dataclasses import dataclass
from typing import Callable


@dataclass(frozen=True)
class VersionChange:
    """One breaking change: how to turn a response from after this date into one from before it."""

    date: str
    description: str
    downgrade: Callable[[dict], dict]


def render_for(latest, client_version, newest_first):
    """Walks the newest response back through each change newer than the client's version.

    The code only ever builds the newest response, so old versions cost one small function each.
    """
    response = dict(latest)
    for change in newest_first:
        if change.date <= client_version:
            break
        response = change.downgrade(response)
    return response
