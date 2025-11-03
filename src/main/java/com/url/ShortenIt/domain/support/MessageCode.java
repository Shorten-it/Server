package com.url.ShortenIt.domain.support;


import lombok.Getter;

@Getter
public enum MessageCode {
    SUCCESS("success", "성공"),
    RESOURCE_DELETED("resource.deleted", "삭제되었습니다."),
    RESOURCE_CREATED("resource.created", "새로 생성되었습니다.");

    private final String code;
    private final String value;

    MessageCode(String code, String value) {
        this.code = code;
        this.value = value;
    }
}
