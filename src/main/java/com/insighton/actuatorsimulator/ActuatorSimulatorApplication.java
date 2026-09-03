package com.insighton.actuatorsimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// SmartThings / LG ThinQ 호환 API를 흉내내는 독립 mock 서버 (상태 없음, CORE와 코드 공유 없음, 8090)
@SpringBootApplication
public class ActuatorSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActuatorSimulatorApplication.class, args);
    }
}
