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
    @DisplayName("operation.airConOperationMode POWER_ON/POWER_OFF -> power ON/OFF")
    void power() {
        ProviderCommand on = translator.translate("d", resource("operation", "airConOperationMode", "POWER_ON"));
        assertThat(on.desiredState()).containsEntry("power", "ON");
        assertThat(on.providerLabel()).isEqualTo("LG_THINQ");
        assertThat(translator.translate("d", resource("operation", "airConOperationMode", "POWER_OFF")).desiredState())
                .containsEntry("power", "OFF");
    }

    @Test
    @DisplayName("공청기/환풍기 operation 키가 달라도 (airPurifierOperationMode 등) power로 해석")
    void power_종류무관() {
        assertThat(translator.translate("d", resource("operation", "airPurifierOperationMode", "POWER_ON")).desiredState())
                .containsEntry("power", "ON");
        assertThat(translator.translate("d", resource("operation", "airFanOperationMode", "POWER_OFF")).desiredState())
                .containsEntry("power", "OFF");
    }

    @Test
    @DisplayName("airConJobMode.currentJobMode -> CORE mode (AIR_DRY -> DRY)")
    void mode() {
        assertThat(translator.translate("d", resource("airConJobMode", "currentJobMode", "COOL")).desiredState())
                .containsEntry("mode", "COOL");
        assertThat(translator.translate("d", resource("airConJobMode", "currentJobMode", "AIR_DRY")).desiredState())
                .containsEntry("mode", "DRY");
    }

    @Test
    @DisplayName("temperature.targetTemperature -> temperature")
    void temperature() {
        assertThat(translator.translate("d", resource("temperature", "targetTemperature", 21)).desiredState())
                .containsEntry("temperature", 21);
    }

    @Test
    @DisplayName("세 resource 모두 -> desiredState에 모두 반영")
    void 복합() {
        ProviderCommand result = translator.translate("d", merge(
                resource("operation", "airConOperationMode", "POWER_ON"),
                resource("airConJobMode", "currentJobMode", "COOL"),
                resource("temperature", "targetTemperature", 20)));
        assertThat(result.desiredState())
                .containsEntry("power", "ON").containsEntry("mode", "COOL").containsEntry("temperature", 20);
    }

    @Test
    @DisplayName("빈 body면 BadRequest")
    void 빈요청() {
        assertThatThrownBy(() -> translator.translate("d", Map.of()))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("모르는 resource 키면 BadRequest")
    void 미지원resource() {
        assertThatThrownBy(() -> translator.translate("d", resource("laserBeam", "power", "MAX")))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("잘못된 operationMode/jobMode면 BadRequest")
    void 잘못된값() {
        assertThatThrownBy(() -> translator.translate("d", resource("operation", "airConOperationMode", "POWER_SURGE")))
                .isInstanceOf(SimulatorException.BadRequest.class);
        assertThatThrownBy(() -> translator.translate("d", resource("airConJobMode", "currentJobMode", "CRYO")))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("airPurifierJobMode -> mode (SLEEP), 잘못된 값이면 BadRequest")
    void 공청기_mode() {
        assertThat(translator.translate("d", resource("airPurifierJobMode", "currentJobMode", "SLEEP")).desiredState())
                .containsEntry("mode", "SLEEP");
        assertThatThrownBy(() -> translator.translate("d", resource("airPurifierJobMode", "currentJobMode", "HYPER")))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("airFlow.windStrength -> mode (HIGH), 잘못된 값이면 BadRequest")
    void 환풍기_mode() {
        assertThat(translator.translate("d", resource("airFlow", "windStrength", "HIGH")).desiredState())
                .containsEntry("mode", "HIGH");
        assertThatThrownBy(() -> translator.translate("d", resource("airFlow", "windStrength", "GALE")))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }
}
