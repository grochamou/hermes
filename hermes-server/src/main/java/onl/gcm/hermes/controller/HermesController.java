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

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import onl.gcm.hermes.logging.LogEntry;
import onl.gcm.hermes.server.ResponseEntityCache;

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
    private final ResponseEntityCache cache = new ResponseEntityCache(getClass().getName());

    @Autowired
    private ConfigurableEnvironment env;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private HermesClient hermesClient;

    private HashMap<String, String> applications;

    protected void setCacheLifetime(long lifetime) {
        cache.setLifetime(lifetime);
    }

    protected void setCachePruneDelay(long pruneDelay) {
        cache.setPruneDelay(pruneDelay);
    }

    @SuppressWarnings("java:S2629")
    protected LogEntry begin(String url) {
        LogEntry entry = new LogEntry();
        String remoteHost = httpServletRequest.getRemoteHost();
        String remoteApplication = getRemoteApplication(resolveHost(remoteHost));
        UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
        String path = builder.buildAndExpand().getPath();
        String requestClientVersion = httpServletRequest.getHeader(HermesClient.VERSION_HEADER);
        String currentClientVersion = hermesClient.getVersion();
        entry.setRemoteHost(remoteHost);
        entry.setRemoteApplication(remoteApplication);
        entry.setRequestUrl(path);
        entry.setApplicationUrl(url);
        entry.setRequestClientVersion(requestClientVersion);
        entry.setClientVersion(currentClientVersion);
        // "Preconditions" and logging arguments should not require evaluation.
        logger.info(MessageFormat.format(FROM_TO_PATTERN, remoteApplication == null ? remoteHost : remoteApplication,
                path, url));
        if (!currentClientVersion.equals(requestClientVersion)) {
            if (requestClientVersion == null) {
                // "Preconditions" and logging arguments should not require evaluation.
                logger.warn(MessageFormat.format(UNKNOWN_CLIENT_PATTERN, path));
            } else {
                // "Preconditions" and logging arguments should not require evaluation.
                logger.warn(MessageFormat.format(VERSION_MISMATCH_PATTERN, path, requestClientVersion,
                        currentClientVersion));
            }
        }
        return entry;
    }

    protected <T> ResponseEntity<T> processGet(boolean cacheable, String url, Class<T> responseType,
            Object... uriVariables) throws RestClientException {
        LogEntry entry = begin(url);
        ResponseEntity<T> response = cacheable ? cache.getCache(url, responseType, uriVariables) : null;
        if (response == null) {
            try {
                response = hermesClient.createRestTemplate().getForEntity(url, responseType, uriVariables);
                if (cacheable) {
                    cache.putCache(url, responseType, response, uriVariables);
                }
                end(entry, response);
            } catch (RestClientException e) {
                response = fail(e);
                end(entry, e);
            }
        } else {
            entry.setCached(true);
            end(entry, response);
        }
        return response;
    }

    protected <T> ResponseEntity<T> processPost(String url, @RequestBody T request, Class<T> responseType,
            Object... uriVariables) throws RestClientException {
        LogEntry entry = begin(url);
        ResponseEntity<T> response = null;
        try {
            response = hermesClient.createRestTemplate().postForEntity(url, request, responseType, uriVariables);
            end(entry, response);
        } catch (RestClientException e) {
            response = fail(e);
            end(entry, e);
        }
        return response;
    }

    @SuppressWarnings("java:S2629")
    protected void end(LogEntry entry, ResponseEntity<?> response) {
        entry.setDuration(System.currentTimeMillis() - entry.getDate().getTime());
        entry.setResponseStatus(response.getStatusCode().toString());
        // "Preconditions" and logging arguments should not require evaluation.
        logger.info(MessageFormat.format(CACHED_FROM_TO_PATTERN, entry.getApplicationUrl(), entry.getResponseStatus(),
                entry.isCached() ? CACHED : NOT_CACHED, entry.getDuration()));
    }

    @SuppressWarnings("java:S2629")
    protected void end(LogEntry entry, HttpStatusCodeException e) {
        entry.setDuration(System.currentTimeMillis() - entry.getDate().getTime());
        entry.setResponseStatus(e.getStatusCode().toString());
        entry.setErrorMessage(hermesClient.getErrorMessage(e));
        // "Preconditions" and logging arguments should not require evaluation.
        logger.info(MessageFormat.format(CACHED_FROM_TO_PATTERN, entry.getApplicationUrl(), entry.getErrorMessage(),
                entry.isCached() ? CACHED : NOT_CACHED, entry.getDuration()));
    }

    @SuppressWarnings("java:S2629")
    protected void end(LogEntry entry, RestClientException e) {
        entry.setDuration(System.currentTimeMillis() - entry.getDate().getTime());
        entry.setResponseStatus(e.getClass().getName());
        entry.setErrorMessage(hermesClient.getErrorMessage(e));
        // "Preconditions" and logging arguments should not require evaluation.
        logger.info(MessageFormat.format(CACHED_FROM_TO_PATTERN, entry.getApplicationUrl(), entry.getErrorMessage(),
                entry.isCached() ? CACHED : NOT_CACHED, entry.getDuration()));
    }

    protected <T> ResponseEntity<T> fail(RestClientException e) {
        return ResponseEntity.status(getResponseStatus(e))
                .header(HermesClient.REST_CLIENT_RESPONSE_EXCEPTION_HEADER, HermesClient.encodeExceptionMessage(e))
                .build();
    }

    private String getRemoteApplication(String remoteHost) {
        buildApplicationMap();
        return applications.get(LOCALHOST_ADDRESS.equals(remoteHost) ? LOCALHOST_HOST : remoteHost);
    }

    private String resolveHost(String host) {
        String hostName;
        try {
            InetAddress address = InetAddress.getByName(host);
            hostName = address.getHostName();
        } catch (UnknownHostException e) {
            hostName = host;
        }
        return hostName;
    }

    private void buildApplicationMap() {
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
    private Properties getAllProperties() {
        Properties properties = new Properties();
        MutablePropertySources propertySources = env.getPropertySources();
        StreamSupport.stream(propertySources.spliterator(), false)
                .filter(EnumerablePropertySource.class::isInstance)
                .map(propertySource -> ((EnumerablePropertySource<?>) propertySource).getPropertyNames())
                .flatMap(Arrays::<String>stream)
                .forEach(key -> properties.setProperty(key, env.getProperty(key)));
        return properties;
    }

    private int getResponseStatus(RestClientException e) {
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (e instanceof RestClientResponseException) {
            RestClientResponseException ex = (RestClientResponseException) e;
            status = ex.getRawStatusCode();
        }
        return status;
    }

}
