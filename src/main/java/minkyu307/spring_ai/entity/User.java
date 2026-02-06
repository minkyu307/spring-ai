package minkyu307.spring_ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 엔티티. Role과 N:1 관계(한 사용자당 역할 하나).
 * loginId: PK, OAuth sub 또는 직접가입 시 로그인 아이디 등 고유 식별자.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

	@Id
	@Column(name = "login_id", nullable = false, unique = true, columnDefinition = "text")
	@EqualsAndHashCode.Include
	private String loginId;

	@Column(nullable = false, columnDefinition = "text")
	private String username;

	@Column(columnDefinition = "text")
	private String password;

	@Column(columnDefinition = "text")
	private String email;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	public User(String loginId, String username, String password, Role role) {
		this.loginId = loginId;
		this.username = username;
		this.password = password;
		this.role = role;
	}
}
