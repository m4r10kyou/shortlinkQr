package com.m4r10kyou.shortlinkQr.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ActiveProfileGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment env;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        if (env.getActiveProfiles().length == 0) {

            throw new IllegalStateException(
                    "No Spring profile is active. This application does not pick an environment "
                            + "for you: set SPRING_PROFILES_ACTIVE to dev, test or prod, or pass "
                            + "-Dspring-boot.run.profiles when running from Maven.");
        }
    }

    @Override
    public void setEnvironment(Environment environment) {

        this.env = environment;
    }
}
