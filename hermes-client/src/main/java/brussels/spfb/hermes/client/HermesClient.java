package brussels.spfb.hermes.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@PropertySource("classpath:hermes.properties")
@PropertySource("classpath:hermes-${spring.profiles.active}.properties")
public abstract class HermesClient {

    public static final String REST_CLIENT_RESPONSE_EXCEPTION_HEADER = "RestClientResponseException";
    public static final String VERSION_HEADER = "version";

    private static final String LF = "\\n";
    private static final String BACKSLASH = "\\";
    private static final String TAB = "\\t";

    @Value("${hermes.server.url}")
    private String hermesServerUrl;

    @Value("${version}")
    private String version;

    @Value("${timeout}")
    private int timeout;

    @Value("${hermes.path.alive}")
    private String hermesAlivePath;

    public String getVersion() {
        return version;
    }

    public RestTemplate createRestTemplate() {
        return new RestTemplate(createClientHttpRequestFactory());
    }

    // Get the embedded exception message in the headers if it is a
    // RestClientResponseException, else get the normal exception message.
    public String getErrorMessage(Exception e) {
        String errorMessage = e.getMessage();
        HttpHeaders headers = null;
        if (e instanceof HttpServerErrorException) {
            HttpServerErrorException ex = (HttpServerErrorException) e;
            headers = ex.getResponseHeaders();
        } else if (e instanceof RestClientResponseException) {
            RestClientResponseException ex = (RestClientResponseException) e;
            headers = ex.getResponseHeaders();
        }
        String headerErrorMessage = headers == null ? null : headers.getFirst(REST_CLIENT_RESPONSE_EXCEPTION_HEADER);
        return headerErrorMessage == null ? errorMessage : headerErrorMessage;
    }

    public boolean isHermesAlive() {
        try {
            process(hermesServerUrl + hermesAlivePath, Void.class);
        } catch (RestClientException e) {
            return false;
        }
        return true;
    }

    public boolean isAlive() {
        try {
            process(hermesServerUrl + getCompleteAlivePath(), Void.class);
        } catch (RestClientException e) {
            return false;
        }
        return true;
    }

    public String encodeExceptionMessage(Exception e) {
        return e.getMessage().replace(LF, BACKSLASH + LF).replace(TAB, BACKSLASH + TAB);
    }

    protected HermesClient() {
        // Protected constructor to prevent instantiation.
    }

    protected <T> T process(String url, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        ResponseEntity<T> response = createRestTemplate().exchange(url, HttpMethod.GET, createRequestEntity(),
                responseType, uriVariables);
        return response.getBody();
    }

    protected String getHermesServerUrl() {
        return hermesServerUrl;
    }

    private ClientHttpRequestFactory createClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(timeout);
        return clientHttpRequestFactory;
    }

    private <T> HttpEntity<T> createRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.addIfAbsent(VERSION_HEADER, getVersion());
        return new HttpEntity<>(null, headers);
    }

    protected abstract String getCompleteAlivePath();

}
