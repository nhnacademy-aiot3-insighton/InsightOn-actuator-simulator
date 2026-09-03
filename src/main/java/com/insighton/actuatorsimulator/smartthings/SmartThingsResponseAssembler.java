package com.insighton.actuatorsimulator.smartthings;

import com.insighton.actuatorsimulator.smartthings.dto.SmartThingsCommandResponse;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 반영된 상태 키 집합 → SmartThings "Execute commands" 응답 (키 수만큼 ACCEPTED result). */
@Component
public class SmartThingsResponseAssembler {

    // 반영된 상태 키마다 ACCEPTED result 하나씩
    public SmartThingsCommandResponse commandResponse(Set<String> appliedKeys) {
        return new SmartThingsCommandResponse(appliedKeys.stream()
                .map(k -> new SmartThingsCommandResponse.Result(UUID.randomUUID().toString(), "ACCEPTED"))
                .toList());
    }
}
