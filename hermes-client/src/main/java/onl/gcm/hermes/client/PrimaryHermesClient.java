package onl.gcm.hermes.client;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
// A primary component is mandatory because Spring will not be able to choose
// between all HermesClient beans.
public class PrimaryHermesClient extends HermesClient {

    @Override
    public String getCompleteAlivePath() {
        return null;
    }

}
