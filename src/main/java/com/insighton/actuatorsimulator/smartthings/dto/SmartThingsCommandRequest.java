package com.insighton.actuatorsimulator.smartthings.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * SmartThings "Execute commands" 요청을 받는 독립 inbound DTO.
 * CORE의 outbound DTO와 형태는 같지만 클래스는 공유하지 않는다 (코드가 아닌 계약으로 연결).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SmartThingsCommandRequest(List<Command> commands) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Command(
            String component,
            String capability,
            String command,
            List<Object> arguments
    ) {
    }
}
