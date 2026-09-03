package com.insighton.actuatorsimulator.protocol;

import java.util.Map;

/**
 * 공급자 독립 명령 - 각 공급자 RequestTranslator가 자기 요청 DTO를 이 형태로 바꾼다.
 * 시뮬레이터는 이걸 저장하지 않는다. 응답을 조립하는 데만 쓴다.
 */
public record ProviderCommand(
        String deviceId,
        Map<String, Object> desiredState,
        String providerLabel
) {
}
