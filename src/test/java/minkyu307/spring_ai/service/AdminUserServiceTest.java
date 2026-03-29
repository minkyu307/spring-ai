package minkyu307.spring_ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import minkyu307.spring_ai.dto.MailSendRequest;
import minkyu307.spring_ai.entity.Role;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.exception.ForbiddenOperationException;
import minkyu307.spring_ai.repository.ChatConversationRepository;
import minkyu307.spring_ai.repository.ChatMemoryJdbcQueryRepository;
import minkyu307.spring_ai.repository.RagVectorStoreJdbcRepository;
import minkyu307.spring_ai.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @Mock
    private ChatMemoryJdbcQueryRepository chatMemoryJdbcQueryRepository;

    @Mock
    private ChatConversationRepository chatConversationRepository;

    @Mock
    private RagVectorStoreJdbcRepository ragVectorStoreJdbcRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
            userRepository,
            chatMemoryJdbcQueryRepository,
            chatConversationRepository,
            ragVectorStoreJdbcRepository,
            mailService,
            new UpdateNotificationMailTemplate()
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteUsersWhenSelfIncludedThenThrowsForbiddenOperationException() {
        authenticateAs("admin", ROLE_ADMIN);

        List<String> targetIds = List.of("admin", "user1");

        assertThatThrownBy(() -> adminUserService.deleteUsers(targetIds))
            .isInstanceOf(ForbiddenOperationException.class)
            .hasMessageContaining("현재 로그인한 사용자는 삭제할 수 없습니다");
        verify(userRepository, never()).deleteAll(any());
        verify(chatMemoryJdbcQueryRepository, never()).deleteByLoginIds(any());
        verify(chatConversationRepository, never()).deleteByLoginIdIn(any());
        verify(ragVectorStoreJdbcRepository, never()).deleteByLoginIds(any());
    }

    @Test
    void deleteUsersWhenAdminIncludedThenThrowsForbiddenOperationException() {
        authenticateAs("admin", ROLE_ADMIN);

        List<String> targetIds = List.of("user1", "superadmin");
        when(userRepository.findAllById(targetIds)).thenReturn(List.of(
            user("user1", "일반사용자", ROLE_USER, "user1@test.com"),
            user("superadmin", "최고관리자", ROLE_ADMIN, "admin@test.com")
        ));

        assertThatThrownBy(() -> adminUserService.deleteUsers(targetIds))
            .isInstanceOf(ForbiddenOperationException.class)
            .hasMessageContaining("ROLE_ADMIN 사용자는 삭제할 수 없습니다");
        verify(userRepository, never()).deleteAll(any());
        verify(chatMemoryJdbcQueryRepository, never()).deleteByLoginIds(any());
        verify(chatConversationRepository, never()).deleteByLoginIdIn(any());
        verify(ragVectorStoreJdbcRepository, never()).deleteByLoginIds(any());
    }

    @Test
    void deleteUsersWhenValidTargetsThenDeletesRelatedDataBeforeUsers() {
        authenticateAs("admin", ROLE_ADMIN);

        List<String> targetIds = List.of("user1", "user2");
        when(userRepository.findAllById(targetIds)).thenReturn(List.of(
            user("user1", "사용자1", ROLE_USER, "user1@test.com"),
            user("user2", "사용자2", ROLE_USER, "user2@test.com")
        ));

        int deletedCount = adminUserService.deleteUsers(targetIds);

        assertThat(deletedCount).isEqualTo(2);
        InOrder inOrder = inOrder(
            chatMemoryJdbcQueryRepository,
            chatConversationRepository,
            ragVectorStoreJdbcRepository,
            userRepository
        );
        inOrder.verify(chatMemoryJdbcQueryRepository).deleteByLoginIds(List.of("user1", "user2"));
        inOrder.verify(chatConversationRepository).deleteByLoginIdIn(List.of("user1", "user2"));
        inOrder.verify(ragVectorStoreJdbcRepository).deleteByLoginIds(List.of("user1", "user2"));
        inOrder.verify(userRepository).deleteAll(any());
    }

    @Test
    void sendUpdateNotificationMailsUsesCurrentUserEmailAsFrom() {
        authenticateAs("admin", ROLE_ADMIN);

        User sender = user("admin", "관리자", ROLE_ADMIN, "admin@test.com");
        List<String> targetIds = List.of("user1", "user2");
        when(userRepository.findById("admin")).thenReturn(Optional.of(sender));
        when(userRepository.findAllById(targetIds)).thenReturn(List.of(
            user("user1", "사용자1", ROLE_USER, "user1@test.com"),
            user("user2", "사용자2", ROLE_USER, "user2@test.com")
        ));

        int sentCount = adminUserService.sendUpdateNotificationMails(targetIds, "신규 기능이 배포되었습니다.");

        assertThat(sentCount).isEqualTo(2);
        ArgumentCaptor<MailSendRequest> captor = ArgumentCaptor.forClass(MailSendRequest.class);
        verify(mailService, times(2)).send(captor.capture());
        List<MailSendRequest> requests = captor.getAllValues();
        assertThat(requests).hasSize(2);
        assertThat(requests)
            .extracting(MailSendRequest::from)
            .containsOnly("admin@test.com");
        assertThat(requests)
            .extracting(MailSendRequest::subject)
            .containsOnly("DocuSearch 업데이트 알림");
        assertThat(requests)
            .extracting(MailSendRequest::body)
            .allSatisfy(body -> {
                assertThat(body).contains("보내는 사람: admin@test.com");
                assertThat(body).contains("DocuSearch 업데이트 알림");
                assertThat(body).contains("신규 기능이 배포되었습니다.");
                assertThat(body).contains("감사합니다.");
            });
    }

    @Test
    void sendUpdateNotificationMailsWhenSenderEmailMissingThenThrowsIllegalStateException() {
        authenticateAs("admin", ROLE_ADMIN);
        User senderWithoutEmail = user("admin", "관리자", ROLE_ADMIN, "");

        when(userRepository.findById("admin")).thenReturn(Optional.of(senderWithoutEmail));

        assertThatThrownBy(() -> adminUserService.sendUpdateNotificationMails(List.of("user1"), "공지"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("현재 로그인 사용자 이메일이 설정되지 않아 메일을 발송할 수 없습니다");
        verify(mailService, never()).send(any());
    }

    private void authenticateAs(String loginId, String... roles) {
        UserDetails principal = org.springframework.security.core.userdetails.User.builder()
            .username(loginId)
            .password("N/A")
            .authorities(roles)
            .build();
        var authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user(String loginId, String username, String roleName, String email) {
        Role role = new Role(roleName);
        User user = new User(loginId, username, "encoded-password", role);
        user.setEmail(email);
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return user;
    }
}
