package brussels.spfb.hermes.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@PropertySource("classpath:hermes.properties")
@PropertySource("classpath:hermes-${spring.profiles.active}.properties")
public abstract class HermesClient {

    public static final String REST_CLIENT_RESPONSE_EXCEPTION_HEADER = "RestClientResponseException";
    public static final String VERSION_HEADER = "version";

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
        if (e instanceof RestClientResponseException) {
            RestClientResponseException ex = (RestClientResponseException) e;
            HttpHeaders headers = ex.getResponseHeaders();
            if (headers != null) {
                List<String> restClientResponseException = headers.get(REST_CLIENT_RESPONSE_EXCEPTION_HEADER);
                if (restClientResponseException != null) {
                    errorMessage = restClientResponseException.get(0);
                }
            }
        }
        return errorMessage;
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
