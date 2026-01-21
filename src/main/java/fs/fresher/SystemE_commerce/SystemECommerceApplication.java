package fs.fresher.SystemE_commerce;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class SystemECommerceApplication {

	public static void main(String[] args) {
		try {
			log.info("🚀 Starting E-Commerce Application...");
			SpringApplication app = new SpringApplication(SystemECommerceApplication.class);
			
			// Set some default properties
			System.setProperty("spring.jpa.open-in-view", "false");
			
			app.run(args);
			log.info("✅ E-Commerce Application started successfully!");
			
		} catch (Exception e) {
			log.error("❌ Failed to start E-Commerce Application: {}", e.getMessage());
			log.error("💡 Please check your database connection and configuration");
			e.printStackTrace();
			System.exit(1);
		}
	}

}
