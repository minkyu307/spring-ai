package minkyu307.spring_ai.exception;

/**
 * 현재 사용자에게 허용되지 않은 작업 수행 시 사용한다.
 */
public class ForbiddenOperationException extends RuntimeException {

	public ForbiddenOperationException(String message) {
		super(message);
	}
}
