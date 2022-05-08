package onl.gcm.hermes.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "onl.gcm.hermes.controller", "onl.gcm.hermes.server", "onl.gcm.hermes.client" })
public class HermesServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HermesServerApplication.class, args);
	}

}
