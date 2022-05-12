package onl.gcm.hermes.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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

    private static final String HERMES_PROPERTIES = "/hermes.properties";
    private static final String ENVIRONMENT_PROPERTIES = "/hermes-" + System.getProperty("spring.profiles.active")
            + ".properties";
    private static final String PROPERTY_PATTERN = "[${}]";
    private static final String EMPTY_STRING = "";
    private static final String LF = "\\n";
    private static final String BACKSLASH = "\\";
    private static final String TAB = "\\t";

    @Value("${hermes.server.url}")
    protected String hermesServerUrl;

    @Value("${hermes.client.version}")
    protected String version;

    @Value("${http.request.connect.timeout}")
    protected int httpRequestConnectTimeout;

    @Value("${http.request.read.timeout}")
    protected int httpRequestReadTimeout;

    @Value("${hermes.server.path.alive}")
    protected String hermesServerAlivePath;

    public String getVersion() {
        return version;
    }

    // Initialization for non Spring projects.
    @SuppressWarnings({ "java:S112", "java:S3011" })
    // Generic exceptions should never be thrown.
    public void initialize() throws IOException, IllegalArgumentException, RuntimeException {
        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream(HERMES_PROPERTIES));
        Properties environmentProperties = new Properties();
        environmentProperties.load(getClass().getResourceAsStream(ENVIRONMENT_PROPERTIES));
        environmentProperties.forEach(properties::put);

        // Using reflection to detect @Value fields and assign them to their property
        // value.
        getAllFields(getClass()).stream().forEach(field -> {
            Value value = field.getAnnotation(Value.class);
            if (value != null) {
                String property = value.value().replaceAll(PROPERTY_PATTERN, EMPTY_STRING);
                try {
                    if (field.getType().equals(int.class)) {
                        // Reflection should not be used to increase accessibility of classes, methods,
                        // or fields.
                        field.set(this, Integer.parseInt(properties.getProperty(property)));
                    } else if (field.getType().equals(long.class)) {
                        // Reflection should not be used to increase accessibility of classes, methods,
                        // or fields.
                        field.set(this, Long.parseLong(properties.getProperty(property)));
                    } else {
                        // Reflection should not be used to increase accessibility of classes, methods,
                        // or fields.
                        field.set(this, properties.get(property));
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e); // Generic exceptions should never be thrown.
                }
            }
        });
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

    @SuppressWarnings("java:S5361")
    public static String encodeExceptionMessage(Exception e) {
        // "String#replace" should be preferred to "String#replaceAll".
        return e.getMessage().replaceAll(LF, BACKSLASH + LF).replaceAll(TAB, BACKSLASH + TAB);
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

    // https://www.baeldung.com/java-reflection-class-fields
    @SuppressWarnings("rawtypes")
    // Raw types should not be used.
    private List<Field> getAllFields(Class clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }

        List<Field> result = new ArrayList<>(getAllFields(clazz.getSuperclass()));
        List<Field> filteredFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers()) || Modifier.isProtected(f.getModifiers()))
                .collect(Collectors.toList());
        result.addAll(filteredFields);
        return result;
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
