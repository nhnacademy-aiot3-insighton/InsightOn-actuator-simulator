package com.insighton.actuatorsimulator.lg;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.insighton.actuatorsimulator.protocol.ProviderTokenValidator;
import com.insighton.actuatorsimulator.protocol.SimulatorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LgThinQController.class)
@Import(LgThinQRequestTranslator.class)
class LgThinQControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProviderTokenValidator tokenValidator;

    private static final String CONTROL_URL = "/lg/devices/lg-aircon-001/control";
    private static final String POWER_ON = """
            {"operation":{"airConOperationMode":"POWER_ON"}}""";

    @Test
    @DisplayName("정상 제어 - messageId 반환 (상태 저장 없이 수락)")
    void control_성공() throws Exception {
        willDoNothing().given(tokenValidator).validate(any());

        mockMvc.perform(post(CONTROL_URL)
                        .header("Authorization", "Bearer local-sim-token")
                        .contentType(MediaType.APPLICATION_JSON).content(POWER_ON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").isNotEmpty());
    }

    @Test
    @DisplayName("카탈로그가 없으므로 아무 deviceId나 수락한다")
    void control_임의deviceId() throws Exception {
        willDoNothing().given(tokenValidator).validate(any());

        mockMvc.perform(post("/lg/devices/2f1e0d9c-aaaa-bbbb-cccc-ddddeeeeffff/control")
                        .header("Authorization", "Bearer local-sim-token")
                        .contentType(MediaType.APPLICATION_JSON).content(POWER_ON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").isNotEmpty());
    }

    @Test
    @DisplayName("잘못된 토큰 - 401")
    void control_잘못된토큰() throws Exception {
        willThrow(new SimulatorException.Unauthorized()).given(tokenValidator).validate(any());

        mockMvc.perform(post(CONTROL_URL)
                        .header("Authorization", "Bearer WRONG")
                        .contentType(MediaType.APPLICATION_JSON).content(POWER_ON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("변경할 property가 없으면 400")
    void control_빈요청() throws Exception {
        willDoNothing().given(tokenValidator).validate(any());

        mockMvc.perform(post(CONTROL_URL)
                        .header("Authorization", "Bearer local-sim-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
