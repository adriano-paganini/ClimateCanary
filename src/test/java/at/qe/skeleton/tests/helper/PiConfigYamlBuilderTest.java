package at.qe.skeleton.tests.helper;

import at.qe.skeleton.helper.PiConfigYamlBuilder;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.services.RaspberryPiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PiConfigYamlBuilderTest {

    private final RaspberryPiService raspberryPiService = mock(RaspberryPiService.class);
    private final PiConfigYamlBuilder piConfigYamlBuilder = new PiConfigYamlBuilder(raspberryPiService);

    @Test
    @DisplayName("buildYaml includes Raspberry Pi, room and backend config with escaped values")
    void buildYaml_formatsConfigWithEscapedValues() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);
        room.setName("Room \"A\" \\ West");
        room.setPrivacyMode(true);

        RaspberryPi raspberryPi = new RaspberryPi();
        ReflectionTestUtils.setField(raspberryPi, "id", 5L);
        raspberryPi.setHostName("pi-\"main\"\\01");
        raspberryPi.setRoom(room);

        when(raspberryPiService.getByIdInternal(5L)).thenReturn(raspberryPi);
        ReflectionTestUtils.setField(piConfigYamlBuilder, "appBackendUrl", "http://backend.example:8080");

        String yaml = piConfigYamlBuilder.buildYaml(5L);

        assertThat(yaml).isEqualTo("""
                pi:
                  id: 5
                  room_id: 10
                  room_name: "Room \\"A\\" \\\\ West"
                  host_name: "pi-\\"main\\"\\\\01"
                  backend_url: "http://backend.example:8080"
                  privacy_mode: true
                """);
    }

    @Test
    @DisplayName("buildYaml uses configured backend URL instead of discovering local network address")
    void buildYaml_usesConfiguredBackendUrl() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);
        room.setName("Office");
        room.setPrivacyMode(false);

        RaspberryPi raspberryPi = new RaspberryPi();
        ReflectionTestUtils.setField(raspberryPi, "id", 5L);
        raspberryPi.setHostName(null);
        raspberryPi.setRoom(room);

        when(raspberryPiService.getByIdInternal(5L)).thenReturn(raspberryPi);
        ReflectionTestUtils.setField(piConfigYamlBuilder, "appBackendUrl", "http://configured-backend:9000");

        String yaml = piConfigYamlBuilder.buildYaml(5L);

        assertThat(yaml).contains("host_name: \"\"");
        assertThat(yaml).contains("backend_url: \"http://configured-backend:9000\"");
    }

    @Test
    @DisplayName("buildYaml escapes configured backend URL")
    void buildYaml_escapesConfiguredBackendUrl() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);
        room.setName("Office");
        room.setPrivacyMode(false);

        RaspberryPi raspberryPi = new RaspberryPi();
        ReflectionTestUtils.setField(raspberryPi, "id", 5L);
        raspberryPi.setHostName("pi-1");
        raspberryPi.setRoom(room);

        when(raspberryPiService.getByIdInternal(5L)).thenReturn(raspberryPi);
        ReflectionTestUtils.setField(piConfigYamlBuilder, "appBackendUrl", "http://backend.example/\"pi\"\\config");

        String yaml = piConfigYamlBuilder.buildYaml(5L);

        assertThat(yaml).contains("backend_url: \"http://backend.example/\\\"pi\\\"\\\\config\"");
    }
}
