package at.qe.skeleton.services;

import at.qe.skeleton.models.RaspberryPi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

@Slf4j
@Service
public class RaspberryPiClientService {

    private final RaspberryPiService raspberryPiService;
    private final RestClient restClient;

    public RaspberryPiClientService(RaspberryPiService raspberryPiService, RestClient restClient) {
        this.raspberryPiService = raspberryPiService;
        this.restClient = restClient;
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
            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(String.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (RestClientException ex) {
            log.warn("Could not verify Pi config for piId={} at {}", piId, url, ex);
            return false;
        }
    }


}
