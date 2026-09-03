package com.insighton.actuatorsimulator.smartthings.dto;

import java.util.List;

/** SmartThings "Execute commands" 응답: { "results": [ { "id": "&lt;uuid&gt;", "status": "ACCEPTED" } ] } */
public record SmartThingsCommandResponse(List<Result> results) {

    public record Result(String id, String status) {
    }
}
