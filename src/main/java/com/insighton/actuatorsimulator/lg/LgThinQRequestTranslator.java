package com.insighton.actuatorsimulator.lg;

import com.insighton.actuatorsimulator.protocol.ProviderCommand;
import com.insighton.actuatorsimulator.protocol.SimulatorException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * LG ThinQ Connect control payload → 공급자 독립 ProviderCommand (CORE Assembler의 역방향).
 *
 * <p>실제 payload는 resource로 중첩된 property bag:
 * {@code { "operation": {"airConOperationMode":"POWER_ON"}, "airConJobMode": {"currentJobMode":"COOL"}, ... }}
 * 시뮬레이터는 상태를 저장하지 않으므로 형식 검증 + 응답 result 산정에만 쓴다.
 */
@Component
public class LgThinQRequestTranslator {

    private static final String PROVIDER_LABEL = "LG_THINQ";

    // 알고 있는 resource — 이 외의 키가 오면 400
    private static final Set<String> KNOWN_RESOURCES = Set.of(
            "operation", "airConJobMode", "airPurifierJobMode", "airFlow", "temperature");

    // LG currentJobMode → CORE 중립 mode (Assembler의 역방향)
    private static final Map<String, String> AIRCON_JOB_MODE_TO_CORE = Map.of(
            "COOL", "COOL", "AIR_DRY", "DRY", "FAN", "FAN", "AUTO", "AUTO");
    private static final Map<String, String> PURIFIER_JOB_MODE_TO_CORE = Map.of(
            "AUTO", "AUTO", "SLEEP", "SLEEP", "CLEAN", "TURBO");
    private static final Map<String, String> WIND_STRENGTH_TO_CORE = Map.of(
            "LOW", "LOW", "MID", "MID", "HIGH", "HIGH");

    @SuppressWarnings("unchecked")
    public ProviderCommand translate(String deviceId, Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new SimulatorException.BadRequest("변경할 property가 없습니다");
        }
        for (String key : body.keySet()) {
            if (!KNOWN_RESOURCES.contains(key)) {
                throw new SimulatorException.BadRequest("지원하지 않는 resource: " + key);
            }
        }

        Map<String, Object> desiredState = new LinkedHashMap<>();

        Map<String, Object> operation = (Map<String, Object>) body.get("operation");
        if (operation != null) {
            // airConOperationMode / airPurifierOperationMode / airFanOperationMode - 아무 *OperationMode 키
            Object mode = operation.values().stream().findFirst().orElse(null);
            desiredState.put("power", toPower(mode));
        }
        Map<String, Object> airConJobMode = (Map<String, Object>) body.get("airConJobMode");
        if (airConJobMode != null) {
            desiredState.put("mode", fromTable(AIRCON_JOB_MODE_TO_CORE, airConJobMode.get("currentJobMode"), "airConJobMode"));
        }
        Map<String, Object> airPurifierJobMode = (Map<String, Object>) body.get("airPurifierJobMode");
        if (airPurifierJobMode != null) {
            desiredState.put("mode", fromTable(PURIFIER_JOB_MODE_TO_CORE, airPurifierJobMode.get("currentJobMode"), "airPurifierJobMode"));
        }
        Map<String, Object> airFlow = (Map<String, Object>) body.get("airFlow");
        if (airFlow != null) {
            desiredState.put("mode", fromTable(WIND_STRENGTH_TO_CORE, airFlow.get("windStrength"), "airFlow.windStrength"));
        }
        Map<String, Object> temperature = (Map<String, Object>) body.get("temperature");
        if (temperature != null) {
            Object target = temperature.get("targetTemperature");
            if (target == null) {
                throw new SimulatorException.BadRequest("targetTemperature가 없습니다");
            }
            desiredState.put("temperature", target);
        }

        if (desiredState.isEmpty()) {
            throw new SimulatorException.BadRequest("해석할 수 있는 property가 없습니다");
        }
        return new ProviderCommand(deviceId, desiredState, PROVIDER_LABEL);
    }

    private String toPower(Object operationMode) {
        String v = String.valueOf(operationMode);
        if ("POWER_ON".equalsIgnoreCase(v)) {
            return "ON";
        }
        if ("POWER_OFF".equalsIgnoreCase(v)) {
            return "OFF";
        }
        throw new SimulatorException.BadRequest("지원하지 않는 operation 값: " + operationMode);
    }

    private String fromTable(Map<String, String> table, Object value, String group) {
        String core = table.get(String.valueOf(value).toUpperCase());
        if (core == null) {
            throw new SimulatorException.BadRequest("지원하지 않는 " + group + " 값: " + value);
        }
        return core;
    }
}
