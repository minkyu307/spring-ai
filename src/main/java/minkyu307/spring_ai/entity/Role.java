package minkyu307.spring_ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 권한(역할) 엔티티. 예: administrator, user. Spring Security 권한명은 ROLE_ 접두사(예: ROLE_USER).
 */
@Entity
@Table(name = "app_role")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Role {

	@Id
	@EqualsAndHashCode.Include
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "text")
	private String id;

	@Column(nullable = false, unique = true, columnDefinition = "text")
	private String name;

	@OneToMany(mappedBy = "role")
	private List<User> users = new ArrayList<>();

	public Role(String name) {
		this.id = UUID.randomUUID().toString();
		this.name = name;
	}

	@PrePersist
	private void assignIdIfNecessary() {
		if (this.id == null) {
			this.id = UUID.randomUUID().toString();
		}
	}
}
