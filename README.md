# FactoryEye

> 한국 GM 부평공장 차체 공정 설비 통합 관제 시스템  
> Web SCADA 기반 실시간 설비 모니터링, 알람 관리, 예방보전 지표 분석, RAG+MCP 기반 알람 리포트 메일링 시스템

---

## 1. 프로젝트 소개

**FactoryEye**는 자동차 차체 용접 공정의 설비 상태를 실시간으로 수집·시각화하고, 알람 발생 시 담당자에게 유사 사례 기반 조치 방향을 제공하는 **설비 통합 관제 시스템**입니다.

자동차 차체 공정은 패널 투입 장치, 지그, 로봇, 점용접기, 실러, 컨베이어, 비전 검사기 등 여러 설비가 연동되는 공정입니다. 하나의 설비 이상이 전체 라인 정지로 이어질 수 있기 때문에, 본 프로젝트는 기존의 사후 대응 중심 설비 관리 방식에서 벗어나 **실시간 관제와 사전 이상 감지 기반 예방보전 체계**를 구축하는 것을 목표로 합니다.

---

## 2. 프로젝트 배경

자동차 제조 현장에서는 설비 노후화, 센서 이상, 로봇 오류, 프레스 고장, 누수 및 안전사고 등으로 인해 생산 차질이 발생할 수 있습니다.

특히 차체 공정은 다음과 같은 특징을 가집니다.

- 용접 로봇, 지그, 컨베이어, PLC, 센서 등이 반복적으로 연동됨
- 설비 간 의존성이 높아 단일 설비 이상이 전체 라인 정지로 확산될 수 있음
- 반복 작업 중심 공정이기 때문에 정상 패턴과 이상 패턴 비교가 용이함
- 설비 데이터 기반의 이상 감지 및 예방보전 적용 효과가 큼

따라서 본 프로젝트에서는 설비 데이터를 실시간으로 수집하고, 웹 기반 SCADA 화면에서 상태를 확인하며, 알람 발생 시 과거 유사 사례를 기반으로 대응 방안을 제공하는 시스템을 구현했습니다.

---

## 3. 주요 기능

### 3.1 종합 대시보드
<img width="1919" height="970" alt="image (1)" src="https://github.com/user-attachments/assets/f42c5a9f-6d38-40ef-b20d-147fd97190e0" />

- 전체 설비 상태 요약
- 라인별 가동률, 성능률, 품질률, OEE 시각화
- 설비별 잔존 수명 하위 5개 표시
- 설비 가동 상태 및 상세 현황 확인
- 공장 전체 상태를 한 화면에서 파악 가능

### 3.2 설비 현황
<img width="1919" height="1079" alt="image (2)" src="https://github.com/user-attachments/assets/5cec3d7e-6ecd-4e73-a022-1d546d716cf5" />

- 차체 공정 2D 레이아웃 시각화
- 라인/구역별 설비 위치 확인
- 설비별 상태를 색상으로 표시
- 특정 설비 클릭 시 상세 정보 확인
- 컨베이어 인버터 상태 조회 및 가동/정지 제어

### 3.3 설비 상세 모니터링
<img width="1919" height="857" alt="image (3)" src="https://github.com/user-attachments/assets/0ab0d07c-0c09-440e-a50b-9e3aa26ab823" />

- 설비별 주요 센서 데이터 실시간 확인
- MQTT 기반 1초 단위 데이터 업데이트
- MTTF, MTBF 지표 시각화
- 설비 이벤트 로그 및 상태 로그 확인
- 설비별 이상 상태 추적

### 3.4 알람 관리
<img width="1919" height="1079" alt="image (4)" src="https://github.com/user-attachments/assets/959a39c5-40a8-46e4-8ad6-039c79b1d0be" />

- 담당 설비별 알람 목록 조회
- 알람 발생 추이 일별/주별/월별 시각화
- 알람 상세 정보 확인
- 알람 조치 메모 작성
- 조치 상태 변경
- 미확인 알람 집계

### 3.5 수명 관리
<img width="1919" height="1079" alt="image (5)" src="https://github.com/user-attachments/assets/8e5fe646-2c6e-44be-9f94-deec8719b1b5" />

- 설비별 잔존 수명 현황 조회
- 잔존 수명 낮은 순/높은 순 정렬
- 라인별/존별 필터링
- 설비 상세 정보 및 최근 알람 확인

### 3.6 사용자 관리
<img width="1919" height="1079" alt="image (5)" src="https://github.com/user-attachments/assets/02a33a18-f6a7-4f5c-9a61-66da6d2843ef" />

- 사용자 목록 조회
- 사용자 상세 정보 확인
- 담당 설비 할당
- 사용자 정보 수정
- 사용자 삭제

### 3.7 RAG + MCP 기반 메일 알람 시스템

- 알람 발생 시 설비 정보와 이상 데이터를 기반으로 리포트 생성
- 과거 알람 및 설비 데이터를 Vector DB에 저장
- 현재 알람 데이터와 유사한 과거 사례 검색
- LLM을 활용해 원인 및 예상 영향 분석
- Gmail SMTP 서버를 통해 담당자에게 메일 발송

---

## 4. 시스템 아키텍처
<img width="1672" height="941" alt="image" src="https://github.com/user-attachments/assets/ede47aaa-722d-4190-919e-8f15b2b28172" />


## 5. 기술 스택

### Frontend

- Vue.js
- JavaScript
- HTML/CSS
- WebSocket
- MQTT Client
- ECharts

### Backend

- Spring Boot
- Java
- MyBatis
- REST API
- WebSocket

### Database

- MySQL
- Aiven Managed MySQL

### DAS / Data Pipeline

- Node-RED
- MQTT
- Modbus
- PLC/인버터 연동
- 설비별 가상 데이터 생성

### AI / Automation

- RAG
- Vector DB
- MCP
- LLM 기반 알람 분석
- Gmail SMTP 메일 발송

### Development Tools

- VS Code
- IntelliJ
- GitHub
- Notion
- Figma

---

## 6. 관제 대상 설비

| 구분 | 설비 | 주요 데이터 |
|---|---|---|
| 1 | 패널 투입 장치 | 모터 전류, 진공 압력, 위치 오차 |
| 2 | 차체 지그 | 클램프 압력, 잠금 여부, 클램프 위치 |
| 3 | 산업용 로봇 | 로봇 위치, 토크, 모터 온도 |
| 4 | 점용접기 | 2차 전류, 용접 가압력, 전극 마모, 전압, 팁 온도 |
| 5 | 실러 도포 장치 | 토출 압력, 실러 온도, 도포량 |
| 6 | 컨베이어 | 모터 전류, 모터 온도, 이동 속도 |
| 7 | 비전 검사기 | 검사 결과, 불량 여부 |

---

## 7. 데이터 흐름

### 7.1 실시간 설비 데이터

```text
Node-RED → MQTT → Frontend Web SCADA
```

- Node-RED에서 설비별 데이터를 생성 또는 수집
- MQTT Topic으로 실시간 데이터 발행
- 프론트엔드는 MQTT를 구독하여 설비 상태를 실시간 반영

**예시 Topic**

```text
factory/equipment/{equipment_id}/realtime
```

---

### 7.2 설비 로그 저장

```text
Node-RED → MySQL
```

- 설비별 로그는 10초마다 DB에 저장
- 설비 상태 변경 시 상태 로그 저장
- 알람 발생 시 알람 로그 저장

---

### 7.3 KPI 및 지표 집계

```text
Node-RED → Backend API → DB → Frontend
```

- 라인 생산 완료 후 OEE, MTTR 등 지표 집계 요청
- 백엔드는 집계 결과를 DB에 저장
- 프론트엔드는 API 또는 WebSocket을 통해 최신 지표 반영

---

### 7.4 알람 리포트 메일링

```text
Alarm 발생 → DB 저장 → RAG 유사 사례 검색 → LLM 분석 → MCP/Gmail SMTP 메일 발송
```

- Health Score가 기준값 미만일 경우 메일 발송
- 현재 알람 데이터와 유사한 과거 사례 검색
- 원인, 예상 영향, 조치 방향을 포함한 메일 생성

---

## 8. 주요 페이지

### 8.1 로그인

- 이메일 기반 로그인
- 잘못된 아이디/비밀번호 입력 시 예외 처리

**API**

```http
POST /api/auth/login
```

---

### 8.2 회원가입

- 이름, 이메일, 비밀번호 입력
- 필수값 검증
- 이메일 형식 검증
- 비밀번호 확인 검증

**API**

```http
POST /api/auth/signup
```

---

### 8.3 메인 대시보드

**주요 기능**

- 전체 설비 상태 요약
- OEE 지표 표시
- 라인별 OEE 조회
- 설비별 잔존 수명 표시
- 실시간 설비 현황 표시

**API**

```http
GET /api/dashboard/oee
GET /api/dashboard/lines/{lineNo}/oee
GET /api/equipments/{equipmentId}/remaining-life
```

**MQTT**

```text
factory/equipment/{equipment_id}/realtime
```

---

### 8.4 설비 현황

**주요 기능**

- 공정 2D 레이아웃 조회
- 설비별 상태 확인
- 설비 상세 정보 확인
- 최근 알람 확인
- 인버터 제어

**API**

```http
GET /api/equipments
GET /api/equipments/{id}/control/status
POST /api/equipments/{id}/control/on
POST /api/equipments/{id}/control/off
```

**MQTT**

```text
factory/equipment/{equipment_id}/realtime
factory/equipment/{equipment_id}/alarm
```

---

### 8.5 설비 상세

**주요 기능**

- 설비 주요 데이터 실시간 그래프
- MTTF / MTBF 확인
- 설비 이벤트 로그 확인

**MQTT**

```text
factory/equipment/{equipment_id}/realtime
factory/equipment/{equipment_id}/mt
```

---

### 8.6 알람 관리

**주요 기능**

- 담당 설비별 알람 조회
- 미확인 알람 집계
- 알람 발생 추이 시각화
- 알람 상세 정보 확인
- 조치 메모 작성 및 상태 변경

**API**

```http
GET /api/alarms/log
GET /api/alarms/{equipmentId}/statistics
GET /api/alarms/detail/{equipmentId}
PATCH /api/alarms/memo/{alarm_id}
GET /api/users/{userId}/equipments
```

---

### 8.7 수명 관리

**주요 기능**

- 설비별 잔존 수명 조회
- 잔존 수명 기준 정렬
- 라인/존별 필터링
- 최근 알람 확인

**API**

```http
GET /api/equipments/{equipmentId}/remaining-life
```

**MQTT**

```text
factory/equipment/{equipment_id}/realtime
factory/equipment/{equipment_id}/alarm
```

---

### 8.8 사용자 관리

**주요 기능**

- 사용자 목록 조회
- 사용자 상세 조회
- 담당 설비 할당
- 사용자 정보 수정
- 사용자 삭제

**API**

```http
GET /api/users
GET /api/users/{userId}/equipments
POST /api/users/{userId}/equipments/batch
PUT /api/users/{userId}
DELETE /api/users/{userId}
```

---

## 9. Database Schema

### 9.1 기준 정보 및 사용자 관리

- `equipment`
- `user_equipment`
- `users`

### 9.2 설비 로그

- `panel_feeder_log`
- `sealer_log`
- `body_jig_log`
- `robot_log`
- `spot_welder_log`
- `conveyor_log`
- `vision_inspection_log`

### 9.3 알람 및 정비 이력

- `alarm_log`
- `mail_report`
- `maintenance_log`

### 9.4 라인 및 설비 상태 요약

- `line_summary`
- `equipment_status_log`

---

## 10. MQTT Topic

| Topic | 설명 |
|---|---|
| `factory/equipment/{equipment_id}/realtime` | 설비별 실시간 센서 데이터 |
| `factory/equipment/{equipment_id}/alarm` | 설비별 알람 데이터 |
| `factory/equipment/{equipment_id}/mt` | MTTF, MTBF, MTTR 지표 데이터 |

---

## 11. 실행 방법

### 11.1 Backend 실행

```bash
cd backend
./gradlew bootRun
```

또는

```bash
cd backend
./mvnw spring-boot:run
```

**`application.properties` 예시**

```properties
spring.datasource.url=jdbc:mysql://{DB_HOST}:{DB_PORT}/{DB_NAME}
spring.datasource.username={DB_USERNAME}
spring.datasource.password={DB_PASSWORD}

spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

server.port=8080
```

---

### 11.2 Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

**`.env` 예시**

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_MQTT_BROKER_URL=ws://localhost:8083/mqtt
```

---

### 11.3 Node-RED 실행

```bash
node-red
```

Node-RED에서는 다음 역할을 수행합니다.

- 설비별 가상 데이터 생성
- MQTT 메시지 발행
- DB 로그 저장
- 컨베이어 인버터 Modbus 데이터 수집
- 백엔드 요청 기반 인버터 제어

---

## 12. 트러블 슈팅

### 12.1 데이터 수집 구조 복잡성

초기에는 Node-RED에서 설비별 노드를 각각 분산하여 생성했기 때문에 전체 흐름 관리가 어려웠습니다.

**해결 방법**

- Switch 노드를 활용하여 설비별 흐름을 통합
- 설비별 로그 함수로 분기 처리
- 데이터 생성 및 저장 구조를 단순화

---

### 12.2 실시간성 문제

1초마다 발생하는 데이터를 모두 DB에 저장한 뒤 API로 조회하면 화면 반영에 지연이 발생했습니다.

**해결 방법**

- 실시간 화면 갱신은 MQTT로 처리
- DB는 일정 주기 로그 저장 및 이력 분석 용도로 사용
- 실시간성과 이력 관리 역할을 분리

---

### 12.3 KPI 집계 문제

OEE, MTBF, MTTF 등 지표 계산을 위해 설비 로그와 상태 로그를 통합적으로 관리할 필요가 있었습니다.

**해결 방법**

- 지표 계산용 테이블 추가
- 설비 상태 변경 이벤트 저장
- 백엔드에서 KPI 집계 API 구현

---

## 13. 기대 효과

- 설비 이상 징후 조기 파악을 통한 라인 정지 사전 대응
- 가동률, 성능률, 품질률 기반 생산 효율 분석
- MTTF, MTBF 기반 점검 우선순위 도출
- 알람 이력과 작업자 메모 기반 반복 고장 원인 추적
- 이상 발생 시 담당자에게 유사 사례 기반 조치 가이드 제공
- 설비 상태, 품질, 알람, 담당자 정보를 하나의 시스템에서 통합 관리

---

## 14. 고도화 방안

### 14.1 관제 범위 확장

- 차체 공정 내 모든 설비 데이터 수집
- 자동차 제조 공정 전반으로 모니터링 범위 확장
- 공정 간 영향 관계 분석 기능 추가

### 14.2 실제 현장 데이터 연동

- Node-RED 기반 가상 데이터에서 실제 PLC 데이터 연동으로 전환
- 점용접기, 로봇, 지그, 실러, 비전 검사기 등 주요 설비의 실제 운전 데이터 반영
- 실제 센서 데이터 기반 이상 판단 로직 고도화

### 14.3 알림 채널 확장

- 메일 중심 알림에서 카카오톡, Slack, Teams 등 다양한 채널로 확장
- 담당자별 알림 우선순위 설정
- 알람 심각도별 알림 정책 분리

### 14.4 예지보전 모델 고도화

- 단순 임계값 기반 이상 감지를 넘어 시계열 기반 이상 탐지 모델 적용
- 설비별 고장 패턴 학습
- 잔존 수명 예측 정확도 개선

---

## 15. 팀 구성

| 이름 | 역할 | 담당 업무 |
|---|---|---|
| 이온유 | PM / Frontend | Node-RED 데이터 생성, Web SCADA 제작, 설비 상세 및 알람 관리 페이지 제작 |
| 김수민 | Backend | Node-RED 데이터 생성, MQTT 연동, 인버터 활용, 설비 현황 및 대시보드 API 제작 |
| 김민재 | Frontend | Node-RED 데이터 생성, 대시보드, 사용자 관리, 설비 현황 제작, 사용자 관리 파이프라인 구현 |
| 표성중 | Backend | Node-RED 데이터 생성, 상세 분석 메일 시스템 제작, 알람 관리 API 제작 |

---

## 16. 개발 일정

| 기간 | 주요 작업 |
|---|---|
| 5.12 | 웹 디자인, ERD, 데이터 정의, 프론트엔드/백엔드 기본 틀 제작 |
| 5.13 ~ 5.14 | 데이터 수집 및 가공, 데이터베이스 설계, API 정의 |
| 5.15 ~ 5.18 | 백엔드 및 프론트엔드 개발 |
| 5.18 ~ 5.20 | 백엔드-프론트엔드 연동 |
| 5.20 | QA, 배포, 발표 준비 |

---

## 17. AI 활용

본 프로젝트에서는 생성형 AI를 다음 영역에 활용했습니다.

- API 명세서 작성
- Spring Boot CRUD 코드 초안 작성
- SQL 쿼리 작성
- Vue.js 컴포넌트 코드 초안 작성
- Node-RED 설비 데이터 생성 로직 작성
- 설비별 정상/이상 데이터 범위 설계
- RAG 및 MCP 기반 메일 자동화 구조 설계
- 화면 정의서 및 발표자료 개선

---

## 18. 프로젝트 성과

- Web SCADA 기반 설비 통합 관제 화면 구현
- Node-RED 기반 설비 데이터 생성 및 MQTT 실시간 연동 구현
- MySQL 기반 설비 로그, 알람 로그, 사용자 정보 관리 구조 설계
- 컨베이어 인버터 Modbus 연동 및 제어 기능 구현
- OEE, MTTF, MTBF 등 설비 관리 지표 시각화
- RAG + MCP 기반 알람 분석 리포트 메일링 시스템 구현
- 설비 상태, 알람, 수명, 담당자 정보를 통합 관리하는 시스템 구축

---

## 19. 프로젝트 한계

- 대부분의 설비 데이터는 실제 PLC가 아닌 Node-RED 기반 가상 데이터로 생성
- 점용접기 등 일부 설비의 실제 PLC 데이터 연동은 미완료
- 실제 제조 현장 적용을 위해서는 센서 데이터 검증 및 운영 환경 테스트 필요
- 예지보전 모델은 향후 실제 고장 이력 기반으로 고도화 필요

---

## 20. License

본 프로젝트는 현대오토에버 모빌리티 SW스쿨 3기 스마트팩토리 과정 최종 프로젝트로 수행되었습니다.
