package minkyu307.spring_ai.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 요청 DTO. 서버 측 유효성 검사용.
 */
@Getter
@Setter
public class SignUpRequest {

    @NotBlank(message = "아이디를 입력하세요.")
    @Size(min = 4, max = 64, message = "아이디는 4~64자여야 합니다.")
    @Pattern(regexp = "[a-zA-Z0-9_]+", message = "아이디는 영문, 숫자, _ 만 사용할 수 있습니다.")
    private String loginId;

    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    private String username;

    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "올바른 이메일 형식을 입력하세요.")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 128, message = "비밀번호는 8~128자여야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordConfirm;

    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    public boolean isPasswordMatch() {
        return password == null || password.equals(passwordConfirm);
    }
}
