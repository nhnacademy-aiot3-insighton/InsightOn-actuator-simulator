package com.insighton.actuatorsimulator.lg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.actuatorsimulator.lg.dto.LgThinQControlResponse;
import com.insighton.actuatorsimulator.protocol.ProviderCommand;
import com.insighton.actuatorsimulator.protocol.ProviderTokenValidator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LG ThinQ Connect API 호환 endpoint.
 *
 * <p><b>실제:</b> {@code POST https://api-kic.lgthinq.com/devices/{deviceId}/control} (KR 리전, US=-aic / EU=-eic).
 * 로컬은 base-url {@code http://localhost:8090/lg} 라서 컨트롤러 경로가 {@code /lg/devices/{id}/control} —
 * base-url만 실제 주소로 바꾸면 CORE의 `.uri("/devices/{id}/control")` 이 그대로 실제 endpoint를 친다.
 *
 * <p>실제 헤더: {@code Authorization: Bearer}, {@code x-api-key}, {@code x-client-id}, {@code x-message-id},
 * {@code x-country}, {@code x-service-phase}, 제어엔 {@code x-conditional-control: true}.
 * 시뮬레이터는 Bearer 토큰만 확인하고 나머지는 로그로 남긴다 (로컬 개발 편의).
 *
 * <p>상태도 기기 카탈로그도 들고 있지 않는다 — 받은 payload를 파싱해 형식만 검증하고 messageId를 돌려준다.
 */
@RestController
@RequestMapping("/lg/devices")
@RequiredArgsConstructor
@Slf4j
public class LgThinQController {

    private final ProviderTokenValidator tokenValidator;
    private final LgThinQRequestTranslator translator;
    private final ObjectMapper objectMapper;

    @PostMapping("/{deviceId}/control")
    public LgThinQControlResponse control(
            @PathVariable String deviceId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "x-client-id", required = false) String clientId,
            @RequestHeader(value = "x-message-id", required = false) String messageId,
            @RequestHeader(value = "x-country", required = false) String country,
            @RequestBody Map<String, Object> body) {

        tokenValidator.validate(authorization);
        log.info("[LG_THINQ] {} ← {}  (x-api-key={}, x-client-id={}, x-message-id={}, x-country={})",
                deviceId, toJson(body), present(apiKey), present(clientId), present(messageId), country);
        ProviderCommand command = translator.translate(deviceId, body);
        log.debug("[LG_THINQ] {} 해석 → {}", deviceId, command.desiredState());
        return LgThinQControlResponse.accepted();
    }

    private String present(String header) {
        return header == null || header.isBlank() ? "(없음)" : "(있음)";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
