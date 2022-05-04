package brussels.spfb.hermes.controller;

import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import brussels.spfb.hermes.client.HermesClient;
import brussels.spfb.hermes.logging.LogEntry;
import brussels.spfb.hermes.server.ResponseEntityCache;

public class HermesController {

    private static final String FROM_TO_PATTERN = "{0} -> {1}";
    private static final String CACHED_FROM_TO_PATTERN = "{0} -> {1} ({2}{3} ms)";
    private static final String VERSION_MISMATCH_PATTERN = "{0} is using HermesClient {1} instead of {2}!";
    private static final String CACHED = "cached, ";
    private static final String NOT_CACHED = "";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ResponseEntityCache cache = new ResponseEntityCache(getClass().getName());

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private HermesClient hermesClient;

    protected void setCacheLifetime(long lifetime) {
        cache.setLifetime(lifetime);
    }

    protected void setCachePruneDelay(long pruneDelay) {
        cache.setPruneDelay(pruneDelay);
    }

    @SuppressWarnings("java:S2629")
    protected LogEntry begin(String url) {
        LogEntry entry = new LogEntry();
        UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
        String path = builder.buildAndExpand().getPath();
        String requestClientVersion = httpServletRequest.getHeader(HermesClient.VERSION_HEADER);
        String currentClientVersion = hermesClient.getVersion();
        entry.setRequestUrl(path);
        entry.setApplicationUrl(url);
        entry.setRequestClientVersion(requestClientVersion);
        entry.setClientVersion(currentClientVersion);
        // "Preconditions" and logging arguments should not require evaluation.
        logger.info(MessageFormat.format(FROM_TO_PATTERN, path, url));
        if (!currentClientVersion.equals(requestClientVersion)) {
            // "Preconditions" and logging arguments should not require evaluation.
            logger.warn(MessageFormat.format(VERSION_MISMATCH_PATTERN, path, requestClientVersion,
                    currentClientVersion));
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
        return ResponseEntity.internalServerError()
                .header(HermesClient.REST_CLIENT_RESPONSE_EXCEPTION_HEADER, hermesClient.getErrorMessage(e)).build();
    }

}
