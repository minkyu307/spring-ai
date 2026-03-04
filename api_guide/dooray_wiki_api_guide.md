## Wiki > Pages

### GET /wiki/v1/wikis

- 접근 가능한 위키 목록

#### Request

- Parameters:

```
  page={}       /* 페이지번호(0 base), Default value : 0 */
  size={}       /* 페이지사이즈, Default value : 20 */
```

#### Response

```javascript
{
  "header": {
    "isSuccessful": true,
    "resultCode": 0,
    "resultMessage": "Success"
  },
  "result": [
      {
        "id": "100",
        "project": {
            "id": "10"
        },
        "name": "Dooray-공지사항",
        "type": "public",
        "scope": "public",
        "home": {
          "pageId": "1001"
        }
      }
    ],
    "totalCount": 1
}
```

- HTTP 응답코드
    - 200
    - 400
    - 401
    - 403
    - 404
    - 500

### GET /wiki/v1/pages/{page-id}

조회 가능한 위키 페이지{page-id} 상세 조회

#### Request

- path: {page-id} - 위키 페이지 id

#### Response

```javascript
{
    "header": {
        "isSuccessful": true,
        "resultMessage": "",
        "resultCode": 0
    },
    "result": {
        "id": "100",
        "wikiId": "1",
        "version": "2",
        "parentPageId": "10",
        "subject": "공지사항",
        "body": {
            "mimeType": "text/x-markdown",          /* text/x-markdown */
            "content": "위키 본문 내용"
        },
        "root": true,
        "createdAt": "2023-04-21T15:47:14+09:00",
        "updatedAt": "2023-09-14T12:26:19+09:00",
        "creator": {
            "type": "member",
            "member": {
                "organizationMemberId": "3521165460461543659"
            }
        },
        "referrers": [                           /* 참조자 */
            {
                "type": "member",
                "member": {
                    "organizationMemberId": ""
                }
            }
        ],
        "files": [{
            "id": "3751642702429982329",
            "name": "test.xlsx",
            "size": 5167119,
            "attachFileId": "3751642694059202495"
        }],
        "images": [{  // 인라인 이미지 파일 목록
            "id": "4071828729722696495",
            "name": "Inline-image-2025-06-02 15.03.20.400.png",
            "size": 52911,
            "attachFileId": "3627153387907672202"
        }]
    }
}
```

- HTTP 응답 코드
    - 200
    - 401
    - 403
    - 404
    - 500

### POST /wiki/v1/wikis/{wiki-id}/pages

- 위키 페이지 생성

#### Request

- Header

```
    Content-Type: application/json
```

- Body

```javascript
{
    "parentPageId": "{parentPageId}",       /* wiki 부모 페이지를 지정 */
    "subject": "두레이 사용법",
    "body": {
        "mimeType": "text/x-markdown",      /* text/x-markdown */
        "content": "위키 본문 내용"
    },
    "attachFileIds": [ "{attachFileId}" ],
    "referrers": [                           /* 참조자 설정 */
        {
            "type": "member",
            "member": {
                "organizationMemberId": ""
            }
        }
    ]
}
```

- 본문은 markdown 형식으로 처리됩니다.
- referrers 필드는 위키의 참조자를 설정합니다.
    - type 필드는 member 고정값 입니다.

#### Response

- Body

```javascript
{
    "header": {
        "isSuccessful": true,
        "resultCode": 0,
        "resultMessage": "Success"
    },
    "result": {
        "id": "100",
        "wikiId": "1",
        "parentPageId": "10",
        "version": 2
    }
}
```

- HTTP 응답 코드
    - 200
    - 201
    - 401
    - 403
    - 404
    - 409
    - 415
    - 500

### GET /wiki/v1/wikis/{wiki-id}/pages

- 위키 페이지들 한 depth(sibling) 페이지들 조회

#### Request

- Parameters

```
  parentPageId={}               /* 상위 페이지 아이디(null 이면 최상위 페이지들 조회) */
```

#### Response

- Body

```javascript
{
    "header": {
        "isSuccessful": true,
        "resultCode": 0,
        "resultMessage": "Success"
    },
    "result": [{
        "id": "100",
        "wikiId": "1",
        "version": "2",
        "parentPageId": "10",
        "subject": "공지사항",
        "root": true,
        "creator": {
            "type": "member",
            "member": {
                "organizationMemberId": "2139624229289676300"
            }
        }
    }]
}
```

- HTTP 응답 코드
    - 200
    - 401
    - 403
    - 404
    - 500

### GET /wiki/v1/wikis/{wiki-id}/pages/{page-id}

- 위키 페이지 1개를 응답

#### Response

- Body

```javascript
{
    "header": {
        "isSuccessful": true,
        "resultCode": 0,
        "resultMessage": "Success"
    },
    "result": {
        "id": "100",
        "wikiId": "1",
        "version": "2",
        "parentPageId": "10",
        "subject": "공지사항",
        "body": {
            "mimeType": "text/x-markdown",          /* text/x-markdown */
            "content": "위키 본문 내용"
        },
        "root": true,
        "createdAt": "2019-08-08T16:58:27+09:00",
        "creator": {
            "type": "member",
            "member": {
                "organizationMemberId": "2139624229289676300",
            }
        },
        "updatedAt": "2019-08-08T16:58:27+09:00",
        "referrers": [                           /* 참조자 */
            {
                "type": "member",
                "member": {
                    "organizationMemberId": ""
                }
            }
        ],
        "files": [{  // 첨부 파일 목록
            "id": "4071828729722696495",
            "name": "test.xlsx",
            "size": 52911
        }],
        "images": [{  // 인라인 이미지 파일 목록
            "id": "4071828729722696495",
            "name": "Inline-image-2025-06-02 15.03.20.400.png",
            "size": 52911
        }]
    }
}
```

- HTTP 응답 코드
    - 200
    - 401
    - 403
    - 404
    - 409
    - 500

### PUT /wiki/v1/wikis/{wiki-id}/pages/{page-id}

- 위키 페이지 1건 제목+본문 수정

#### Request

- Body

```javascript
{
    "subject": "두레이 사용법",
    "body": {
        "mimeType": "text/x-markdown",          /* text/x-markdown */
        "content": "위키 본문 내용 블라블라..."
    },
    "referrers": [                           /* 참조자 설정 */
        {
            "type": "member",
            "member": {
                "organizationMemberId": ""
            }
        }
    ]
}
```

- referrers 필드는 위키의 참조자를 설정합니다.
    - type 필드는 member 고정값 입니다.
    - 기존의 참조자는 모두 지워지고 입력된 참조자로 덮어씁니다.
    - 필드값이 null이면, 기존의 참조자는 모두 지워집니다.
- HTTP 응답 코드
    - 200
    - 401
    - 403
    - 404
    - 409
    - 500

### PUT /wiki/v1/wikis/{wiki-id}/pages/{page-id}/title

- 위키 페이지 1건 제목 수정

#### Request

- Body

```javascript
{
    "subject": "두레이 사용법"
}
```

#### Response

- Body

```javascript
{
    "header": {
        "isSuccessful": true,
        "resultCode": 0,
        "resultMessage": "Success"
    },
    "result": null
}
```

- HTTP 응답 코드
    - 200
    - 401
    - 403
    - 404
    - 409 제목이 겹치는 경우
    - 500

### PUT /wiki/v1/wikis/{wiki-id}/pages/{page-id}/content

- 위키 페이지 1건 내용 수정

#### Request

- Body

```javascript
{
    "body": {
        "mimeType": "text/x-markdown",          /* text/x-markdown */
        "content": "위키 본문 내용"
    }
}
```

#### Response

- Body

```javascript
{
    "header": {
        "isSuccessful": true,
        "resultCode": 0,
        "resultMessage": "Success"
    },
    "result": null
}
```

- HTTP 응답 코드
    - 200
    - 401
    - 403
    - 404
    - 500

### PUT /wiki/v1/wikis/{wiki-id}/pages/{page-id}/referrers

- page-id에 해당하는 wiki의 참조자를 업데이트 합니다.

#### Request

- Body

```javascript
{
  "referrers": [
    {
      "type": "member",
      "member": {
        "organizationMemberId": "3021133863024909194"
      }
    }
  ]
}
```

#### Response

- Body

```javascript
{
    "header": {
        "isSuccessful": true,
        "resultCode": 0,
        "resultMessage": "Success"
    },
    "result": null
}
```

- HTTP 응답 코드
    - 200
    - 400
    - 401
    - 403
    - 404
    - 500
