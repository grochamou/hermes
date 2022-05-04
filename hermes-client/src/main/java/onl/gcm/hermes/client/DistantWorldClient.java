package onl.gcm.hermes.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import onl.gcm.hermes.dto.DistantWorldDTO;

@Component
public class DistantWorldClient extends HermesClient {

    @Value("${distantworld.path.alive}")
    private String alivePath;

    @Value("${distantworld.path.test}")
    private String testPath;

    @Value("${distantworld.path.nocontent}")
    private String noContentPath;

    @Value("${distantworld.path.notfound}")
    private String notFoundPath;

    @Value("${distantworld.path.crash}")
    private String crashPath;

    @Value("${distantworld.path}")
    private String distantWorldPath;

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
        return process(getHermesServerUrl() + distantWorldPath + testPath, DistantWorldDTO.class, id);
    }

    public void getNoContent() throws RestClientException {
        process(getHermesServerUrl() + distantWorldPath + noContentPath, Void.class);
    }

    public void getNotFound() throws RestClientException {
        process(getHermesServerUrl() + distantWorldPath + notFoundPath, Void.class);
    }

    public void getCrash() throws RestClientException {
        process(getHermesServerUrl() + distantWorldPath + crashPath, Void.class);
    }

}
