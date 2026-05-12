package at.qe.skeleton.services;

import at.qe.skeleton.dtos.OccupancyDTO;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.dtos.ThresholdDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.List;

@Slf4j
@Service
public class RaspberryPiServerService {

    private static final String URL_PROTOCOL = "http://";
    private static final String API_BASE_PATH = ":8000/api/spi/";

    private final RaspberryPiService raspberryPiService;
    private final RestClient restClient;
    private final RoomService roomService;

    public RaspberryPiServerService(RaspberryPiService raspberryPiService, RestClient restClient, RoomService roomService) {
        this.raspberryPiService = raspberryPiService;
        this.restClient = restClient;
        this.roomService = roomService;
    }

    private String buildPiUrl(RaspberryPi pi, String path) {
        return URL_PROTOCOL + pi.getIpAddress() + API_BASE_PATH + path;
    }

    public PiRequestResult sendConfig(Long piId, String config) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = buildPiUrl(pi, piId + "/config");

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .body(config)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }

            log.warn("Unexpected non-success response while sending config to Raspberry Pi {} at {}: status={}",
                    piId, url, response.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected config update at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error while receiving config at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable while sending config at {}: {}",
                    piId, url, ex.getMessage());
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Failed to send config to Raspberry Pi {} at {} due to REST client error",
                    piId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }

    public boolean verifyPiIdInConfig(Long piId) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = buildPiUrl(pi, "setup/verify/" + piId);

        try {
            ResponseEntity<Void> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();

            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected config verification at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return false;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error during config verification at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return false;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable during config verification at {}: {}",
                    piId, url, ex.getMessage());
            return false;

        } catch (RestClientException ex) {
            log.warn("Failed to verify config for Raspberry Pi {} at {} due to REST client error",
                    piId, url, ex);
            return false;
        }
    }

    public PiRequestResult setOccupancy(Long piId, Long roomId, boolean privacyMode) {
        Room room = roomService.getById(roomId);
        OccupancyDTO dto = new OccupancyDTO(room.getName(), privacyMode);
        RaspberryPi pi = raspberryPiService.getByIdInternal(piId);
        String url = buildPiUrl(pi, piId + "/occupancy");

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }

            log.warn("Unexpected non-success response while setting occupancy/privacy mode for Raspberry Pi {} and room {} at {}: status={}",
                    piId, roomId, url, response.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected occupancy/privacy mode update for room {} at {}: status={}, response={}",
                    piId, roomId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error while setting occupancy/privacy mode for room {} at {}: status={}, response={}",
                    piId, roomId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable while setting occupancy/privacy mode for room {} at {}: {}",
                    piId, roomId, url, ex.getMessage());
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Failed to set occupancy/privacy mode for Raspberry Pi {} and room {} at {} due to REST client error",
                    piId, roomId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }

    public Boolean getHeartbeat(Long piId) {
        RaspberryPi pi = raspberryPiService.getByIdInternal(piId);
        String url = buildPiUrl(pi, piId + "/heartbeat");

        try {
            ResponseEntity<Void> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();

            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected heartbeat request at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return false;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error during heartbeat request at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return false;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable during heartbeat request at {}: {}",
                    piId, url, ex.getMessage());
            return false;

        } catch (RestClientException ex) {
            log.warn("Failed to get heartbeat from Raspberry Pi {} at {} due to REST client error",
                    piId, url, ex);
            return false;
        }
    }

    public PiRequestResult startScanForAvailableSensorStations(Long piId) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = buildPiUrl(pi, piId + "/scan");
        log.debug("SENDING TO ADDRESS: {}", url);

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }

            log.warn("Unexpected non-success response while starting sensor station scan on Raspberry Pi {} at {}: status={}",
                    piId, url, response.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected sensor station scan request at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error while scanning for sensor stations at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable while starting sensor station scan at {}: {}",
                    piId, url, ex.getMessage());
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Failed to start sensor station scan on Raspberry Pi {} at {} due to REST client error",
                    piId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }

    public PiRequestResult connectToStation(Long piId, SensorStationDTO station) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = buildPiUrl(pi, piId + "/station");

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .body(station)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }

            log.warn("Unexpected non-success response while connecting sensor station to Raspberry Pi {} at {}: status={}",
                    piId, url, response.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected sensor station connection request at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error while connecting sensor station at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable while connecting sensor station at {}: {}",
                    piId, url, ex.getMessage());
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Failed to connect sensor station to Raspberry Pi {} at {} due to REST client error",
                    piId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }

    public PiRequestResult setupStation(Long piId, SensorStationDTO station) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = buildPiUrl(pi, piId + "/setup");

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .body(station)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }

            log.warn("Unexpected non-success response while setting up sensor station on Raspberry Pi {} at {}: status={}",
                    piId, url, response.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected sensor station setup request at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error while setting up sensor station at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable while setting up sensor station at {}: {}",
                    piId, url, ex.getMessage());
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Failed to set up sensor station on Raspberry Pi {} at {} due to REST client error",
                    piId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }

    public PiRequestResult informAboutNewThresholds(Long piId, List<ThresholdDTO> thresholdDTOS) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = buildPiUrl(pi, piId + "/config/thresholds");

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .body(thresholdDTOS)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }

            log.warn("Unexpected non-success response while sending threshold configuration to Raspberry Pi {} at {}: status={}",
                    piId, url, response.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected threshold configuration update at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error while receiving threshold configuration at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable while sending threshold configuration at {}: {}",
                    piId, url, ex.getMessage());
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Failed to send threshold configuration to Raspberry Pi {} at {} due to REST client error",
                    piId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }

    public PiRequestResult deleteThresholds(Long piId, List<ThresholdDTO> thresholdDTOS) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = buildPiUrl(pi, piId + "/config/thresholds/remove");

        try {
            ResponseEntity<Void> response = restClient.method(HttpMethod.DELETE)
                    .uri(url)
                    .body(thresholdDTOS)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }

            log.warn("Unexpected non-success response while deleting threshold configuration from Raspberry Pi {} at {}: status={}",
                    piId, url, response.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected threshold deletion request at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error while deleting threshold configuration at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable while deleting threshold configuration at {}: {}",
                    piId, url, ex.getMessage());
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Failed to delete threshold configuration from Raspberry Pi {} at {} due to REST client error",
                    piId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }

    public PiRequestResult resolveActiveViolation(Long piId, ViolationResolvedDTO dto) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = buildPiUrl(pi, piId + "/violation/resolve");

        try {
            ResponseEntity<Void> response = restClient.patch()
                    .uri(url)
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }

            log.warn("Unexpected non-success response while resolving active violation on Raspberry Pi {} at {}: status={}",
                    piId, url, response.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpClientErrorException ex) {
            log.warn("Raspberry Pi {} rejected active violation resolution request at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Raspberry Pi {} reported a server error while resolving active violation at {}: status={}, response={}",
                    piId, url, ex.getStatusCode(), ex.getResponseBodyAsString());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException ex) {
            log.warn("Raspberry Pi {} is unreachable while resolving active violation at {}: {}",
                    piId, url, ex.getMessage());
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Failed to resolve active violation on Raspberry Pi {} at {} due to REST client error",
                    piId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }
}