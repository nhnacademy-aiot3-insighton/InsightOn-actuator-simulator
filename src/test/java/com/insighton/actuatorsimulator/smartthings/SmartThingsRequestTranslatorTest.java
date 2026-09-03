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
    @DisplayName("switch 명령은 command 문자열(on/off)을 그대로 담는다")
    void power() {
        ProviderCommand on = translator.translate("d", request(cmd("switch", "on")));
        assertThat(on.desiredState()).containsEntry("switch", "on");
        assertThat(on.providerLabel()).isEqualTo("SMART_THINGS");
        assertThat(on.deviceId()).isEqualTo("d");

        assertThat(translator.translate("d", request(cmd("switch", "off"))).desiredState())
                .containsEntry("switch", "off");
    }

    @Test
    @DisplayName("switch 외 capability는 arguments[0]을 값 변환 없이 그대로 담는다")
    void 값_그대로_보존() {
        ProviderCommand result = translator.translate("d", request(
                cmd("airConditionerMode", "setAirConditionerMode", "cool"),
                cmd("fanOscillationMode", "setFanOscillationMode", "fixed"),
                cmd("airPurifierFanMode", "setAirPurifierFanMode", "sleep"),
                cmd("fanSpeed", "setFanSpeed", 2),
                cmd("thermostatCoolingSetpoint", "setCoolingSetpoint", 22)));

        assertThat(result.desiredState())
                .containsEntry("airConditionerMode", "cool")
                .containsEntry("fanOscillationMode", "fixed")
                .containsEntry("airPurifierFanMode", "sleep")
                .containsEntry("fanSpeed", 2)
                .containsEntry("thermostatCoolingSetpoint", 22);
    }

    @Test
    @DisplayName("CORE 어휘로 되돌리지 않는다 — 알 수 없는 값이어도 구조만 맞으면 통과")
    void 값검증_안함() {
        assertThat(translator.translate("d", request(cmd("airConditionerMode", "x", "hypercool")))
                .desiredState()).containsEntry("airConditionerMode", "hypercool");
        assertThat(translator.translate("d", request(cmd("fanSpeed", "setFanSpeed", 9)))
                .desiredState()).containsEntry("fanSpeed", 9);
    }

    @Test
    @DisplayName("commands가 비어 있으면 BadRequest")
    void 빈commands() {
        assertThatThrownBy(() -> translator.translate("d", new SmartThingsCommandRequest(List.of())))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("이 mock이 모르는 capability면 BadRequest (실제 API도 동일)")
    void 미지원capability() {
        assertThatThrownBy(() -> translator.translate("d", request(cmd("colorControl", "setHue", "50"))))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }

    @Test
    @DisplayName("switch 외 capability에 arguments가 없으면 BadRequest")
    void arguments_누락() {
        assertThatThrownBy(() -> translator.translate("d", request(cmd("airConditionerMode", "setAirConditionerMode"))))
                .isInstanceOf(SimulatorException.BadRequest.class);
    }
}
