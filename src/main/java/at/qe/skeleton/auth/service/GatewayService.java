package at.qe.skeleton.services;


import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GatewayService {

    private final RestClient restClient;

    public GatewayService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://192.168.0.81:5000")  //raspberry ip
                .build();
    }

    public String getPiStatus() {
        return restClient.get()
                .uri("/test")
                .retrieve()
                .body(String.class); 
                
    }
}