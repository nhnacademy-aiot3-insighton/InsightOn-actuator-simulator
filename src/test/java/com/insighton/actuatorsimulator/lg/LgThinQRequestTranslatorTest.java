package com.insighton.actuatorsimulator.lg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insighton.actuatorsimulator.protocol.ProviderCommand;
import com.insighton.actuatorsimulator.protocol.SimulatorException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LgThinQRequestTranslatorTest {

    private final LgThinQRequestTranslator translator = new LgThinQRequestTranslator();

    // 실제 LG payload는 resource로 중첩: { "operation": {"airConOperationMode": "POWER_ON"}, ... }
    private static Map<String, Object> resource(String resource, String property, Object value) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(resource, Map.of(property, value));
        return body;
    }

    private static Map<String, Object> merge(Map<String, Object>... parts) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (Map<String, Object> p : parts) {
            body.putAll(p);
        }
        return body;
    }

    @Test
    @DisplayName("operation.<property> 값을 resource.property 키로 평탄화해 그대로 담는다")
    void power() {
        ProviderCommand on = translator.translate("d", resource("operation", "airConOperationMode", "POWER_ON"));
        assertThat(on.desiredState()).containsEntry("operation.airConOperationMode", "POWER_ON");
        assertThat(on.providerLabel()).isEqualTo("LG_THINQ");
        assertThat(on.deviceId()).isEqualTo("d");

        assertThat(translator.translate("d", resource("operation", "airConOperationMode", "POWER_OFF")).desiredState())
                .containsEntry("operation.airConOperationMode", "POWER_OFF");
    }

    @Test
    @DisplayName("종류마다 operation property 키가 달라도 (airPurifierOperationMode 등) 그대로 평탄화")
    void power_종류무관() {
        assertThat(translator.translate("d", resource("operation", "airPurifierOperationMode", "POWER_ON")).desiredState())
                .containsEntry("operation.airPurifierOperationMode", "POWER_ON");
        assertThat(translator.translate("d", resource("operation", "airFanOperationMode", "POWER_OFF")).desiredState())
                .containsEntry("operation.airFanOperationMode", "POWER_OFF");
    }

    @Test
    @DisplayName("jobMode/airFlow/windDirection/temperature 값을 변환 없이 그대로 담는다")
    void 값_그대로_보존() {
        assertThat(translator.translate("d", resource("airConJobMode", "currentJobMode", "AIR_DRY")).desiredState())
                .containsEntry("airConJobMode.currentJobMode", "AIR_DRY");
        assertThat(translator.translate("d", resource("airPurifierJobMode", "currentJobMode", "SLEEP")).desiredState())
                .containsEntry("airPurifierJobMode.currentJobMode", "SLEEP");
        assertThat(translator.translate("d", resource("airFlow", "windStrength", "HIGH")).desiredState())
                .containsEntry("airFlow.windStrength", "HIGH");
        assertThat(translator.translate("d", resource("windDirection", "rotateUpDown", true)).desiredState())
                .containsEntry("windDirection.rotateUpDown", true);
        assertThat(translator.translate("d", resource("temperature", "targetTemperature", 21)).desiredState())
                .containsEntry("temperature.targetTemperature", 21);
    }

    @Test
    @DisplayName("CORE 어휘로 되돌리지 않는다 — 알 수 없는 값이어도 구조만 맞으면 통과")
    void 값검증_안함() {
        assertThat(translator.translate("d", resource("operation", "airConOperationMode", "POWER_SURGE")).desiredState())
                .containsEntry("operation.airConOperationMode", "POWER_SURGE");
        assertThat(translator.translate("d", resource("airConJobMode", "currentJobMode", "CRYO")).desiredState())
                .containsEntry("airConJobMode.currentJobMode", "CRYO");
    }

    @Test
    @DisplayName("여러 resource -> 모두 평탄화해서 담는다")
    void 복합() {
        ProviderCommand result = translator.translate("d", merge(
                resource("operation", "airConOperationMode", "POWER_ON"),
                resource("airConJobMode", "currentJobMode", "COOL"),
                resource("temperature", "targetTemperature", 20)));
        assertThat(result.desiredState())
                .containsEntry("operation.airConOperationMode", "POWER_ON")
                .containsEntry("airConJobMode.currentJobMode", "COOL")
                .containsEntry("temperature.targetTemperature", 20);
    }

    @Test
    @DisplayName("빈 body면 BadRequest")
    void 빈요청() {
        assertThatThrownBy(() -> translator.translate("d", Map.of()))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("이 mock이 모르는 resource 키면 BadRequest")
    void 미지원resource() {
        assertThatThrownBy(() -> translator.translate("d", resource("laserBeam", "power", "MAX")))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("resource 값이 property 객체가 아니거나 비어 있으면 BadRequest")
    void 잘못된형태() {
        assertThatThrownBy(() -> translator.translate("d", Map.of("operation", "POWER_ON")))
                .isInstanceOf(SimulatorException.BadRequest.class);
        assertThatThrownBy(() -> translator.translate("d", Map.of("operation", Map.of())))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }
}
