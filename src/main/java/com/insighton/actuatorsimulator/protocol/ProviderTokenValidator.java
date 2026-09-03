package com.insighton.actuatorsimulator.protocol;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 공급자 호환 API 공통 - Authorization: Bearer &lt;token&gt; 검증. 실제 OAuth는 범위 밖, 고정 토큰만 확인. */
@Component
public class ProviderTokenValidator {

    private static final String BEARER = "Bearer ";
    private final String expectedToken;

    public ProviderTokenValidator(@Value("${simulator.provider-token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void validate(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER)
                || !authorizationHeader.substring(BEARER.length()).equals(expectedToken)) {
            throw new SimulatorException.Unauthorized();
        }
    }
}
