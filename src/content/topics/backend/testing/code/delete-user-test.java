// Production code under test
@Service
class UserService {
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    UserService(UserRepository users, ApplicationEventPublisher events) { this.users = users; this.events = events; }

    @Transactional
    public void deleteUser(String userId) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        users.delete(user);
        events.publishEvent(new UserDeleted(userId));
    }
}

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock ApplicationEventPublisher events;
    @InjectMocks UserService service;

    @Test
    void deleteUser_existing_deletesAndPublishesEvent() {
        User alice = new User("U1", "alice@example.com");
        when(users.findById("U1")).thenReturn(Optional.of(alice));

        service.deleteUser("U1");

        // the event only after the delete
        InOrder inOrder = inOrder(users, events);
        inOrder.verify(users).delete(alice);
        // records compare by value
        inOrder.verify(events).publishEvent((Object) new UserDeleted("U1"));
    }

    @Test
    void deleteUser_missing_throwsAndChangesNothing() {
        when(users.findById("ghost")).thenReturn(Optional.empty());

        var ex = assertThrows(UserNotFoundException.class, () -> service.deleteUser("ghost"));

        assertThat(ex.getMessage()).contains("ghost");
        verify(users, never()).delete(any());
        verifyNoInteractions(events);
    }
}
