package onl.gcm.hermes.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import onl.gcm.hermes.dto.DistantWorldDTO;

@Component
public class DistantWorldClient extends HermesClient {

    @Value("${distantworld.path.alive}")
    protected String alivePath;

    @Value("${distantworld.path.test}")
    protected String testPath;

    @Value("${distantworld.path.nocontent}")
    protected String noContentPath;

    @Value("${distantworld.path.notfound}")
    protected String notFoundPath;

    @Value("${distantworld.path.crash}")
    protected String crashPath;

    @Value("${distantworld.path}")
    protected String distantWorldPath;

    public String getAlivePath() {
        return alivePath;
    }

    @Override
    public String getCompleteAlivePath() {
        return distantWorldPath + alivePath;
    }

    public String getTestPath() {
        return testPath;
    }

    public String getNoContentPath() {
        return noContentPath;
    }

    public String getNotFoundPath() {
        return notFoundPath;
    }

    public String getCrashPath() {
        return crashPath;
    }

    public DistantWorldDTO getTest(String id) throws RestClientException {
        return process(getHermesServerUrl() + distantWorldPath + testPath, HttpMethod.GET, DistantWorldDTO.class, null,
                id);
    }

    public void getNoContent() throws RestClientException {
        process(getHermesServerUrl() + distantWorldPath + noContentPath, HttpMethod.GET, Void.class, null);
    }

    public void getNotFound() throws RestClientException {
        process(getHermesServerUrl() + distantWorldPath + notFoundPath, HttpMethod.GET, Void.class, null);
    }

    public void getCrash() throws RestClientException {
        process(getHermesServerUrl() + distantWorldPath + crashPath, HttpMethod.GET, Void.class, null);
    }

}
