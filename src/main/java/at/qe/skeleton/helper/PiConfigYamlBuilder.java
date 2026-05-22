package at.qe.skeleton.helper;

import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.services.RaspberryPiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PiConfigYamlBuilder {

    private final RaspberryPiService raspberryPiService;

    @Value("${app.backend-url}")
    private String backendUrl;

    public PiConfigYamlBuilder(RaspberryPiService raspberryPiService) {
        this.raspberryPiService = raspberryPiService;
    }

    public String buildYaml(Long piId) {
        RaspberryPi raspberryPi = raspberryPiService.getByIdInternal(piId);
        Room room = raspberryPi.getRoom();

        return """
                pi:
                  id: %d
                  room_id: %d
                  room_name: "%s"
                  host_name: "%s"
                  backend_url: "%s"
                  privacy_mode: %s
                """.formatted(
                raspberryPi.getId(),
                room.getId(),
                escapeYaml(room.getName()),
                escapeYaml(raspberryPi.getHostName()),
                escapeYaml(backendUrl),
                room.getPrivacyMode()
        );
    }

    private String escapeYaml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
