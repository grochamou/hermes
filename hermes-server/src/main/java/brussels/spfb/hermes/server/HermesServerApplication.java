package brussels.spfb.hermes.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "brussels.spfb.hermes.controller", "brussels.spfb.hermes.client" })
public class HermesServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HermesServerApplication.class, args);
	}

}
