package onl.gcm.hermes.controller;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import onl.gcm.hermes.client.HermesClient;
import onl.gcm.hermes.db.model.LogEntry;
import onl.gcm.hermes.db.service.LogEntryService;
import onl.gcm.hermes.server.ResponseEntityCache;
import onl.gcm.hermes.server.SpringUtils;

@PropertySource("classpath:servers.properties")
@PropertySource("classpath:servers-${spring.profiles.active}.properties")
public class HermesController {

    private static final String FROM_TO_PATTERN = "[Caller: {0}] {1} -> {2}";
    private static final String UNKNOWN_CLIENT_PATTERN = "{0} did not send any HermesClient version.";
    private static final String VERSION_MISMATCH_PATTERN = "{0} is using HermesClient {1} instead of {2}!";
    private static final String CACHED_FROM_TO_PATTERN = "{0} -> {1} ({2}{3} ms)";
    private static final String CACHED = "cached, ";
    private static final String NOT_CACHED = "";
    private static final String SERVER_URL_SUFFIX = ".server.url";
    private static final String NAME_SUFFIX = ".name";
    private static final String LOCALHOST_ADDRESS = "127.0.0.1";
    private static final String LOCALHOST_HOST = "localhost";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${default.server.cache.lifetime}")
    private long defaultCacheLifetime;

    @Value("${default.server.cache.prune.delay}")
    private long defaultCachePruneDelay;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private HermesClient hermesClient;

    @Autowired
    LogEntryService logEntryService;

    private static HashMap<String, String> applications;

    private ResponseEntityCache cache;

    protected void setCacheLifetime(long lifetime) {
        cache.setLifetime(lifetime);
    }

    protected void setCachePruneDelay(long pruneDelay) {
        cache.setPruneDelay(pruneDelay);
    }

    protected LogEntry begin(String url) {
        LogEntry logEntry = new LogEntry();
        String remoteHost = httpServletRequest.getRemoteHost();
        String remoteApplication = getRemoteApplication(resolveHost(remoteHost));
        UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
        String path = builder.buildAndExpand().getPath();
        String requestClientVersion = httpServletRequest.getHeader(HermesClient.HERMES_CLIENT_VERSION_HEADER);
        String currentClientVersion = hermesClient.getVersion();
        logEntry.setRemoteHost(remoteHost);
        logEntry.setRemoteApplication(remoteApplication);
        logEntry.setRequestUrl(path);
        logEntry.setApplicationUrl(url);
        logEntry.setRequestClientVersion(requestClientVersion);
        logEntry.setClientVersion(currentClientVersion);

        String host = remoteApplication == null ? remoteHost : remoteApplication;
        String msg = MessageFormat.format(FROM_TO_PATTERN, host, path, url);
        logger.info(msg);
        if (!currentClientVersion.equals(requestClientVersion)) {
            if (requestClientVersion == null) {
                msg = MessageFormat.format(UNKNOWN_CLIENT_PATTERN, host);
                logger.warn(msg);
            } else {
                msg = MessageFormat.format(VERSION_MISMATCH_PATTERN, host, requestClientVersion, currentClientVersion);
                logger.warn(msg);
            }
        }

        return logEntry;
    }

    protected <T> ResponseEntity<T> processGet(boolean cacheable, String url, Class<T> responseType,
            Object... uriVariables) throws RestClientException {
        LogEntry logEntry = begin(url);

        ResponseEntity<T> response = cacheable ? cache.getCache(url, responseType, uriVariables) : null;
        if (response == null) {
            try {
                response = hermesClient.createRestTemplate().getForEntity(url, responseType, uriVariables);
                if (cacheable) {
                    cache.putCache(url, responseType, response, uriVariables);
                }
                end(logEntry, response);
            } catch (RestClientException e) {
                response = fail(e);
                end(logEntry, e);
            }
        } else {
            logEntry.setCached(true);
            end(logEntry, response);
        }

        logEntryService.create(logEntry);
        return response;
    }

    protected <T> ResponseEntity<T> processPost(String url, @RequestBody T request, Class<T> responseType,
            Object... uriVariables) throws RestClientException {
        LogEntry logEntry = begin(url);

        ResponseEntity<T> response = null;
        try {
            response = hermesClient.createRestTemplate().postForEntity(url, request, responseType, uriVariables);
            end(logEntry, response);
        } catch (RestClientException e) {
            response = fail(e);
            end(logEntry, e);
        }

        logEntryService.create(logEntry);
        return response;
    }

    protected void end(LogEntry logEntry, ResponseEntity<?> response) {
        logEntry.setDuration(System.currentTimeMillis() - logEntry.getDate().getTime());
        logEntry.setResponseStatus(response.getStatusCode().toString());
        String msg = MessageFormat.format(CACHED_FROM_TO_PATTERN, logEntry.getApplicationUrl(),
                logEntry.getResponseStatus(), logEntry.isCached() ? CACHED : NOT_CACHED, logEntry.getDuration());
        logger.info(msg);
    }

    protected void end(LogEntry logEntry, HttpStatusCodeException e) {
        logEntry.setDuration(System.currentTimeMillis() - logEntry.getDate().getTime());
        logEntry.setResponseStatus(e.getStatusCode().toString());
        logEntry.setErrorMessage(HermesClient.getErrorMessage(e));
        String msg = MessageFormat.format(CACHED_FROM_TO_PATTERN, logEntry.getApplicationUrl(),
                logEntry.getErrorMessage(), logEntry.isCached() ? CACHED : NOT_CACHED, logEntry.getDuration());
        logger.info(msg);
    }

    protected void end(LogEntry logEntry, RestClientException e) {
        logEntry.setDuration(System.currentTimeMillis() - logEntry.getDate().getTime());
        logEntry.setResponseStatus(e.getClass().getName());
        logEntry.setErrorMessage(HermesClient.getErrorMessage(e));
        String msg = MessageFormat.format(CACHED_FROM_TO_PATTERN, logEntry.getApplicationUrl(),
                logEntry.getErrorMessage(), logEntry.isCached() ? CACHED : NOT_CACHED, logEntry.getDuration());
        logger.info(msg);
    }

    protected <T> ResponseEntity<T> fail(RestClientException e) {
        return ResponseEntity.status(getResponseStatus(e))
                .header(HermesClient.EXCEPTION_MESSAGE_HEADER, HermesClient.encodeExceptionMessage(e))
                .build();
    }

    private static String getRemoteApplication(String remoteHost) {
        buildApplicationMap();
        return applications.get(LOCALHOST_ADDRESS.equals(remoteHost) ? LOCALHOST_HOST : remoteHost);
    }

    private static String resolveHost(String host) {
        String hostName;
        try {
            InetAddress address = InetAddress.getByName(host);
            hostName = address.getHostName();
        } catch (UnknownHostException e) {
            hostName = host;
        }
        return hostName;
    }

    private static void buildApplicationMap() {
        if (applications != null) {
            return;
        }

        Properties properties = getAllProperties();
        applications = new HashMap<>();
        properties.entrySet().stream().filter(e -> e.getKey().toString().endsWith(SERVER_URL_SUFFIX))
                .forEach(e -> {
                    try {
                        URL url = new URL(e.getValue().toString());
                        String host = url.getHost();
                        String application = (String) properties.get(e.getKey().toString() + NAME_SUFFIX);
                        applications.put(host, application);
                    } catch (MalformedURLException ex) {
                        // Nothing to do.
                    }
                });
    }

    // https://stackoverflow.com/questions/23506471/access-all-environment-properties-as-a-map-or-properties-object
    private static Properties getAllProperties() {
        Properties properties = new Properties();
        ConfigurableEnvironment env = SpringUtils.getBean(ConfigurableEnvironment.class);
        MutablePropertySources propertySources = env.getPropertySources();
        StreamSupport.stream(propertySources.spliterator(), false)
                .filter(EnumerablePropertySource.class::isInstance)
                .map(propertySource -> ((EnumerablePropertySource<?>) propertySource).getPropertyNames())
                .flatMap(Arrays::<String>stream)
                .forEach(key -> properties.setProperty(key, env.getProperty(key)));
        return properties;
    }

    private static int getResponseStatus(RestClientException e) {
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (e instanceof RestClientResponseException) {
            RestClientResponseException ex = (RestClientResponseException) e;
            status = ex.getRawStatusCode();
        }
        return status;
    }

    @PostConstruct
    private void init() {
        cache = new ResponseEntityCache(getClass().getName(), defaultCacheLifetime, defaultCachePruneDelay);
    }

}
