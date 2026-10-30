class EmailNotifier:
    def send(self, to: str, msg: str) -> None: ...


class SmsNotifier:
    def send(self, to: str, msg: str) -> None: ...


_CREATORS = {"email": EmailNotifier, "sms": SmsNotifier}  # classes are callables


def create_notifier(channel: str):
    try:
        return _CREATORS[channel]()
    except KeyError:
        raise ValueError(f"unknown channel {channel}") from None
