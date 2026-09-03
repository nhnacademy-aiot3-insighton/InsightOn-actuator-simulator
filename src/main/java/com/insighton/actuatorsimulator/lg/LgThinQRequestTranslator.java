package com.insighton.actuatorsimulator.lg;

import com.insighton.actuatorsimulator.protocol.ProviderCommand;
import com.insighton.actuatorsimulator.protocol.SimulatorException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * LG ThinQ Connect control payload의 구조 검증기.
 *
 * <p>값을 CORE 어휘로 되돌리지 않는다 — 어떤 resource가 어떤 property/값으로 왔는지 그대로 받아
 * messageId만 돌려준다. resource가 이 목록에 없거나 property 객체 형태가 아니면 400.
 */
@Component
public class LgThinQRequestTranslator {

    private static final String PROVIDER_LABEL = "LG_THINQ";

    // 이 mock이 아는 resource (실제 LG ThinQ Connect resource 이름). 이 외는 400.
    private static final Set<String> KNOWN_RESOURCES = Set.of(
            "operation", "airConJobMode", "airPurifierJobMode", "airFlow", "windDirection", "temperature");

    // resource 중첩 payload를 구조 검증하고 "resource.property" → 받은 값 맵으로 평탄화 (로그용. 값 변환 없음).
    @SuppressWarnings("unchecked")
    public ProviderCommand translate(String deviceId, Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new SimulatorException.BadRequest("변경할 property가 없습니다");
        }

        Map<String, Object> received = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            String resource = entry.getKey();
            if (!KNOWN_RESOURCES.contains(resource)) {
                throw new SimulatorException.BadRequest("지원하지 않는 resource: " + resource);
            }
            if (!(entry.getValue() instanceof Map<?, ?> props) || props.isEmpty()) {
                throw new SimulatorException.BadRequest(resource + " 는 property 객체여야 합니다");
            }
            ((Map<String, Object>) props).forEach((property, value) -> received.put(resource + "." + property, value));
        }
        return new ProviderCommand(deviceId, received, PROVIDER_LABEL);
    }
}
