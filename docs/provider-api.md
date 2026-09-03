# 공급자 API 계약 — SmartThings / LG ThinQ

이 시뮬레이터가 흉내내는 **실제 공급자 API**의 URL · 헤더 · 요청/응답 형식 · 상수 정리.
실연동 전환 = CORE의 `base-url` + 토큰/키만 교체 (시뮬레이터 코드 변경 0).

> 시뮬레이터는 **상태를 저장하지 않고** 값을 CORE 어휘로 되돌리지도 않는다 (dumb parser).
> 받은 요청의 **구조만 검증**하고 (capability/resource가 아는 목록에 있나) 공급자 형식의 성공 응답을 돌려준다.
> 중립값 ↔ wire 값 매핑 로직은 CORE의 `SmartThingsVocab` / `LgThinQVocab` enum에 있고, 아래 §4에 사본을 둔다.

---

## 1. 엔드포인트

| | 실제 | 로컬 (이 시뮬레이터) |
|---|---|---|
| **SmartThings 제어** | `POST https://api.smartthings.com/v1/devices/{deviceId}/commands` | `POST http://localhost:8090/smartthings/v1/devices/{deviceId}/commands` |
| **LG ThinQ 제어** | `POST https://api-kic.lgthinq.com/devices/{deviceId}/control`<br>(KR, US=`api-aic` / EU=`api-eic`) | `POST http://localhost:8090/lg/devices/{deviceId}/control` |

- 컨트롤러 경로(`/smartthings/v1/...`, `/lg/devices/...`)는 실제 API path와 **동일**. `base-url`만 다르다.
- `{deviceId}` 는 아무 문자열이나 받는다 (실제 UUID여도, CORE가 만든 `st-aircon-xxxxxxxx` 여도).
- 조회 계열(`GET /v1/devices`, `/devices/{id}/status`, `/profile` 등)은 현재 미구현 — 제어 경로만.

---

## 2. 헤더

| 헤더 | SmartThings | LG ThinQ | 시뮬레이터 처리 |
|---|---|---|---|
| `Authorization: Bearer <token>` | ✅ | ✅ | **검증** — `simulator.provider-token` 불일치 시 401 |
| `Content-Type: application/json` | ✅ | ✅ | (스프링 기본) |
| `x-api-key` | — | ✅ 공개 고정키 `v6GFvkweNo7DK7yD3ylIZ9w52aKBU0eJ7wLXkSR3` (SDK 내장) | 존재 여부만 로그 |
| `x-client-id` | — | ✅ 앱 식별 UUID4 (고정) | 존재 여부만 로그 |
| `x-message-id` | — | ✅ 요청마다 `base64url(uuid)` 22자 (패딩 `==` 제거) | 존재 여부만 로그 |
| `x-country` | — | ✅ `KR` | 로그 |
| `x-service-phase` | — | ✅ `OP` | 무시 |
| `x-conditional-control: true` | — | ✅ 제어 요청만 | 무시 |

CORE 쪽 설정: SmartThings는 `Authorization` 하나. LG는 `x-api-key`·`x-client-id`·`x-country`·`x-service-phase` 를 RestClient `defaultHeader`, `x-message-id`·`x-conditional-control` 를 요청마다.

---

## 3. 요청 / 응답 형식

### SmartThings

요청:
```json
{ "commands": [
  { "component": "main", "capability": "switch",            "command": "on",                    "arguments": [] },
  { "component": "main", "capability": "airConditionerMode", "command": "setAirConditionerMode",  "arguments": ["cool"] },
  { "component": "main", "capability": "thermostatCoolingSetpoint", "command": "setCoolingSetpoint", "arguments": [25] }
]}
```
응답 (HTTP 200) — `capability` 개수만큼 `ACCEPTED`:
```json
{ "results": [
  { "id": "5b4...", "status": "ACCEPTED" },
  { "id": "9c1...", "status": "ACCEPTED" },
  { "id": "e2f...", "status": "ACCEPTED" }
]}
```

### LG ThinQ

요청 — 변경하는 resource만, 나머지 생략:
```json
{
  "operation":     { "airConOperationMode": "POWER_ON" },
  "airConJobMode": { "currentJobMode": "COOL" },
  "temperature":   { "targetTemperature": 25, "unit": "C" }
}
```
응답 (HTTP 200):
```json
{ "messageId": "d1e2f3...", "timestamp": "2026-09-03T12:00:00Z", "response": {} }
```

### 오류 (공통) — 공급자 스타일 바디

```json
{ "error": { "code": "BAD_REQUEST", "message": "지원하지 않는 capability: colorControl" } }
```

| 상황 | HTTP | code |
|---|---|---|
| 아는 capability/resource 아님, JSON 형식 불량, property 객체 아님, `switch` 외 capability에 `arguments` 없음 | 400 | `BAD_REQUEST` |
| `Authorization` 헤더가 `Bearer <설정된 토큰>` 이 아님 | 401 | `UNAUTHORIZED` |

CORE는 이 오류를 받으면 `SmartThingsApiException` / `LgThinQApiException` → 사용자에게 **502**.

---

## 4. 상수 — 중립 명령 ↔ 공급자 wire 값

> 매핑 로직은 CORE (`SmartThingsVocab` / `LgThinQVocab`). 시뮬레이터는 wire 쪽 키(capability/resource)만 "아는 것"으로 인식하고 값은 검증하지 않는다.
> `[근사]` = 실제 장치 profile 확인 전 임시값. 실연동 시 `GET /devices/{id}/profile`(LG) · `/status`(ST)로 확정.

### 4-1. SmartThings — `KNOWN_CAPABILITIES`

`{ switch, airConditionerMode, airPurifierFanMode, fanSpeed, fanOscillationMode, thermostatCoolingSetpoint }`

| capability | command | arguments | 중립: 종류 / 명령 / 값 |
|---|---|---|---|
| `switch` | `on` / `off` | `[]` | 전 종류 / `power` / `ON`·`OFF` |
| `airConditionerMode` | `setAirConditionerMode` | `["cool"]` `["dry"]` `["wind"]` `["auto"]` | 에어컨 / `mode` / `COOL`·`DRY`·`FAN`·`AUTO` |
| `airPurifierFanMode` | `setAirPurifierFanMode` | `["auto"]` `["sleep"]` `["high"]` `[근사]` | 공기청정기 / `mode` / `AUTO`·`SLEEP`·`TURBO` |
| `fanSpeed` | `setFanSpeed` | `[1]` `[2]` `[3]` (정수) | 환풍기 / `mode` / `LOW`·`MID`·`HIGH` |
| `fanOscillationMode` | `setFanOscillationMode` | `["fixed"]` `["all"]` | 에어컨 / `windDirection` / `FIXED`·`SWING` |
| `thermostatCoolingSetpoint` | `setCoolingSetpoint` | `[<정수>]` | 에어컨 / `temperature` |

SmartThings 에어컨엔 `AIRCLEAN` 매핑 **없음** (LG 전용) → CORE 어댑터가 502.

### 4-2. LG ThinQ — `KNOWN_RESOURCES`

`{ operation, airConJobMode, airPurifierJobMode, airFlow, windDirection, temperature }`

| resource | property | value | 중립: 종류 / 명령 / 값 |
|---|---|---|---|
| `operation` | `airConOperationMode` | `POWER_ON` / `POWER_OFF` | 에어컨 / `power` |
| `operation` | `airPurifierOperationMode` | `POWER_ON` / `POWER_OFF` `[근사]` | 공기청정기 / `power` |
| `operation` | `airFanOperationMode` | `POWER_ON` / `POWER_OFF` `[근사]` | 환풍기 / `power` |
| `airConJobMode` | `currentJobMode` | `COOL` `AIR_DRY` `FAN` `AUTO` `AIR_CLEAN` | 에어컨 / `mode` / `COOL`·`DRY`·`FAN`·`AUTO`·`AIRCLEAN` |
| `airPurifierJobMode` | `currentJobMode` | `AUTO` `SLEEP` `CLEAN` `[근사]` | 공기청정기 / `mode` / `AUTO`·`SLEEP`·`TURBO` |
| `airFlow` | `windStrength` | `LOW` `MID` `HIGH` | 환풍기 / `mode` |
| `windDirection` | `rotateUpDown` | `false` / `true` (JSON boolean) | 에어컨 / `windDirection` / `FIXED`·`SWING` |
| `temperature` | `targetTemperature` (+ `unit`: `"C"`) | `<정수>` | 에어컨 / `temperature` |

`AIR_CLEAN` = LG 에어컨 전용 (공기청정 모드).

### 4-3. 중립 어휘 요약 (CORE·Front 공용)

| 명령 키 | 값 | 종류 |
|---|---|---|
| `power` | `ON` / `OFF` | 전 종류 |
| `mode` | 에어컨 `COOL`·`DRY`·`FAN`·`AUTO`(+LG `AIRCLEAN`) / 공기청정기 `AUTO`·`SLEEP`·`TURBO` / 환풍기 `LOW`·`MID`·`HIGH` | 종류별 |
| `windDirection` | `FIXED` / `SWING` | 에어컨만 (수동 조작 전용) |
| `temperature` | 정수 18~30 | 에어컨만 |

---

## 5. 코드 위치

| 역할 | 파일 |
|---|---|
| SmartThings endpoint | `src/main/java/.../smartthings/SmartThingsController.java` |
| SmartThings 구조 검증 (capability 화이트리스트) | `src/main/java/.../smartthings/SmartThingsRequestTranslator.java` |
| SmartThings 응답 조립 | `src/main/java/.../smartthings/SmartThingsResponseAssembler.java` |
| SmartThings 요청/응답 DTO | `src/main/java/.../smartthings/dto/` |
| LG endpoint | `src/main/java/.../lg/LgThinQController.java` |
| LG 구조 검증 (resource 화이트리스트) | `src/main/java/.../lg/LgThinQRequestTranslator.java` |
| LG 응답 DTO | `src/main/java/.../lg/dto/LgThinQControlResponse.java` |
| 토큰 검증 (`Bearer`) | `src/main/java/.../protocol/ProviderTokenValidator.java` |
| 오류 → 공급자 형식 바디 | `src/main/java/.../protocol/GlobalExceptionHandler.java` |
| 공급자 독립 명령 record | `src/main/java/.../protocol/ProviderCommand.java` |
| **(CORE) 중립 → wire 매핑** | `InsightOn-core/.../smartthings/SmartThingsVocab.java`, `.../lg/LgThinQVocab.java` |
| **(CORE) 전체 분기 스펙** | `InsightOn-core/docs/provider-contract.md` |

---

## 6. 실연동 전환 체크리스트

1. CORE `application-{prod}.properties`:
   - `actuator.smartthings.base-url=https://api.smartthings.com`
   - `actuator.lg-thinq.base-url=https://api-kic.lgthinq.com`
   - `actuator.smartthings.token` = SmartThings PAT
   - `actuator.lg-thinq.token` = LG ThinQ PAT, `actuator.lg-thinq.client-id` = 우리 앱 UUID
2. 시뮬레이터 종료. **코드 변경 없음** (계약이 동일).
3. `[근사]` 값 (`airPurifierOperationMode`, `TURBO`→`high`/`CLEAN` 등) 실제 장치 profile로 확정 → CORE `SmartThingsVocab` / `LgThinQVocab` 만 수정.

---

## 7. 설정 / 실행

| 파일 | 내용 | 언제 |
|---|---|---|
| `application.properties` (base) | `simulator.provider-token` — **포트 미지정 → 8080** (Dockerfile 헬스체크가 `:8080`) | prod 컨테이너 |
| `application-dev.properties` | `server.port=8090` + DEBUG 로그 | **로컬 개발** |
| `application-prod.properties` | INFO 로그 | 배포 |

`simulator.provider-token` 은 CORE의 `actuator.smartthings.token` / `actuator.lg-thinq.token` 과 일치해야 함 (로컬 둘 다 `local-sim-token`).

**로컬 실행** — CORE가 `localhost:8090` 을 호출하므로 `dev` 프로파일로 띄운다:
- IntelliJ: Run/Debug Configurations → `ActuatorSimulatorApplication` → **Active profiles: `dev`**
- CLI: `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run`

DEBUG 로그로 요청 본문·헤더가 찍혀 CORE 매핑을 눈으로 확인할 수 있다.
