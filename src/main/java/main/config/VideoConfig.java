package main.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.video")
@Getter
@Setter
public class VideoConfig {
    private String mode;
    private String localPath;
    private String cloudfrontBaseUrl;

    public boolean isCloudFront() {
        return "cloudfront".equalsIgnoreCase(mode);
    }

    public boolean isLocal() {
        return "local".equalsIgnoreCase(mode);
    }
}
