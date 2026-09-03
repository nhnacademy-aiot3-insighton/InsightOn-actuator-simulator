package com.insighton.actuatorsimulator.smartthings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.actuatorsimulator.protocol.ProviderCommand;
import com.insighton.actuatorsimulator.protocol.ProviderTokenValidator;
import com.insighton.actuatorsimulator.smartthings.dto.SmartThingsCommandRequest;
import com.insighton.actuatorsimulator.smartthings.dto.SmartThingsCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SmartThings 공식 API 호환 endpoint.
 *
 * <p><b>실제:</b> {@code POST https://api.smartthings.com/v1/devices/{deviceId}/commands},
 * 헤더 {@code Authorization: Bearer <token>}. 로컬은 base-url {@code http://localhost:8090/smartthings} 라서
 * 컨트롤러 경로가 {@code /smartthings/v1/devices/{id}/commands} — base-url만 실제 주소로 바꾸면
 * CORE의 {@code .uri("/v1/devices/{id}/commands")} 가 그대로 실제 endpoint를 친다.
 *
 * <p>상태도 기기 카탈로그도 들고 있지 않는다 - 받은 명령을 파싱해 형식만 검증하고 ACCEPTED를 돌려준다.
 * deviceId가 무엇이든(실제 SmartThings처럼 UUID여도) 받는다. 어떤 공급자/종류인지는 CORE가 이미 알고 JSON을 만든다.
 */
@RestController
@RequestMapping("/smartthings/v1")
@RequiredArgsConstructor
@Slf4j
public class SmartThingsController {

    private final ProviderTokenValidator tokenValidator;
    private final SmartThingsRequestTranslator translator;
    private final SmartThingsResponseAssembler assembler;
    private final ObjectMapper objectMapper;

    // 토큰 확인 → 요청 로그 → 구조 검증 → capability 수만큼 ACCEPTED 응답
    @PostMapping("/devices/{deviceId}/commands")
    public SmartThingsCommandResponse executeCommands(
            @PathVariable String deviceId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SmartThingsCommandRequest request) {

        tokenValidator.validate(authorization);
        log.info("[SMART_THINGS] {} ← {}", deviceId, toJson(request));
        ProviderCommand command = translator.translate(deviceId, request);
        log.debug("[SMART_THINGS] {} 해석 → {}", deviceId, command.desiredState());
        return assembler.commandResponse(command.desiredState().keySet());
    }

    // 객체를 로그용 JSON 문자열로 (실패 시 toString)
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
