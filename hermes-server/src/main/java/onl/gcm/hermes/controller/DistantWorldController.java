package onl.gcm.hermes.controller;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import onl.gcm.hermes.client.DistantWorldClient;
import onl.gcm.hermes.dto.DistantWorldDTO;

@RestController
@PropertySource("classpath:servers.properties")
@PropertySource("classpath:servers-${spring.profiles.active}.properties")
@PropertySource("classpath:hermes.properties")
@PropertySource("classpath:hermes-${spring.profiles.active}.properties")
public class DistantWorldController extends HermesController {

    @Value("${distantworld.server.url}")
    private String distantWorldServerUrl;

    @Value("${distantworld.server.cache.lifetime}")
    private long cacheLifetime;

    @Value("${distantworld.server.cache.prune.delay}")
    private long cachePruneDelay;

    @Autowired
    private DistantWorldClient distantWorldClient;

    @GetMapping("${distantworld.path}${distantworld.path.alive}")
    protected ResponseEntity<Void> isAlive() {
        return processGet(false, distantWorldServerUrl + distantWorldClient.getAlivePath(), Void.class);
    }

    @GetMapping("${distantworld.path}${distantworld.path.test}")
    protected ResponseEntity<DistantWorldDTO> test(@PathVariable String id) {
        return processGet(true, distantWorldServerUrl + distantWorldClient.getTestPath(), DistantWorldDTO.class, id);
    }

    @GetMapping("${distantworld.path}${distantworld.path.nocontent}")
    protected ResponseEntity<Void> noContent() {
        return processGet(true, distantWorldServerUrl + distantWorldClient.getNoContentPath(), Void.class);
    }

    @GetMapping("${distantworld.path}${distantworld.path.notfound}")
    protected ResponseEntity<Void> notFound() {
        return processGet(false, distantWorldServerUrl + distantWorldClient.getNotFoundPath(), Void.class);
    }

    @GetMapping("${distantworld.path}${distantworld.path.crash}")
    protected ResponseEntity<Void> crash() {
        return processGet(false, distantWorldServerUrl + distantWorldClient.getCrashPath(), Void.class);
    }

    @PostConstruct
    private void initialize() {
        setCacheLifetime(cacheLifetime);
        setCachePruneDelay(cachePruneDelay);
    }

}
