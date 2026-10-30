from typing import Protocol


class MessageSender(Protocol):
    def send(self, user, msg: str) -> None: ...


class NotificationService:
    def __init__(self, sender: MessageSender):  # injected, typed by the abstraction
        self._sender = sender

    def notify(self, user, msg: str) -> None:
        self._sender.send(user, msg)
