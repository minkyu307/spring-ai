---
name: git-commit-push
description: 이 프로젝트의 커밋 컨벤션에 맞게 변경사항을 스테이징, 커밋, 푸시한다. PowerShell 환경에서 heredoc 대신 백틱 개행을 사용한다. 사용자가 "커밋", "commit", "push", "푸시" 등을 요청할 때 사용한다.
---

# Git Commit & Push (spring-ai 프로젝트)

## 커밋 메시지 컨벤션

```
YYMMDD <type>: <한국어 요약>

- 변경 상세 1
- 변경 상세 2
```

**type 목록**: `feat` / `fix` / `refactor` / `docs` / `test` / `chore`

**실제 커밋 예시:**
```
260304 fix: Wiki 트리 체크박스 미렌더링 하위 페이지 카운트 누락 수정
260303 feat: subtree API BFS 레벨 단위 병렬화 및 429 재시도 처리
260227 feat: Dooray Wiki 트리 UI 및 체크박스 연동 구현
```

## 실행 순서

### 1. 변경사항 확인 (병렬 실행)
```bash
git status
git diff --stat
git log --oneline -5
```

### 2. 커밋 (PowerShell — heredoc 불가, 백틱 개행 사용)
```powershell
git add . && git commit -m "YYMMDD <type>: <요약>`n`n- 상세1`n- 상세2"
```

> **주의**: PowerShell에서는 `$(cat <<'EOF' ... EOF)` heredoc이 동작하지 않는다.  
> 멀티라인은 반드시 `` `n `` 으로 처리한다.

### 3. 푸시
```bash
git push
```

## 체크리스트

- [ ] `git log --oneline -5` 로 날짜·type 컨벤션 확인
- [ ] 커밋 메시지 첫 줄: `YYMMDD type: 한국어 요약`
- [ ] PowerShell 멀티라인: `` `n `` 사용
- [ ] 푸시 후 원격 반영 확인 (`To https://...` 출력)
