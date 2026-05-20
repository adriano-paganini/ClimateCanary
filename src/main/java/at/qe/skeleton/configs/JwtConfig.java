package at.qe.skeleton.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.expirationMs}")
    private long jwtExpirationMs;
    @Value("${app.jwt.login.url}")
    private String loginUrl;
    @Value("${app.jwt.token.header}")
    private String tokenHeader;
    @Value("${app.jwt.token.prefix}")
    private String tokenPrefix;
    @Value("${app.jwt.token.type}")
    private String tokenType;
    @Value("${app.jwt.token.issuer}")
    private String tokenIssuer;
    @Value("${app.jwt.token.audience}")
    private String tokenAudience;

}
