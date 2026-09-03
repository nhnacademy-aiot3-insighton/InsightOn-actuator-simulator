package com.insighton.actuatorsimulator.lg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * LG ThinQ Connect "device control" 성공 응답:
 * {@code { "messageId": "<uuid>", "timestamp": "2026-09-03T12:00:00Z", "response": {} }}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LgThinQControlResponse(String messageId, String timestamp, Map<String, Object> response) {

    // 매번 새 messageId로 성공 응답 생성
    public static LgThinQControlResponse accepted() {
        return new LgThinQControlResponse(
                UUID.randomUUID().toString(),
                OffsetDateTime.now().toString(),
                Map.of());
    }
}
