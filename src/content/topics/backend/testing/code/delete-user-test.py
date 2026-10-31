# Production code under test
class UserNotFound(Exception):
    pass

class UserService:
    def __init__(self, users, events):
        self.users, self.events = users, events

    def delete_user(self, user_id: str) -> None:
        user = self.users.find_by_id(user_id)
        if user is None:
            raise UserNotFound(f"user {user_id} not found")
        self.users.delete(user)
        self.events.publish({"type": "UserDeleted", "user_id": user_id})

# test_user_service.py (pytest + unittest.mock)
from unittest.mock import Mock, call
import pytest

def test_delete_existing_user_deletes_then_publishes():
    # attach both mocks to one parent to check call ORDER
    parent = Mock()
    users, events = parent.users, parent.events
    alice = {"id": "U1"}
    users.find_by_id.return_value = alice

    UserService(users, events).delete_user("U1")

    assert parent.mock_calls[-2:] == [call.users.delete(alice),
                                      call.events.publish({"type": "UserDeleted", "user_id": "U1"})]

def test_delete_missing_user_raises_and_changes_nothing():
    users, events = Mock(), Mock()
    users.find_by_id.return_value = None

    with pytest.raises(UserNotFound, match="ghost"):
        UserService(users, events).delete_user("ghost")

    users.delete.assert_not_called()
    events.publish.assert_not_called()
