package onl.gcm.hermes.logging;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    private final Date date = new Date();
    private String remoteHost;
    private String remoteApplication;
    private String requestUrl;
    private String applicationUrl;
    private boolean cached;
    private String responseStatus;
    private String errorMessage;
    private long duration;
    private String requestClientVersion;
    private String clientVersion;

}
