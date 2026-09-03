package com.insighton.actuatorsimulator.protocol;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 오류 응답을 공급자 API 스타일로 통일한다.
 *
 * <p>SmartThings·LG ThinQ 모두 오류 바디가 {@code { "error": { "code": "...", "message": "..." } }} 형태다
 * (SmartThings는 details 배열이 추가로 붙지만 핵심은 동일). 실연동 시 CORE가 이 형태를 그대로 파싱한다
 * (LgThinQControlResponse.Error).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "error", Map.of("code", code, "message", message == null ? status.getReasonPhrase() : message)));
    }

    @ExceptionHandler({SimulatorException.BadRequest.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(SimulatorException.Unauthorized.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(SimulatorException.Unauthorized e) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
    }
}
