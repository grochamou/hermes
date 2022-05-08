package onl.gcm.hermes.controller;

import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PropertySource("classpath:hermes.properties")
@PropertySource("classpath:hermes-${spring.profiles.active}.properties")
public class ServerController extends HermesController {

    @GetMapping("${hermes.server.path.alive}")
    protected ResponseEntity<Void> isAlive() {
        return ResponseEntity.ok().build();
    }

}
