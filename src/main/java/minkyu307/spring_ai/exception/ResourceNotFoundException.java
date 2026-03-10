package minkyu307.spring_ai.exception;

/**
 * 조회 대상 리소스를 찾을 수 없을 때 사용한다.
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
