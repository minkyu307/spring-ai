package minkyu307.spring_ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자별 두레이 API 키 저장 엔티티. User와 1:1 관계(PK 공유).
 */
@Entity
@Table(name = "user_dooray_api_key")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDoorayApiKey {

    /** app_user.login_id를 PK로 공유 */
    @Id
    @Column(name = "login_id", columnDefinition = "text")
    private String loginId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "login_id")
    private User user;

    @Column(name = "api_key", nullable = false, columnDefinition = "text")
    private String apiKey;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserDoorayApiKey(User user, String apiKey) {
        this.user = user;
        this.apiKey = apiKey;
    }
}
