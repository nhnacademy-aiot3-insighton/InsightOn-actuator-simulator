package com.insighton.actuatorsimulator.smartthings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insighton.actuatorsimulator.protocol.ProviderCommand;
import com.insighton.actuatorsimulator.protocol.SimulatorException;
import com.insighton.actuatorsimulator.smartthings.dto.SmartThingsCommandRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SmartThingsRequestTranslatorTest {

    private final SmartThingsRequestTranslator translator = new SmartThingsRequestTranslator();

    private static SmartThingsCommandRequest request(SmartThingsCommandRequest.Command... commands) {
        return new SmartThingsCommandRequest(List.of(commands));
    }

    private static SmartThingsCommandRequest.Command cmd(String capability, String command, Object... args) {
        return new SmartThingsCommandRequest.Command("main", capability, command, List.of(args));
    }

    @Test
    @DisplayName("switch on/off -> power ON/OFF")
    void power() {
        ProviderCommand on = translator.translate("d", request(cmd("switch", "on")));
        assertThat(on.desiredState()).containsEntry("power", "ON");
        assertThat(on.providerLabel()).isEqualTo("SMART_THINGS");
        assertThat(on.deviceId()).isEqualTo("d");

        assertThat(translator.translate("d", request(cmd("switch", "off"))).desiredState())
                .containsEntry("power", "OFF");
    }

    @Test
    @DisplayName("airConditionerMode -> CORE mode (cool->COOL, wind->FAN)")
    void mode() {
        assertThat(translator.translate("d", request(cmd("airConditionerMode", "setAirConditionerMode", "cool")))
                .desiredState()).containsEntry("mode", "COOL");
        assertThat(translator.translate("d", request(cmd("airConditionerMode", "setAirConditionerMode", "wind")))
                .desiredState()).containsEntry("mode", "FAN");
    }

    @Test
    @DisplayName("thermostatCoolingSetpoint -> temperature (정수 보존)")
    void temperature() {
        assertThat(translator.translate("d", request(cmd("thermostatCoolingSetpoint", "setCoolingSetpoint", 22)))
                .desiredState()).containsEntry("temperature", 22);
        assertThat(translator.translate("d", request(cmd("thermostatCoolingSetpoint", "setCoolingSetpoint", 22.5)))
                .desiredState()).containsEntry("temperature", 22.5);
    }

    @Test
    @DisplayName("여러 capability를 한 번에 -> 모두 desiredState에 반영")
    void 복합명령() {
        ProviderCommand result = translator.translate("d", request(
                cmd("switch", "on"),
                cmd("airConditionerMode", "setAirConditionerMode", "cool"),
                cmd("thermostatCoolingSetpoint", "setCoolingSetpoint", 20)));

        assertThat(result.desiredState())
                .containsEntry("power", "ON").containsEntry("mode", "COOL").containsEntry("temperature", 20);
    }

    @Test
    @DisplayName("commands가 비어 있으면 BadRequest")
    void 빈commands() {
        assertThatThrownBy(() -> translator.translate("d", new SmartThingsCommandRequest(List.of())))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("지원하지 않는 capability / 잘못된 값이면 BadRequest")
    void 잘못된요청() {
        assertThatThrownBy(() -> translator.translate("d", request(cmd("fanOscillationMode", "x", "all"))))
                .isInstanceOf(SimulatorException.BadRequest.class);
        assertThatThrownBy(() -> translator.translate("d", request(cmd("switch", "explode"))))
                .isInstanceOf(SimulatorException.BadRequest.class);
        assertThatThrownBy(() -> translator.translate("d", request(cmd("airConditionerMode", "x", "hypercool"))))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("airPurifierFanMode -> mode (sleep -> SLEEP)")
    void 공청기_mode() {
        assertThat(translator.translate("d", request(cmd("airPurifierFanMode", "setAirPurifierFanMode", "sleep")))
                .desiredState()).containsEntry("mode", "SLEEP");
    }

    @Test
    @DisplayName("fanSpeed(정수) -> mode (2 -> MID), 범위 밖이면 BadRequest")
    void 환풍기_mode() {
        assertThat(translator.translate("d", request(cmd("fanSpeed", "setFanSpeed", 2)))
                .desiredState()).containsEntry("mode", "MID");
        assertThatThrownBy(() -> translator.translate("d", request(cmd("fanSpeed", "setFanSpeed", 9))))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }
}
