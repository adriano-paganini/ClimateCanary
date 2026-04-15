package at.qe.skeleton.controllers;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import at.qe.skeleton.services.GatewayService;
import org.springframework.web.bind.annotation.GetMapping;

/*
Endpoint for Systemdurchstich to test the connectivity to the 
webserver on the raspberry. Here, raspberry is server, backend is client */
@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @GetMapping("/check")
    public String checkPi() {
        return "Antwort vom Raspberry Pi: " + gatewayService.getPiStatus();
    }
}