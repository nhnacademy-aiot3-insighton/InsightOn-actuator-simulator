package com.insighton.actuatorsimulator.protocol;

/** 시뮬레이터가 낼 수 있는 오류. GlobalExceptionHandler가 공급자 API 스타일 상태코드로 매핑한다. */
public final class SimulatorException {

    private SimulatorException() {
    }

    /** 지원하지 않는 capability/property, 잘못된 명령 값 → 400 */
    public static class BadRequest extends RuntimeException {
        public BadRequest(String message) {
            super(message);
        }
    }

    /** 잘못된 Bearer token → 401 */
    public static class Unauthorized extends RuntimeException {
        public Unauthorized() {
            super("유효하지 않은 인증 토큰입니다");
        }
    }
}
