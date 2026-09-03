package com.insighton.actuatorsimulator.smartthings;

import com.insighton.actuatorsimulator.protocol.ProviderCommand;
import com.insighton.actuatorsimulator.protocol.SimulatorException;
import com.insighton.actuatorsimulator.smartthings.dto.SmartThingsCommandRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * SmartThings "Execute commands" 요청의 구조 검증기.
 *
 * <p>값을 CORE 어휘로 되돌리지 않는다 — 어떤 capability가 어떤 command/argument로 왔는지 그대로 받아
 * capability 수만큼 ACCEPTED를 돌려준다. capability가 이 목록에 없으면 400 (실제 API도 마찬가지).
 */
@Component
public class SmartThingsRequestTranslator {

    private static final String PROVIDER_LABEL = "SMART_THINGS";

    // 이 mock이 아는 capability (실제 SmartThings capability 이름). 이 외는 400.
    private static final Set<String> KNOWN_CAPABILITIES = Set.of(
            "switch", "airConditionerMode", "airPurifierFanMode", "fanSpeed",
            "fanOscillationMode", "thermostatCoolingSetpoint");

    // commands 배열을 구조 검증하고 capability→받은 값 맵으로 정리 (로그·result 개수용. 값 변환 없음).
    public ProviderCommand translate(String deviceId, SmartThingsCommandRequest request) {
        if (request == null || request.commands() == null || request.commands().isEmpty()) {
            throw new SimulatorException.BadRequest("commands가 비어 있습니다");
        }

        Map<String, Object> received = new LinkedHashMap<>();
        for (SmartThingsCommandRequest.Command command : request.commands()) {
            String capability = command.capability();
            if (capability == null || !KNOWN_CAPABILITIES.contains(capability)) {
                throw new SimulatorException.BadRequest("지원하지 않는 capability: " + capability);
            }
            received.put(capability, "switch".equals(capability) ? command.command() : firstArg(command));
        }
        return new ProviderCommand(deviceId, received, PROVIDER_LABEL);
    }

    // switch 외 capability는 arguments[0]이 있어야 한다
    private Object firstArg(SmartThingsCommandRequest.Command command) {
        List<Object> arguments = command.arguments();
        if (arguments == null || arguments.isEmpty()) {
            throw new SimulatorException.BadRequest(command.capability() + " 명령에 arguments가 없습니다");
        }
        return arguments.get(0);
    }
}
