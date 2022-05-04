package onl.gcm.hermes.controller;

import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PropertySource("classpath:hermes-${spring.profiles.active}.properties")
public class ServerController extends HermesController {

    @GetMapping("${hermes.path.alive}")
    protected ResponseEntity<Void> isAlive() {
        return ResponseEntity.ok().build();
    }

}
