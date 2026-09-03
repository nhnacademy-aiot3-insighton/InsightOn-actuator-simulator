package com.insighton.actuatorsimulator.smartthings;

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

@WebMvcTest(SmartThingsController.class)
@Import({SmartThingsRequestTranslator.class, SmartThingsResponseAssembler.class})
class SmartThingsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProviderTokenValidator tokenValidator;

    private static final String CMD_URL = "/smartthings/v1/devices/st-aircon-001/commands";
    private static final String SWITCH_ON = """
            {"commands":[{"component":"main","capability":"switch","command":"on","arguments":[]}]}""";

    @Test
    @DisplayName("정상 명령 - ACCEPTED results 반환 (상태 저장 없이 echo)")
    void commands_성공() throws Exception {
        willDoNothing().given(tokenValidator).validate(any());

        mockMvc.perform(post(CMD_URL)
                        .header("Authorization", "Bearer local-sim-token")
                        .contentType(MediaType.APPLICATION_JSON).content(SWITCH_ON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.results[0].id").isNotEmpty());
    }

    @Test
    @DisplayName("카탈로그가 없으므로 아무 deviceId나 수락한다 (UUID 형태 포함)")
    void commands_임의deviceId() throws Exception {
        willDoNothing().given(tokenValidator).validate(any());

        mockMvc.perform(post("/smartthings/v1/devices/4e0e6a1c-1a2b-3c4d-5e6f-7a8b9c0d1e2f/commands")
                        .header("Authorization", "Bearer local-sim-token")
                        .contentType(MediaType.APPLICATION_JSON).content(SWITCH_ON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("잘못된 토큰 - 401")
    void commands_잘못된토큰() throws Exception {
        willThrow(new SimulatorException.Unauthorized()).given(tokenValidator).validate(any());

        mockMvc.perform(post(CMD_URL)
                        .header("Authorization", "Bearer WRONG")
                        .contentType(MediaType.APPLICATION_JSON).content(SWITCH_ON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("지원하지 않는 capability - 400")
    void commands_미지원capability() throws Exception {
        willDoNothing().given(tokenValidator).validate(any());

        mockMvc.perform(post(CMD_URL)
                        .header("Authorization", "Bearer local-sim-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commands":[{"component":"main","capability":"nope","command":"x","arguments":[]}]}"""))
                .andExpect(status().isBadRequest());
    }
}
