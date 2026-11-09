# HKG Standard Gen5W → CarrotPilot 7706 Bridge (`HKG_5W_CarrotNavi`)

현대·기아 표준형 5세대 와이드(Standard Gen5W, Android 4.4.2 API 19) 순정 헤드유닛(`com.mnsoft.navi` / `com.hkmc.micom.ClusterService`)의 실시간 내비게이션·단속카메라·경로안내(TBT) 데이터를 **CarrotPilot (`UDP 7706`)** 포맷으로 변환·전송하는 NEMESIS 패키지 및 GitHub 자동 업데이트 시스템입니다.

---

## 주요 기능

1. **CarrotPilot `UDP 7706` 실시간 브릿지 (5Hz)**
   - **단속카메라·안전구간 (SDI 7대 항목)**:
     - 고정식 과속카메라 (`nSdiType = 1`), 신호·과속카메라 (`nSdiType = 0`), 이동식 카메라 (`nSdiType = 7`), 후면 번호판인식 카메라 (`nSdiType = 75/76`)
     - 구간단속 카메라 및 구간 내 차량 평균속도 (`nSdiType = 4`, `nSdiBlockType = 2`, `nSdiBlockDist`, `nSdiBlockSpeed`, `nSdiBlockAverageSpeed`)
     - 어린이보호구역 (`nSdiType = 20` / `nSdiPlusType = 20`, 30 km/h 연동)
     - 과속방지턱 (`nSdiType = 22` / `nSdiPlusType = 22`, 도심 일반도로 `roadcate = 4` 보장)
   - **경로안내 (Turn-By-Turn 1차·2차 회전 및 목적지 정보)**:
     - `Navigation$DisplayType`(47종) + 회전 각도(`Direction` `0~360°`) + `SCC_HU_Navi_P`(`nextTurnType1/2`)를 CarrotPilot `nTBTTurnType` / `nTBTTurnTypeNext` 코드로 매핑
     - `com.mnsoft.navi`의 UTF-16LE/BE 바이트 스왑(`CrossroadName`, `DestinationName`) 자동 복원 디코더 탑재 (`szTBTMainText`, `szGoalName`)
     - 목적지 잔여 거리(`nGoPosDist`), 잔여 시간(`nGoPosTime`), 차선 수 기반 도로 폭(`nTBTNextRoadWidth`), GPS(`vpPosPointLat`, `vpPosPointLon`, `nPosAngle`, `nPosSpeed`) 전송
   - **CarrotPilot 자동 탐색**:
     - `UDP 7705` 브로드캐스트 수신 + `wlan0` ARP 테이블(`/proc/net/arp`) 스캔 + 서브넷 브로드캐스트(`192.168.0.255:7706`) 병행

2. **순정 핫스팟(SoftAP) 자동 활성화 및 유지**
   - 부팅 시 `wifi.hotspot.subscription=true`, `wifi.hotspot.apn=Public`, `eth0`(`192.168.0.123`) 서브넷 충돌 제거 및 `service call wifi 29 i32 0 i32 1`로 SoftAP 자동 활성화

3. **GitHub 자동 업데이트 (OTA)**
   - `rmnet1`(블루링크 폐쇄망 DNS `172.19.140.37`) 함정을 우회하기 위해 공용 LTE(`rmnet0`)를 통한 직접 UDP DNS 쿼리(`8.8.8.8` / `1.1.1.1`) 수행
   - Android 4.4.2 `conscrypt.odex`의 `TLSv1.2` + SNI(`setHostname`) 명시적 활성화
   - [`version.json`](version.json) 및 `carrotbridge.jar.sha256` 기반 `SHA-256` 무결성 검증과 데몬 비정상 종료 시 `.bak` 자동 롤백 워치독 내장

4. **USB / 로컬 TBT 정밀 덤프 (`scripts/tbt-dump.sh`)**
   - `am broadcast -a com.android.Eng.LogSetting --eia intArray 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0` 브로드캐스트로 `com.mnsoft.navi` 내부 `[Cluster]` 평문 TBT 로거 자동 활성화
   - 경로안내 중 모든 TBT 이벤트와 `dumpsys` 스냅샷을 `/data/local/tmp/hkg_tbt_dump/tbt_live_trace.log`에 기록하며, USB 메모리(`/storage/usb0`) 장착 시 `/storage/usb0/HKG_TBT_Dump/`로 자동 복사

5. **MAP 버튼 3연타 & NEMESIS 대시보드 상태 오버레이 (`StatusDashboardOverlay`)**
   - 전면 물리 `MAP` 버튼을 연속 3번 누르거나(`ivi_mode_broadcast` `KEYCODE_SHORTCUT_MAP` / `com.hkmc.intent.action.MAP_SHORTCUT` 감지), NEMESIS 대시보드에서 `CarrotNavi 7706 Bridge`를 선택하면 현재 화면 위에 **실시간 패키지 버전·동작 상태 대시보드 오버레이**가 즉시 표시됨
   - 실시간 표시 항목: 패키지 버전, UDP `7706` 송신 카운터, Wi-Fi 핫스팟 상태, 콤마(캐럿파일럿) 연결 IP 및 수신 차속, 현재 도로 제한속도, 전방 단속카메라/구간단속/방지턱(SDI), 1차/2차 TBT 회전 및 한글 교차로명, 목적지 잔여 거리/시간
   - 터치 조작 버튼: `[📶 핫스팟 설정 화면]`(`com.hkmc.wifisettings/.WifiHotspot` 즉시 진입), `[⚙️ 엔지니어링 WiFi]`(`com.hkmc.system.app.engineering/.module.WifiInfo` 진입), `[📡 핫스팟 즉시 켜기]`, `[🔄 GitHub 업데이트]`, `[💾 TBT 정밀 덤프]`, `[✖ 닫기 (MAP 3연타)]`

---

## 빌드 방법

```bash
# CarrotNavi 7706 Bridge 패키지 및 USB 번들(HKG_5W_CarrotNavi_USB.zip) 빌드
python package_carrotnavi_bridge/build_package.py

# Engineering WiFi Info 단독 패키지 빌드
python package_engineering_wifiinfo/build_package.py
```

---

## Gen5W 확장 DisplayType / 회전 각도 매핑 (v1.2.7, K7 PE 실주행 검증)

2026-11-08 K7 PE(`wp_daudioplus_ygpe_kr`) 퇴근길 덤프(서호학산TG → 고속도로 → 남악IC → 시내 → 목적지)에서 확인한 값입니다.

| 순정 0x4E DisplayType | 실제 의미 (덤프 근거) | CarrotPilot `nTBTTurnType` |
|---|---|---|
| `0xd` + Direction `22.5` | 완만한 우측 (TG 진입로, 고속도로 출구) | `6` (fork right, 도로 제한속도 유지) |
| `0xd` + Direction `90` / `270` | 우회전 / 좌회전 | `13` / `12` |
| `0x42` | 직진 유지, 고속도로 (다음 안내까지 13.5km 카운트다운) | `51` |
| `0x46` | 직진 유지, 일반도로 | `51` |
| `0xB2` | 요금소 (교차로명 = "서호학산톨게이트", 500m 카운트다운) | `153` |
| `0x71` | 목적지 부근 (200m 카운트다운 → `0x3` ARRIVED) | `201` |
| 그 외 미확인 코드 | — | `51` (감속 유발 금지) |

Direction은 22.5° 단위입니다: `0` 직진, `22.5` 완만한 우측, `45~112.5` 우회전, `135~157.5` 급우회전, `180` 유턴, `202.5~225` 급좌회전, `247.5~315` 좌회전, `337.5` 완만한 좌측, `1912.5` 무효.

그 외 수정:
- dumpsys `TBT SubInfo`의 손상값(`18.5 METER` = 실제 km, `minutes = 12` = 도착예정시각의 분)이 로그캣 값을 덮어써서 `nGoPosDist`/`nGoPosTime`이 매 패킷 튀던 문제 → 로그캣 값이 15초 이내로 신선하면 dumpsys 무시
- dumpsys `Unknown display type(0x42)` 문자열이 `szTBTMainText`로 그대로 전송되던 문제 → 16진 코드 해석 + 한글 라벨
- dumpsys 무효 거리 `65535.15 unknown scale(0x0f)` 필터링
- 자동 업데이트 대상을 `disdong/HKG_5W_CarrotNavi`로 변경 (원본 1.2.6으로 덮어쓰기 방지)

### 실주행 덤프 리플레이 (오프라인 검증)

```bash
cd package_carrotnavi_bridge
mkdir -p /tmp/cls
javac --release 8 -encoding UTF-8 -d /tmp/cls src_java/com/hkg/carrotbridge/CarrotBridgeMain.java tools/com/hkg/carrotbridge/TbtReplay.java
java -Dfile.encoding=UTF-8 -cp /tmp/cls com.hkg.carrotbridge.TbtReplay tbt_live_trace.log.old tbt_live_trace.log
```

차에서 기록한 `TX7706` 패킷 시점마다 현재 코드로 패킷을 다시 만들어 TBT 값을 비교하고, SDI/제한속도 값이 기록과 다르면 `SDI DIFF`로 표시합니다.
