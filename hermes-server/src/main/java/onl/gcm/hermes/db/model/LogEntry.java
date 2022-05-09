package onl.gcm.hermes.db.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private final Date date = new Date();

    private String remoteHost;

    private String remoteApplication;

    private String requestUrl;

    private String applicationUrl;

    private boolean cached;

    private String responseStatus;

    @Lob
    private String errorMessage;

    private long duration;

    private String requestClientVersion;
    
    private String clientVersion;

}
