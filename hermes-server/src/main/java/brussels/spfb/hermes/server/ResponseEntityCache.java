package brussels.spfb.hermes.server;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ResponseEntityCache {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public class CacheKey {
        private String url;
        // Class is a raw type. References to generic type Class<T> should be
        // parameterized.
        Class responseType;
        private Object[] uriVariables;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("rawtypes")
    public class CacheValue {
        private long timestamp;
        // Class is a raw type. References to generic type Class<T> should be
        // parameterized.
        private ResponseEntity response;
    }

    public static final long INFINITE_LIFETIME = -1;
    public static final long ZERO_LIFETIME = 0;
    public static final long ONE_SECOND_LIFETIME = 1000;
    public static final long ONE_MINUTE_LIFETIME = 60 * ONE_SECOND_LIFETIME;
    public static final long FIVE_MINUTES_LIFETIME = 5 * ONE_MINUTE_LIFETIME;
    public static final long TEN_MINUTES_LIFETIME = 10 * ONE_MINUTE_LIFETIME;
    public static final long THIRTY_MINUTES_LIFETIME = 30 * ONE_MINUTE_LIFETIME;
    public static final long ONE_HOUR_LIFETIME = 60 * ONE_MINUTE_LIFETIME;
    public static final long DEFAULT_LIFETIME = ONE_HOUR_LIFETIME;
    public static final long DEFAULT_PRUNE_DELAY = ONE_HOUR_LIFETIME;

    private static final String BEGIN_CACHE_PRUNE_PATTERN = "Starting {0} cache prune...";
    private static final String END_CACHE_PRUNE_PATTERN = "Removed {0} item(s) from cache {1}.";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConcurrentHashMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();
    private final String name;
    private long lifetime;
    private long pruneDelay;
    private Timer pruneTimer;

    public ResponseEntityCache(String name) {
        this(name, DEFAULT_LIFETIME, DEFAULT_PRUNE_DELAY);
    }

    public ResponseEntityCache(String name, long lifetime, long pruneDelay) {
        this.name = name;
        this.lifetime = lifetime;
        this.pruneDelay = pruneDelay;
        startAutomaticPrune();
    }

    public long getLifetime() {
        return lifetime;
    }

    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    public long getPruneDelay() {
        return pruneDelay;
    }

    public void setPruneDelay(long pruneDelay) {
        this.pruneDelay = pruneDelay;
        startAutomaticPrune();
    }

    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> getCache(String url, Class<T> responseType, Object... uriVariables) {
        CacheKey key = new CacheKey(url, responseType, uriVariables);
        CacheValue value = cache.get(key);
        ResponseEntity<T> response = null;
        if (value != null) {
            long age = getTimestamp() - value.getTimestamp();
            if ((lifetime != INFINITE_LIFETIME) && (age > lifetime)) {
                cache.remove(key);
                response = null;
            } else {
                // Type safety: The expression of type ResponseEntity needs unchecked conversion
                // to conform to ResponseEntity<T>.
                response = value.getResponse();
            }
        }
        return response;
    }

    public <T> void putCache(String url, Class<T> responseType, ResponseEntity<T> response, Object... uriVariables) {
        CacheKey key = new CacheKey(url, responseType, uriVariables);
        CacheValue value = new CacheValue(getTimestamp(), response);
        cache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> void removeCache(Class<T> responseType, Predicate<ResponseEntity<T>> predicate) {
        Iterator<Entry<CacheKey, CacheValue>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<CacheKey, CacheValue> entry = iterator.next();
            // Type safety: The expression of type ResponseEntity needs unchecked conversion
            // to conform to ResponseEntity<T>.
            if (entry.getValue().getResponse().getBody().getClass().equals(responseType)
                    && predicate.test(entry.getValue().getResponse())) {
                iterator.remove();
            }
        }
    }

    public void clearCache() {
        cache.clear();
    }

    private long getTimestamp() {
        return new Date().getTime();
    }

    private void startAutomaticPrune() {
        if (pruneTimer != null) {
            pruneTimer.cancel();
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                pruneCache();
            }
        };
        pruneTimer = new Timer();
        pruneTimer.scheduleAtFixedRate(task, pruneDelay, pruneDelay);
    }

    @SuppressWarnings("java:S2629")
    private void pruneCache() {
        if (lifetime == INFINITE_LIFETIME) {
            return;
        }

        // "Preconditions" and logging arguments should not require evaluation.
        logger.info(MessageFormat.format(BEGIN_CACHE_PRUNE_PATTERN, name));
        int count = 0;
        Iterator<Entry<CacheKey, CacheValue>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<CacheKey, CacheValue> entry = iterator.next();
            long age = getTimestamp() - entry.getValue().getTimestamp();
            if (age > lifetime) {
                iterator.remove();
                count++;
            }
        }
        // "Preconditions" and logging arguments should not require evaluation.
        logger.info(MessageFormat.format(END_CACHE_PRUNE_PATTERN, count, name));
    }

}
