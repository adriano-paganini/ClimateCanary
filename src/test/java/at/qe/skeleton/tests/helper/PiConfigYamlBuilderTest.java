package at.qe.skeleton.tests.helper;

import at.qe.skeleton.helper.PiConfigYamlBuilder;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.services.RaspberryPiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PiConfigYamlBuilderTest {

    private final RaspberryPiService raspberryPiService = mock(RaspberryPiService.class);
    private final PiConfigYamlBuilder piConfigYamlBuilder = new PiConfigYamlBuilder(raspberryPiService);

    @Test
    @DisplayName("buildYaml includes Raspberry Pi, room and backend config with escaped values")
    void buildYaml_formatsConfigWithEscapedValues() throws Exception {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);
        room.setName("Room \"A\" \\ West");
        room.setPrivacyMode(true);

        RaspberryPi raspberryPi = new RaspberryPi();
        ReflectionTestUtils.setField(raspberryPi, "id", 5L);
        raspberryPi.setHostName("pi-\"main\"\\01");
        raspberryPi.setRoom(room);

        NetworkInterface networkInterface = activePhysicalInterfaceWithAddress("192.168.1.50");

        when(raspberryPiService.getByIdInternal(5L)).thenReturn(raspberryPi);

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = Mockito.mockStatic(NetworkInterface.class)) {
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                    .thenReturn(Collections.enumeration(List.of(networkInterface)));

            String yaml = piConfigYamlBuilder.buildYaml(5L);

            assertThat(yaml).isEqualTo("""
                    pi:
                      id: 5
                      room_id: 10
                      room_name: "Room \\\"A\\\" \\\\ West"
                      host_name: "pi-\\\"main\\\"\\\\01"
                      backend_url: "http://192.168.1.50:8080"
                      privacy_mode: true
                    """);
        }
    }

    @Test
    @DisplayName("buildYaml uses empty string when host name is null")
    void buildYaml_usesEmptyStringForNullHostName() throws Exception {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);
        room.setName("Office");
        room.setPrivacyMode(false);

        RaspberryPi raspberryPi = new RaspberryPi();
        ReflectionTestUtils.setField(raspberryPi, "id", 5L);
        raspberryPi.setHostName(null);
        raspberryPi.setRoom(room);

        NetworkInterface networkInterface = activePhysicalInterfaceWithAddress("10.0.0.12");

        when(raspberryPiService.getByIdInternal(5L)).thenReturn(raspberryPi);

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = Mockito.mockStatic(NetworkInterface.class)) {
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                    .thenReturn(Collections.enumeration(List.of(networkInterface)));

            String yaml = piConfigYamlBuilder.buildYaml(5L);

            assertThat(yaml).contains("host_name: \"\"");
            assertThat(yaml).contains("backend_url: \"http://10.0.0.12:8080\"");
        }
    }

    @Test
    @DisplayName("buildYaml throws when no suitable local IPv4 address exists")
    void buildYaml_throwsWhenNoSuitableLocalIpv4AddressExists() throws Exception {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);

        RaspberryPi raspberryPi = new RaspberryPi();
        raspberryPi.setRoom(room);

        NetworkInterface networkInterface = activePhysicalInterfaceWithAddress("169.254.1.10");

        when(raspberryPiService.getByIdInternal(5L)).thenReturn(raspberryPi);

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = Mockito.mockStatic(NetworkInterface.class)) {
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                    .thenReturn(Collections.enumeration(List.of(networkInterface)));

            assertThatThrownBy(() -> piConfigYamlBuilder.buildYaml(5L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No suitable local IPv4 address found");
        }
    }

    @Test
    @DisplayName("buildYaml wraps SocketException from network discovery")
    void buildYaml_wrapsSocketExceptionFromNetworkDiscovery()  {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);

        RaspberryPi raspberryPi = new RaspberryPi();
        raspberryPi.setRoom(room);

        when(raspberryPiService.getByIdInternal(5L)).thenReturn(raspberryPi);

        SocketException socketException = new SocketException("network failure");

        try (MockedStatic<NetworkInterface> mockedNetworkInterface = Mockito.mockStatic(NetworkInterface.class)) {
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                    .thenThrow(socketException);

            assertThatThrownBy(() -> piConfigYamlBuilder.buildYaml(5L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Could not determine local IP address")
                    .hasCause(socketException);
        }
    }

    private NetworkInterface activePhysicalInterfaceWithAddress(String ipAddress) throws Exception {
        NetworkInterface networkInterface = mock(NetworkInterface.class);

        when(networkInterface.isUp()).thenReturn(true);
        when(networkInterface.isLoopback()).thenReturn(false);
        when(networkInterface.isVirtual()).thenReturn(false);
        when(networkInterface.getInetAddresses())
                .thenReturn(Collections.enumeration(List.of(InetAddress.getByName(ipAddress))));

        return networkInterface;
    }
}
