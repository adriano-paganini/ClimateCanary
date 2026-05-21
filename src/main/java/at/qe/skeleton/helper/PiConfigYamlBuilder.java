package at.qe.skeleton.helper;

import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.services.RaspberryPiService;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Component
public class PiConfigYamlBuilder {

    private final RaspberryPiService raspberryPiService;

    public PiConfigYamlBuilder(RaspberryPiService raspberryPiService) {
        this.raspberryPiService = raspberryPiService;
    }

    public String buildYaml(Long piId) {
        RaspberryPi raspberryPi = raspberryPiService.getByIdInternal(piId);
        Room room = raspberryPi.getRoom();
        String backendUrl = findBackendUrl();

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
    private String findBackendUrl() {
        return "http://" + findLocalIpAddress() + ":8080";
    }

    private String findLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (!networkInterface.isUp()
                        || networkInterface.isLoopback()
                        || networkInterface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    if (address instanceof Inet4Address
                            && !address.isLoopbackAddress()
                            && !address.isLinkLocalAddress()
                            && !address.isMulticastAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            throw new IllegalStateException("Could not determine local IP address", e);
        }

        throw new IllegalStateException("No suitable local IPv4 address found");
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
