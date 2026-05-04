package at.qe.skeleton.tests.services;

import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.dtos.ThresholdDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.ThresholdType;
import at.qe.skeleton.services.PiRequestResult;
import at.qe.skeleton.services.RaspberryPiClientService;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.RoomService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RaspberryPiClientServiceTest {

    private static final Long PI_ID = 1L;
    private static final Long ROOM_ID = 1L;
    private static final Long SENSOR_STATION_ID = 1L;
    private static final Long THRESHOLD_ID = 1L;

    private static final String PI_IP_ADDRESS = "localhost";
    private static final String ROOM_NAME = "Room 1";
    private static final String SENSOR_STATION_NAME = "Station A";
    private static final String BLE_MAC = "AA:BB:CC:DD:EE:FF";

    private static final DeviceStatus DEVICE_STATUS = DeviceStatus.ONLINE;
    private static final Integer MEASUREMENT_INTERVAL = 1;

    private static final Metric METRIC = Metric.HUMIDITY;
    private static final Float BOUND_VALUE = 60.0f;
    private static final ThresholdType THRESHOLD_TYPE = ThresholdType.UPPER;
    private static final Boolean THRESHOLD_ENABLED = true;

    private static final Long CLIMATE_HINT_ID = 1L;
    private static final String VIOLATION_END_TIME = "2026-04-23T07:45:00Z";

    private WireMockServer wireMockServer;

    private RaspberryPiClientService clientService;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8080);
        wireMockServer.start();

        RaspberryPiService raspberryPiService = mock(RaspberryPiService.class);
        RoomService roomService = mock(RoomService.class);

        RestClient restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .build()
                ))
                .build();

        clientService = new RaspberryPiClientService(
                raspberryPiService,
                restClient,
                roomService
        );

        RaspberryPi pi = mock(RaspberryPi.class);
        when(pi.getIpAddress()).thenReturn(PI_IP_ADDRESS);
        when(raspberryPiService.getById(PI_ID)).thenReturn(pi);

        Room room = mock(Room.class);
        when(room.getName()).thenReturn(ROOM_NAME);
        when(roomService.getById(ROOM_ID)).thenReturn(room);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    private ThresholdDTO thresholdDto() {
        return new ThresholdDTO(
                THRESHOLD_ID,
                ROOM_ID,
                METRIC,
                BOUND_VALUE,
                THRESHOLD_TYPE,
                List.of(CLIMATE_HINT_ID),
                THRESHOLD_ENABLED
        );
    }

    private SensorStationDTO sensorStationDto() {
        return new SensorStationDTO(
                SENSOR_STATION_ID,
                SENSOR_STATION_NAME,
                BLE_MAC,
                DEVICE_STATUS,
                MEASUREMENT_INTERVAL,
                PI_ID,
                ROOM_ID
        );
    }

    private ViolationResolvedDTO violationResolvedDto() {
        return new ViolationResolvedDTO(
                METRIC,
                ROOM_ID,
                VIOLATION_END_TIME
        );
    }

    @Test
    void sendConfig_shouldReturnSuccess_whenPiReturns2xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/config"))
                .withRequestBody(equalTo("test-config"))
                .willReturn(ok()));

        PiRequestResult result = clientService.sendConfig(PI_ID, "test-config");

        assertEquals(PiRequestResult.SUCCESS, result);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/spi/1/config"))
                .withRequestBody(equalTo("test-config")));
    }

    @Test
    void sendConfig_shouldReturnClientError_whenPiReturns4xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/config"))
                .willReturn(badRequest()));

        PiRequestResult result = clientService.sendConfig(PI_ID, "test-config");

        assertEquals(PiRequestResult.CLIENT_ERROR, result);
    }

    @Test
    void sendConfig_shouldReturnServerError_whenPiReturns5xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/config"))
                .willReturn(serverError()));

        PiRequestResult result = clientService.sendConfig(PI_ID, "test-config");

        assertEquals(PiRequestResult.SERVER_ERROR, result);
    }

    @Test
    void sendConfig_shouldReturnUnreachable_whenPiCannotBeReached() {
        wireMockServer.stop();

        PiRequestResult result = clientService.sendConfig(PI_ID, "test-config");

        assertEquals(PiRequestResult.UNREACHABLE, result);
    }

    @Test
    void verifyPiIdInConfig_shouldReturnTrue_whenPiReturns2xx() {
        wireMockServer.stubFor(get(urlEqualTo("/api/spi/setup/verify/1"))
                .willReturn(ok()));

        boolean result = clientService.verifyPiIdInConfig(PI_ID);

        assertTrue(result);

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/spi/setup/verify/1")));
    }

    @Test
    void verifyPiIdInConfig_shouldReturnFalse_whenPiReturns4xx() {
        wireMockServer.stubFor(get(urlEqualTo("/api/spi/setup/verify/1"))
                .willReturn(badRequest()));

        boolean result = clientService.verifyPiIdInConfig(PI_ID);

        assertFalse(result);
    }

    @Test
    void verifyPiIdInConfig_shouldReturnFalse_whenPiReturns5xx() {
        wireMockServer.stubFor(get(urlEqualTo("/api/spi/setup/verify/1"))
                .willReturn(serverError()));

        boolean result = clientService.verifyPiIdInConfig(PI_ID);

        assertFalse(result);
    }

    @Test
    void verifyPiIdInConfig_shouldReturnFalse_whenPiCannotBeReached() {
        wireMockServer.stop();

        boolean result = clientService.verifyPiIdInConfig(PI_ID);

        assertFalse(result);
    }

    @Test
    void setOccupancy_shouldSendOccupancyDtoAndReturnSuccess_whenPiReturns2xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/occupancy"))
                .withRequestBody(matchingJsonPath("$.roomName", equalTo(ROOM_NAME)))
                .withRequestBody(matchingJsonPath("$.privacyMode", equalTo("true")))
                .willReturn(ok()));

        PiRequestResult result = clientService.setOccupancy(PI_ID, ROOM_ID, true);

        assertEquals(PiRequestResult.SUCCESS, result);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/spi/1/occupancy"))
                .withRequestBody(matchingJsonPath("$.roomName", equalTo(ROOM_NAME)))
                .withRequestBody(matchingJsonPath("$.privacyMode", equalTo("true"))));
    }

    @Test
    void setOccupancy_shouldReturnClientError_whenPiReturns4xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/occupancy"))
                .willReturn(badRequest()));

        PiRequestResult result = clientService.setOccupancy(PI_ID, ROOM_ID, true);

        assertEquals(PiRequestResult.CLIENT_ERROR, result);
    }

    @Test
    void setOccupancy_shouldReturnServerError_whenPiReturns5xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/occupancy"))
                .willReturn(serverError()));

        PiRequestResult result = clientService.setOccupancy(PI_ID, ROOM_ID, true);

        assertEquals(PiRequestResult.SERVER_ERROR, result);
    }

    @Test
    void setOccupancy_shouldReturnUnreachable_whenPiCannotBeReached() {
        wireMockServer.stop();

        PiRequestResult result = clientService.setOccupancy(PI_ID, ROOM_ID, true);

        assertEquals(PiRequestResult.UNREACHABLE, result);
    }

    @Test
    void getHeartbeat_shouldReturnTrue_whenPiReturns2xx() {
        wireMockServer.stubFor(get(urlEqualTo("/api/spi/1/heartbeat"))
                .willReturn(ok()));

        Boolean result = clientService.getHeartbeat(PI_ID);

        assertTrue(result);

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/spi/1/heartbeat")));
    }

    @Test
    void getHeartbeat_shouldReturnFalse_whenPiReturns4xx() {
        wireMockServer.stubFor(get(urlEqualTo("/api/spi/1/heartbeat"))
                .willReturn(badRequest()));

        Boolean result = clientService.getHeartbeat(PI_ID);

        assertFalse(result);
    }

    @Test
    void getHeartbeat_shouldReturnFalse_whenPiReturns5xx() {
        wireMockServer.stubFor(get(urlEqualTo("/api/spi/1/heartbeat"))
                .willReturn(serverError()));

        Boolean result = clientService.getHeartbeat(PI_ID);

        assertFalse(result);
    }

    @Test
    void getHeartbeat_shouldReturnFalse_whenPiCannotBeReached() {
        wireMockServer.stop();

        Boolean result = clientService.getHeartbeat(PI_ID);

        assertFalse(result);
    }

    @Test
    void startScanForAvailableSensorStations_shouldReturnSuccess_whenPiReturns2xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/scan"))
                .willReturn(ok()));

        PiRequestResult result = clientService.startScanForAvailableSensorStations(PI_ID);

        assertEquals(PiRequestResult.SUCCESS, result);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/spi/1/scan")));
    }

    @Test
    void startScanForAvailableSensorStations_shouldReturnClientError_whenPiReturns4xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/scan"))
                .willReturn(badRequest()));

        PiRequestResult result = clientService.startScanForAvailableSensorStations(PI_ID);

        assertEquals(PiRequestResult.CLIENT_ERROR, result);
    }

    @Test
    void startScanForAvailableSensorStations_shouldReturnServerError_whenPiReturns5xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/scan"))
                .willReturn(serverError()));

        PiRequestResult result = clientService.startScanForAvailableSensorStations(PI_ID);

        assertEquals(PiRequestResult.SERVER_ERROR, result);
    }

    @Test
    void startScanForAvailableSensorStations_shouldReturnUnreachable_whenPiCannotBeReached() {
        wireMockServer.stop();

        PiRequestResult result = clientService.startScanForAvailableSensorStations(PI_ID);

        assertEquals(PiRequestResult.UNREACHABLE, result);
    }

    @Test
    void connectToStation_shouldSendSensorStationDtoAndReturnSuccess_whenPiReturns2xx() {
        SensorStationDTO station = sensorStationDto();

        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/station"))
                .withRequestBody(matchingJsonPath("$.id", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.name", equalTo(SENSOR_STATION_NAME)))
                .withRequestBody(matchingJsonPath("$.bleMac", equalTo(BLE_MAC)))
                .withRequestBody(matchingJsonPath("$.deviceStatus", equalTo(DEVICE_STATUS.name())))
                .withRequestBody(matchingJsonPath("$.measurementInterval", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.raspberryPiId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.roomId", equalTo("1")))
                .willReturn(ok()));

        PiRequestResult result = clientService.connectToStation(PI_ID, station);

        assertEquals(PiRequestResult.SUCCESS, result);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/spi/1/station"))
                .withRequestBody(matchingJsonPath("$.id", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.name", equalTo(SENSOR_STATION_NAME)))
                .withRequestBody(matchingJsonPath("$.bleMac", equalTo(BLE_MAC)))
                .withRequestBody(matchingJsonPath("$.deviceStatus", equalTo(DEVICE_STATUS.name())))
                .withRequestBody(matchingJsonPath("$.measurementInterval", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.raspberryPiId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.roomId", equalTo("1"))));
    }

    @Test
    void connectToStation_shouldReturnClientError_whenPiReturns4xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/station"))
                .willReturn(badRequest()));

        PiRequestResult result = clientService.connectToStation(PI_ID, sensorStationDto());

        assertEquals(PiRequestResult.CLIENT_ERROR, result);
    }

    @Test
    void connectToStation_shouldReturnServerError_whenPiReturns5xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/station"))
                .willReturn(serverError()));

        PiRequestResult result = clientService.connectToStation(PI_ID, sensorStationDto());

        assertEquals(PiRequestResult.SERVER_ERROR, result);
    }

    @Test
    void connectToStation_shouldReturnUnreachable_whenPiCannotBeReached() {
        wireMockServer.stop();

        PiRequestResult result = clientService.connectToStation(PI_ID, sensorStationDto());

        assertEquals(PiRequestResult.UNREACHABLE, result);
    }

    @Test
    void setupStation_shouldSendSensorStationDtoAndReturnSuccess_whenPiReturns2xx() {
        SensorStationDTO station = sensorStationDto();

        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/setup"))
                .withRequestBody(matchingJsonPath("$.id", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.name", equalTo(SENSOR_STATION_NAME)))
                .withRequestBody(matchingJsonPath("$.bleMac", equalTo(BLE_MAC)))
                .withRequestBody(matchingJsonPath("$.deviceStatus", equalTo(DEVICE_STATUS.name())))
                .withRequestBody(matchingJsonPath("$.measurementInterval", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.raspberryPiId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.roomId", equalTo("1")))
                .willReturn(ok()));

        PiRequestResult result = clientService.setupStation(PI_ID, station);

        assertEquals(PiRequestResult.SUCCESS, result);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/spi/1/setup"))
                .withRequestBody(matchingJsonPath("$.id", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.name", equalTo(SENSOR_STATION_NAME)))
                .withRequestBody(matchingJsonPath("$.bleMac", equalTo(BLE_MAC)))
                .withRequestBody(matchingJsonPath("$.deviceStatus", equalTo(DEVICE_STATUS.name())))
                .withRequestBody(matchingJsonPath("$.measurementInterval", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.raspberryPiId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.roomId", equalTo("1"))));
    }

    @Test
    void setupStation_shouldReturnClientError_whenPiReturns4xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/setup"))
                .willReturn(badRequest()));

        PiRequestResult result = clientService.setupStation(PI_ID, sensorStationDto());

        assertEquals(PiRequestResult.CLIENT_ERROR, result);
    }

    @Test
    void setupStation_shouldReturnServerError_whenPiReturns5xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/setup"))
                .willReturn(serverError()));

        PiRequestResult result = clientService.setupStation(PI_ID, sensorStationDto());

        assertEquals(PiRequestResult.SERVER_ERROR, result);
    }

    @Test
    void setupStation_shouldReturnUnreachable_whenPiCannotBeReached() {
        wireMockServer.stop();

        PiRequestResult result = clientService.setupStation(PI_ID, sensorStationDto());

        assertEquals(PiRequestResult.UNREACHABLE, result);
    }

    @Test
    void informAboutNewThresholds_shouldSendThresholdDtosAndReturnSuccess_whenPiReturns2xx() {
        List<ThresholdDTO> thresholds = List.of(thresholdDto());

        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/config/thresholds"))
                .withRequestBody(matchingJsonPath("$[0].id", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].roomId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].metric", equalTo(METRIC.name())))
                .withRequestBody(matchingJsonPath("$[0].boundValue", equalTo("60.0")))
                .withRequestBody(matchingJsonPath("$[0].thresholdType", equalTo(THRESHOLD_TYPE.name())))
                .withRequestBody(matchingJsonPath("$[0].climateHintIds[0]", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].enabled", equalTo("true")))
                .willReturn(ok()));

        PiRequestResult result = clientService.informAboutNewThresholds(PI_ID, thresholds);

        assertEquals(PiRequestResult.SUCCESS, result);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/spi/1/config/thresholds"))
                .withRequestBody(matchingJsonPath("$[0].id", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].roomId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].metric", equalTo(METRIC.name())))
                .withRequestBody(matchingJsonPath("$[0].boundValue", equalTo("60.0")))
                .withRequestBody(matchingJsonPath("$[0].thresholdType", equalTo(THRESHOLD_TYPE.name())))
                .withRequestBody(matchingJsonPath("$[0].climateHintIds[0]", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].enabled", equalTo("true"))));
    }

    @Test
    void informAboutNewThresholds_shouldReturnClientError_whenPiReturns4xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/config/thresholds"))
                .willReturn(badRequest()));

        PiRequestResult result = clientService.informAboutNewThresholds(PI_ID, List.of(thresholdDto()));

        assertEquals(PiRequestResult.CLIENT_ERROR, result);
    }

    @Test
    void informAboutNewThresholds_shouldReturnServerError_whenPiReturns5xx() {
        wireMockServer.stubFor(post(urlEqualTo("/api/spi/1/config/thresholds"))
                .willReturn(serverError()));

        PiRequestResult result = clientService.informAboutNewThresholds(PI_ID, List.of(thresholdDto()));

        assertEquals(PiRequestResult.SERVER_ERROR, result);
    }

    @Test
    void informAboutNewThresholds_shouldReturnUnreachable_whenPiCannotBeReached() {
        wireMockServer.stop();

        PiRequestResult result = clientService.informAboutNewThresholds(PI_ID, List.of(thresholdDto()));

        assertEquals(PiRequestResult.UNREACHABLE, result);
    }

    @Test
    void deleteThresholds_shouldSendDeleteRequestWithThresholdDtosAndReturnSuccess_whenPiReturns204() {
        List<ThresholdDTO> thresholds = List.of(thresholdDto());

        wireMockServer.stubFor(delete(urlEqualTo("/api/spi/1/config/thresholds/remove"))
                .withRequestBody(matchingJsonPath("$[0].id", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].roomId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].metric", equalTo(METRIC.name())))
                .withRequestBody(matchingJsonPath("$[0].boundValue", equalTo("60.0")))
                .withRequestBody(matchingJsonPath("$[0].thresholdType", equalTo(THRESHOLD_TYPE.name())))
                .withRequestBody(matchingJsonPath("$[0].climateHintIds[0]", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].enabled", equalTo("true")))
                .willReturn(noContent()));

        PiRequestResult result = clientService.deleteThresholds(PI_ID, thresholds);

        assertEquals(PiRequestResult.SUCCESS, result);

        wireMockServer.verify(deleteRequestedFor(urlEqualTo("/api/spi/1/config/thresholds/remove"))
                .withRequestBody(matchingJsonPath("$[0].id", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].roomId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].metric", equalTo(METRIC.name())))
                .withRequestBody(matchingJsonPath("$[0].boundValue", equalTo("60.0")))
                .withRequestBody(matchingJsonPath("$[0].thresholdType", equalTo(THRESHOLD_TYPE.name())))
                .withRequestBody(matchingJsonPath("$[0].climateHintIds[0]", equalTo("1")))
                .withRequestBody(matchingJsonPath("$[0].enabled", equalTo("true"))));
    }

    @Test
    void deleteThresholds_shouldReturnClientError_whenPiReturns4xx() {
        wireMockServer.stubFor(delete(urlEqualTo("/api/spi/1/config/thresholds/remove"))
                .willReturn(badRequest()));

        PiRequestResult result = clientService.deleteThresholds(PI_ID, List.of(thresholdDto()));

        assertEquals(PiRequestResult.CLIENT_ERROR, result);
    }

    @Test
    void deleteThresholds_shouldReturnServerError_whenPiReturns5xx() {
        wireMockServer.stubFor(delete(urlEqualTo("/api/spi/1/config/thresholds/remove"))
                .willReturn(serverError()));

        PiRequestResult result = clientService.deleteThresholds(PI_ID, List.of(thresholdDto()));

        assertEquals(PiRequestResult.SERVER_ERROR, result);
    }

    @Test
    void deleteThresholds_shouldReturnUnreachable_whenPiCannotBeReached() {
        wireMockServer.stop();

        PiRequestResult result = clientService.deleteThresholds(PI_ID, List.of(thresholdDto()));

        assertEquals(PiRequestResult.UNREACHABLE, result);
    }

    @Test
    void resolveActiveViolation_shouldSendViolationResolvedDtoAndReturnSuccess_whenPiReturns2xx() {
        ViolationResolvedDTO dto = violationResolvedDto();

        wireMockServer.stubFor(patch(urlEqualTo("/api/spi/1/violation/resolve"))
                .withRequestBody(matchingJsonPath("$.metric", equalTo(METRIC.name())))
                .withRequestBody(matchingJsonPath("$.roomId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.endTime", equalTo(VIOLATION_END_TIME)))
                .willReturn(ok()));

        PiRequestResult result = clientService.resolveActiveViolation(PI_ID, dto);

        assertEquals(PiRequestResult.SUCCESS, result);

        wireMockServer.verify(patchRequestedFor(urlEqualTo("/api/spi/1/violation/resolve"))
                .withRequestBody(matchingJsonPath("$.metric", equalTo(METRIC.name())))
                .withRequestBody(matchingJsonPath("$.roomId", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.endTime", equalTo(VIOLATION_END_TIME))));
    }

    @Test
    void resolveActiveViolation_shouldReturnClientError_whenPiReturns4xx() {
        wireMockServer.stubFor(patch(urlEqualTo("/api/spi/1/violation/resolve"))
                .willReturn(badRequest()));

        PiRequestResult result = clientService.resolveActiveViolation(PI_ID, violationResolvedDto());

        assertEquals(PiRequestResult.CLIENT_ERROR, result);
    }

    @Test
    void resolveActiveViolation_shouldReturnServerError_whenPiReturns5xx() {
        wireMockServer.stubFor(patch(urlEqualTo("/api/spi/1/violation/resolve"))
                .willReturn(serverError()));

        PiRequestResult result = clientService.resolveActiveViolation(PI_ID, violationResolvedDto());

        assertEquals(PiRequestResult.SERVER_ERROR, result);
    }

    @Test
    void resolveActiveViolation_shouldReturnUnreachable_whenPiCannotBeReached() {
        wireMockServer.stop();

        PiRequestResult result = clientService.resolveActiveViolation(PI_ID, violationResolvedDto());

        assertEquals(PiRequestResult.UNREACHABLE, result);
    }
}