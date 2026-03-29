package minkyu307.spring_ai.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.dto.AdminDeleteUsersRequest;
import minkyu307.spring_ai.dto.AdminSendUpdateNotificationRequest;
import minkyu307.spring_ai.dto.AdminUserListItemDto;
import minkyu307.spring_ai.service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 사용자 관리 API.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final AdminUserService adminUserService;

    /**
     * 사용자 목록을 조회한다.
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserListItemDto>> listUsers() {
        return ResponseEntity.ok(adminUserService.findAllUsers());
    }

    /**
     * 선택 사용자들을 삭제한다.
     */
    @DeleteMapping("/users")
    public ResponseEntity<Map<String, Integer>> deleteUsers(
        @Valid @RequestBody AdminDeleteUsersRequest request
    ) {
        int deletedCount = adminUserService.deleteUsers(request.loginIds());
        return ResponseEntity.ok(Map.of("deletedCount", deletedCount));
    }

    /**
     * 선택 사용자들에게 업데이트 알림 메일을 발송한다.
     */
    @PostMapping("/users/update-notification")
    public ResponseEntity<Map<String, Integer>> sendUpdateNotificationMail(
        @Valid @RequestBody AdminSendUpdateNotificationRequest request
    ) {
        int sentCount = adminUserService.sendUpdateNotificationMails(request.loginIds(), request.content());
        return ResponseEntity.ok(Map.of("sentCount", sentCount));
    }
}
