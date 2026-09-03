package com.insighton.actuatorsimulator.smartthings;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SmartThingsResponseAssemblerTest {

    private final SmartThingsResponseAssembler assembler = new SmartThingsResponseAssembler();

    @Test
    @DisplayName("commandResponse - 반영된 키 수만큼 ACCEPTED result")
    void commandResponse() {
        var response = assembler.commandResponse(Set.of("power", "mode"));
        assertThat(response.results()).hasSize(2)
                .allMatch(r -> r.status().equals("ACCEPTED") && r.id() != null);
    }

    @Test
    @DisplayName("commandResponse - 빈 키 집합이면 result도 비어 있음")
    void commandResponse_빈키() {
        var response = assembler.commandResponse(Set.of());
        assertThat(response.results()).isEmpty();
    }
}
