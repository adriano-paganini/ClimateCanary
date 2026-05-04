package at.qe.skeleton.services;

import at.qe.skeleton.dtos.OccupancyDTO;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

@Slf4j
@Service
public class RaspberryPiClientService {

    private final RaspberryPiService raspberryPiService;
    private final RestClient restClient;
    private final RoomService roomService;

    public RaspberryPiClientService(RaspberryPiService raspberryPiService, RestClient restClient, RoomService roomService) {
        this.raspberryPiService = raspberryPiService;
        this.restClient = restClient;
        this.roomService = roomService;
    }

    public PiRequestResult sendConfig(Long piId, String config) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = "http://" + pi.getIpAddress() + ":8080/api/spi/" + piId + "/config";

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .body(config)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }

            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpClientErrorException ex) {
            log.warn("Pi {} rejected config request with status {}", piId, ex.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Pi {} failed while receiving config with status {}", piId, ex.getStatusCode());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException _) {
            log.warn("Could not reach Pi {} at {}", piId, url);
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Could not send config to Pi {} at {}", piId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }

    public boolean verifyPiIdInConfig(Long piId) {
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = "http://" + pi.getIpAddress() + ":8080/api/spi/setup/verify/" + piId;

        try {
            ResponseEntity<Void> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();

            return response.getStatusCode().is2xxSuccessful();

        } catch (RestClientException ex) {
            log.warn("Could not verify Pi config for piId={} at {}", piId, url, ex);
            return false;
        }
    }

    public PiRequestResult setPrivacyMode(Long piId,Long roomId, boolean privacyMode){
        Room room =  roomService.getById(roomId);
        OccupancyDTO dto = new OccupancyDTO(room.getName(),privacyMode);
        RaspberryPi pi = raspberryPiService.getById(piId);
        String url = "http://" + pi.getIpAddress() + ":8080/api/spi/" + piId + "/occupancy";
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return PiRequestResult.SUCCESS;
            }
            return PiRequestResult.CLIENT_ERROR;
        } catch (HttpClientErrorException ex) {
            log.warn("Pi {} rejected setting of PrivacyMode with status {}", piId, ex.getStatusCode());
            return PiRequestResult.CLIENT_ERROR;

        } catch (HttpServerErrorException ex) {
            log.warn("Pi {} failed while setting PrivacyMode with status {}", piId, ex.getStatusCode());
            return PiRequestResult.SERVER_ERROR;

        } catch (ResourceAccessException _) {
            log.warn("Could not reach Pi {} at {}", piId, url);
            return PiRequestResult.UNREACHABLE;

        } catch (RestClientException ex) {
            log.warn("Could not set PrivacyMode to Pi {} at {}", piId, url, ex);
            return PiRequestResult.UNREACHABLE;
        }
    }
}
