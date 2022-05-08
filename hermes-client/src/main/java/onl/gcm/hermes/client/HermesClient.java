package onl.gcm.hermes.client;

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

    public static final String EXCEPTION_MESSAGE_HEADER = "Exception.message";
    public static final String HERMES_CLIENT_VERSION_HEADER = "HermesClient.version";

    private static final String LF = "\\n";
    private static final String BACKSLASH = "\\";
    private static final String TAB = "\\t";

    @Value("${hermes.server.url}")
    private String hermesServerUrl;

    @Value("${hermes.client.version}")
    private String version;

    @Value("${http.request.connect.timeout}")
    private int httpRequestConnectTimeout;

    @Value("${http.request.read.timeout}")
    private int httpRequestReadTimeout;

    @Value("${hermes.server.path.alive}")
    private String hermesServerAlivePath;

    public String getVersion() {
        return version;
    }

    public int getHttpRequestConnectTimeout() {
        return httpRequestConnectTimeout;
    }

    public void setHttpRequestConnectTimeout(int httpRequestConnectTimeout) {
        this.httpRequestConnectTimeout = httpRequestConnectTimeout;
    }

    public int getHttpRequestReadTimeout() {
        return httpRequestReadTimeout;
    }

    public void setHttpRequestReadTimeout(int httpRequestReadTimeout) {
        this.httpRequestReadTimeout = httpRequestReadTimeout;
    }

    public static String encodeExceptionMessage(Exception e) {
        // "String#replace" should be preferred to "String#replaceAll".
        return e.getMessage().replace(LF, BACKSLASH + LF).replace(TAB, BACKSLASH + TAB);
    }

    // Get the embedded exception message in the headers if it is a
    // HttpServerErrorException or a RestClientResponseException, else get the
    // normal exception message.
    public static String getErrorMessage(Exception e) {
        String message = e.getMessage();
        HttpHeaders headers = null;
        if (e instanceof HttpServerErrorException) {
            HttpServerErrorException ex = (HttpServerErrorException) e;
            headers = ex.getResponseHeaders();
        } else if (e instanceof RestClientResponseException) {
            RestClientResponseException ex = (RestClientResponseException) e;
            headers = ex.getResponseHeaders();
        }
        String messageInHeader = headers == null ? null : headers.getFirst(EXCEPTION_MESSAGE_HEADER);
        return messageInHeader == null ? message : messageInHeader;
    }

    public RestTemplate createRestTemplate() {
        return new RestTemplate(createClientHttpRequestFactory());
    }

    public boolean isHermesServerAlive() {
        try {
            process(hermesServerUrl + hermesServerAlivePath, Void.class);
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

    protected HermesClient() {
        // Protected constructor to prevent manual instantiation.
    }

    private ClientHttpRequestFactory createClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(httpRequestConnectTimeout);
        clientHttpRequestFactory.setReadTimeout(httpRequestReadTimeout);
        return clientHttpRequestFactory;
    }

    protected String getHermesServerUrl() {
        return hermesServerUrl;
    }

    protected <T> T process(String url, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        ResponseEntity<T> response = createRestTemplate().exchange(url, HttpMethod.GET, createRequestEntity(),
                responseType, uriVariables);
        return response.getBody();
    }

    private <T> HttpEntity<T> createRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HERMES_CLIENT_VERSION_HEADER, getVersion());
        return new HttpEntity<>(null, headers);
    }

    protected abstract String getCompleteAlivePath();

}
