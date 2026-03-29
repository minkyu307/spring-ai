package minkyu307.spring_ai.service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.dto.AdminUserListItemDto;
import minkyu307.spring_ai.dto.MailSendRequest;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.error.ApiErrorCode;
import minkyu307.spring_ai.error.ApiException;
import minkyu307.spring_ai.exception.ForbiddenOperationException;
import minkyu307.spring_ai.repository.ChatConversationRepository;
import minkyu307.spring_ai.repository.ChatMemoryJdbcQueryRepository;
import minkyu307.spring_ai.repository.RagVectorStoreJdbcRepository;
import minkyu307.spring_ai.repository.UserRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 관리자 사용자 조회/삭제/업데이트 알림 메일 발송을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String UPDATE_NOTIFICATION_SUBJECT = "DocuSearch 업데이트 알림";

    private final UserRepository userRepository;
    private final ChatMemoryJdbcQueryRepository chatMemoryJdbcQueryRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final RagVectorStoreJdbcRepository ragVectorStoreJdbcRepository;
    private final MailService mailService;
    private final UpdateNotificationMailTemplate updateNotificationMailTemplate;

    /**
     * 전체 사용자 목록을 생성일 최신순으로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<AdminUserListItemDto> findAllUsers() {
        return userRepository.findAll().stream()
            .sorted(Comparator.comparing(User::getCreatedAt).reversed().thenComparing(User::getLoginId))
            .map(this::toListItem)
            .toList();
    }

    /**
     * 선택 사용자들을 삭제한다.
     */
    @Transactional
    public int deleteUsers(List<String> loginIds) {
        List<String> normalizedLoginIds = normalizeLoginIds(loginIds, "삭제할 사용자를 한 명 이상 선택하세요.");
        String currentLoginId = SecurityUtils.getCurrentLoginId();
        if (normalizedLoginIds.contains(currentLoginId)) {
            throw new ForbiddenOperationException("현재 로그인한 사용자는 삭제할 수 없습니다.");
        }

        List<User> targetUsers = loadUsersForTargets(normalizedLoginIds);
        boolean hasAdminTarget = targetUsers.stream().anyMatch(this::isAdminUser);
        if (hasAdminTarget) {
            throw new ForbiddenOperationException("ROLE_ADMIN 사용자는 삭제할 수 없습니다.");
        }

        List<String> targetLoginIds = targetUsers.stream().map(User::getLoginId).toList();
        deleteRelatedDataByLoginIds(targetLoginIds);
        userRepository.deleteAll(targetUsers);
        return targetUsers.size();
    }

    /**
     * 선택 사용자들에게 업데이트 알림 메일을 발송한다.
     */
    @Transactional(readOnly = true)
    public int sendUpdateNotificationMails(List<String> loginIds, String content) {
        List<String> normalizedLoginIds = normalizeLoginIds(loginIds, "메일을 보낼 사용자를 한 명 이상 선택하세요.");
        String normalizedContent = normalizeContent(content);
        User sender = loadCurrentUser();
        String senderEmail = requireUserEmail(sender, "현재 로그인 사용자 이메일이 설정되지 않아 메일을 발송할 수 없습니다.");

        List<User> recipients = loadUsersForTargets(normalizedLoginIds);
        List<String> usersWithoutEmail = recipients.stream()
            .filter(user -> !StringUtils.hasText(user.getEmail()))
            .map(User::getLoginId)
            .sorted()
            .toList();
        if (!usersWithoutEmail.isEmpty()) {
            throw new IllegalArgumentException("이메일이 없는 사용자에게는 메일을 발송할 수 없습니다: " + String.join(", ", usersWithoutEmail));
        }

        String body = updateNotificationMailTemplate.render(senderEmail, normalizedContent);
        for (User recipient : recipients) {
            mailService.send(new MailSendRequest(
                senderEmail,
                recipient.getEmail().strip(),
                UPDATE_NOTIFICATION_SUBJECT,
                body
            ));
        }
        return recipients.size();
    }

    private AdminUserListItemDto toListItem(User user) {
        String roleName = user.getRole() == null ? "" : user.getRole().getName();
        String email = user.getEmail() == null ? "" : user.getEmail();
        return new AdminUserListItemDto(
            user.getLoginId(),
            user.getUsername(),
            email,
            roleName,
            user.getCreatedAt()
        );
    }

    private User loadCurrentUser() {
        String currentLoginId = SecurityUtils.getCurrentLoginId();
        return userRepository.findById(currentLoginId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.UNAUTHORIZED,
                "사용자를 찾을 수 없습니다: " + currentLoginId
            ));
    }

    private List<User> loadUsersForTargets(List<String> targetLoginIds) {
        List<User> users = userRepository.findAllById(targetLoginIds);
        Set<String> foundIds = users.stream()
            .map(User::getLoginId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<String> missingIds = targetLoginIds.stream()
            .filter(loginId -> !foundIds.contains(loginId))
            .toList();
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자가 포함되어 있습니다: " + String.join(", ", missingIds));
        }
        return users;
    }

    private List<String> normalizeLoginIds(List<String> loginIds, String emptyMessage) {
        if (loginIds == null || loginIds.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        List<String> normalized = loginIds.stream()
            .filter(Objects::nonNull)
            .map(String::strip)
            .filter(StringUtils::hasText)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        return normalized;
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("업데이트 메일 내용을 입력하세요.");
        }
        return content.strip();
    }

    private String requireUserEmail(User user, String message) {
        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalStateException(message);
        }
        return user.getEmail().strip();
    }

    /**
     * 선택 사용자의 채팅 메모리/대화/벡터 데이터를 먼저 정리한다.
     */
    private void deleteRelatedDataByLoginIds(List<String> loginIds) {
        chatMemoryJdbcQueryRepository.deleteByLoginIds(loginIds);
        chatConversationRepository.deleteByLoginIdIn(loginIds);
        ragVectorStoreJdbcRepository.deleteByLoginIds(loginIds);
    }

    private boolean isAdminUser(User user) {
        return user.getRole() != null && ROLE_ADMIN.equals(user.getRole().getName());
    }
}
