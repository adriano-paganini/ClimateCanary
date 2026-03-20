package at.qe.skeleton.controllers;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import at.qe.skeleton.services.GatewayService;
import org.springframework.web.bind.annotation.GetMapping;

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