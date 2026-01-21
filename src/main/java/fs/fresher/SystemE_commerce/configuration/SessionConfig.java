package fs.fresher.SystemE_commerce.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@Configuration
@Slf4j
public class SessionConfig {
    
    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent se) {
                log.debug("Session created: {}", se.getSession().getId());
            }
            
            @Override
            public void sessionDestroyed(HttpSessionEvent se) {
                String sessionId = se.getSession().getId();
                log.info("Session destroyed: {}, cleaning up associated cart", sessionId);
                
                // Note: Cart cleanup will be handled by scheduled task instead
                // to avoid circular dependency during application startup
            }
        };
    }
}