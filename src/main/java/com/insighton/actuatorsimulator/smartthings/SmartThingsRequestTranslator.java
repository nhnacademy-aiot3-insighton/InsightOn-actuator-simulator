package com.insighton.actuatorsimulator.smartthings;

import com.insighton.actuatorsimulator.protocol.ProviderCommand;
import com.insighton.actuatorsimulator.protocol.SimulatorException;
import com.insighton.actuatorsimulator.smartthings.dto.SmartThingsCommandRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** SmartThings capability 명령 → 공급자 독립 ProviderCommand (CORE Assembler의 역방향). */
@Component
public class SmartThingsRequestTranslator {

    private static final String PROVIDER_LABEL = "SMART_THINGS";

    private static final Map<String, String> AC_MODE_TO_CORE = Map.of(
            "cool", "COOL", "dry", "DRY", "wind", "FAN", "fanonly", "FAN", "auto", "AUTO");
    private static final Map<String, String> PURIFIER_MODE_TO_CORE = Map.of(
            "auto", "AUTO", "sleep", "SLEEP", "turbo", "TURBO");
    private static final Map<Integer, String> FAN_SPEED_TO_CORE = Map.of(
            1, "LOW", 2, "MID", 3, "HIGH");

    public ProviderCommand translate(String deviceId, SmartThingsCommandRequest request) {
        if (request == null || request.commands() == null || request.commands().isEmpty()) {
            throw new SimulatorException.BadRequest("commands가 비어 있습니다");
        }

        Map<String, Object> desiredState = new LinkedHashMap<>();
        for (SmartThingsCommandRequest.Command command : request.commands()) {
            String capability = command.capability() == null ? "" : command.capability();
            switch (capability) {
                case "switch" -> desiredState.put("power", toPower(command.command()));
                case "airConditionerMode" -> desiredState.put("mode", fromTable(AC_MODE_TO_CORE, lowerArg(command), capability));
                case "airPurifierFanMode" -> desiredState.put("mode", fromTable(PURIFIER_MODE_TO_CORE, lowerArg(command), capability));
                case "fanSpeed" -> desiredState.put("mode", fromFanSpeed(firstArg(command)));
                case "thermostatCoolingSetpoint" -> desiredState.put("temperature", toTemperature(firstArg(command)));
                default -> throw new SimulatorException.BadRequest("지원하지 않는 capability: " + capability);
            }
        }
        return new ProviderCommand(deviceId, desiredState, PROVIDER_LABEL);
    }

    private String toPower(String stCommand) {
        if ("on".equalsIgnoreCase(stCommand)) {
            return "ON";
        }
        if ("off".equalsIgnoreCase(stCommand)) {
            return "OFF";
        }
        throw new SimulatorException.BadRequest("지원하지 않는 switch 명령: " + stCommand);
    }

    private String fromTable(Map<String, String> table, String value, String capability) {
        String core = table.get(value);
        if (core == null) {
            throw new SimulatorException.BadRequest("지원하지 않는 " + capability + " 값: " + value);
        }
        return core;
    }

    private String fromFanSpeed(Object arg) {
        int speed;
        try {
            speed = (int) Math.round(Double.parseDouble(String.valueOf(arg)));
        } catch (NumberFormatException e) {
            throw new SimulatorException.BadRequest("fanSpeed 인자는 숫자여야 합니다: " + arg);
        }
        String core = FAN_SPEED_TO_CORE.get(speed);
        if (core == null) {
            throw new SimulatorException.BadRequest("지원하지 않는 fanSpeed 값: " + speed);
        }
        return core;
    }

    private Object toTemperature(Object arg) {
        try {
            double d = Double.parseDouble(String.valueOf(arg));
            if (d == Math.rint(d) && !Double.isInfinite(d)) {
                return (int) d;
            }
            return d;
        } catch (NumberFormatException e) {
            throw new SimulatorException.BadRequest("setCoolingSetpoint 인자는 숫자여야 합니다: " + arg);
        }
    }

    private String lowerArg(SmartThingsCommandRequest.Command command) {
        return String.valueOf(firstArg(command)).toLowerCase();
    }

    private Object firstArg(SmartThingsCommandRequest.Command command) {
        List<Object> arguments = command.arguments();
        if (arguments == null || arguments.isEmpty()) {
            throw new SimulatorException.BadRequest(command.capability() + " 명령에 arguments가 없습니다");
        }
        return arguments.get(0);
    }
}
