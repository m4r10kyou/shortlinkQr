package com.m4r10kyou.shortlinkQr.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ActiveProfileGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment env;

    /**
     * Allowed Spring Boot active profiles for application startup.
     * This acts as a safeguard to prevent the application from booting
     * with unknown, unconfigured, or default environments.
     */
    private static final List<String> ALLOWED_PROFILES = List.of("test", "dev", "integration", "prod");

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        String[] activeProfiles = env.getActiveProfiles();
        String validProfiles = String.join(", ", ALLOWED_PROFILES);

        if (activeProfiles.length == 0) {

            throw new IllegalStateException(
                    "No Spring profile is active. This application does not pick an environment "
                            + "for you: set SPRING_PROFILES_ACTIVE to "+ validProfiles +" or pass "
                            + "-Dspring-boot.run.profiles when running from Maven.");
        }

        boolean hasUnknownProfile = !Arrays.stream(activeProfiles)
                .allMatch(ALLOWED_PROFILES::contains);

        if (hasUnknownProfile){

            throw new IllegalStateException(
                    "Spring profile active "+ Arrays.toString(activeProfiles)+" is not valid. "
                            + "This application requires a recognized environment: "
                            + "set SPRING_PROFILES_ACTIVE to "+ validProfiles +" "
                            + "or pass -Dspring-boot.run.profiles when running from Maven.");
        }
    }

    @Override
    public void setEnvironment(Environment environment) {

        this.env = environment;
    }
}
