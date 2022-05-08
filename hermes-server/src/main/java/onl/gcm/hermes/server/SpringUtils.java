package onl.gcm.hermes.server;

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

    public static <T> T getBean(Class<T> requiredType) {
        return instance.applicationContext.getBean(requiredType);
    }

    private static void setInstance(SpringUtils instance) {
        SpringUtils.instance = instance;
    }

    @PostConstruct
    private void init() {
        setInstance(this);
    }

}
