package onl.gcm.hermes.client;

import java.io.IOException;
import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

// https://stackoverflow.com/questions/17659875/autowired-and-static-method
@Component
public class SpringUtils {

    @Autowired
    private ApplicationContext applicationContext;

    private static SpringUtils instance;

    private HashMap<Class<?>, Object> beans;

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> requiredType) {
        T bean = null;
        if (instance == null) {
            instance = new SpringUtils();
        }
        if (instance.applicationContext == null) {
            if (instance.beans == null) {
                instance.beans = new HashMap<>();
            }
            // Type safety: Unchecked cast from Object to T.
            bean = (T) instance.beans.get(requiredType);
            if (bean == null) {
                try {
                    bean = requiredType.newInstance();
                    if (bean instanceof HermesClient) {
                        HermesClient hermesClient = (HermesClient) bean;
                        hermesClient.initialize();
                    }
                    instance.beans.put(requiredType, bean);
                } catch (InstantiationException | IllegalAccessException | IOException | RuntimeException e) {
                    // Nothing to do.
                }
            }
        } else {
            bean = instance.applicationContext.getBean(requiredType);
        }
        return bean;
    }

    private static void setInstance(SpringUtils instance) {
        SpringUtils.instance = instance;
    }

    @PostConstruct
    private void init() {
        setInstance(this);
    }

}
