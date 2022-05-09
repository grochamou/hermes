package onl.gcm.hermes.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages = { "onl.gcm.hermes.controller", "onl.gcm.hermes.server", "onl.gcm.hermes.db",
		"onl.gcm.hermes.client" })
@EnableJpaRepositories("onl.gcm.hermes.db.repository")
@EnableTransactionManagement
@EntityScan("onl.gcm.hermes.db.model")
public class HermesServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HermesServerApplication.class, args);
	}

}
