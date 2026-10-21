package com.m4r10kyou.shortlinkQr.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveProfileGuardTests {

    @Test
    void postProcessBeanFactory_withNoActiveProfile_throwsIllegalStateException() {

        Environment environment = mock(Environment.class);
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);

        when(environment.getActiveProfiles()).thenReturn(new String[0]);

        ActiveProfileGuard guard = new ActiveProfileGuard();
        guard.setEnvironment(environment);

        assertThatThrownBy(() -> guard.postProcessBeanFactory(beanFactory))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_PROFILES_ACTIVE");
    }

    @Test
    void postProcessBeanFactory_withActiveProfileTest_desNotThrowAnyException() {

        Environment environment = mock(Environment.class);
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);

        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        ActiveProfileGuard guard = new ActiveProfileGuard();
        guard.setEnvironment(environment);

        assertThatCode(() ->guard.postProcessBeanFactory(beanFactory)).doesNotThrowAnyException();

    }
}
