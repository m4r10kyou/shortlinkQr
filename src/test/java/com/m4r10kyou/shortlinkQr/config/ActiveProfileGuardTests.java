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

        var setup = stubEnvironmentAndProfile();

        assertThatThrownBy(() -> setup.guard().postProcessBeanFactory(setup.beanFactory()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_PROFILES_ACTIVE");
    }

    @Test
    void postProcessBeanFactory_withActiveProfileTest_doesNotThrowAnyException() {

        var setup = stubEnvironmentAndProfile("test");

        assertThatCode(() ->setup.guard().postProcessBeanFactory(setup.beanFactory())).doesNotThrowAnyException();

    }

    @Test
    void postProcessBeanFactory_withActiveProfileProd_doesNotThrowAnyException(){

        var setup = stubEnvironmentAndProfile("prod");

        assertThatCode(() ->setup.guard().postProcessBeanFactory(setup.beanFactory())).doesNotThrowAnyException();
    }

    @Test
    void postProcessBeanFactory_withActiveProfileMisspellingProd_throwsIllegalStateException(){

        var setup = stubEnvironmentAndProfile("prd");

        assertThatThrownBy(() -> setup.guard().postProcessBeanFactory(setup.beanFactory()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prd");
    }

    @Test
    void postProcessBeanFactory_withAnUnknownProfileAmongValidOnes_throwsIllegalStateException(){

        var setup = stubEnvironmentAndProfile("prd","prod");

        assertThatThrownBy(() -> setup.guard().postProcessBeanFactory(setup.beanFactory()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prd");
    }

    private GuardSetup stubEnvironmentAndProfile(String... activeProfiles) {

        Environment environment = mock(Environment.class);
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);

        when(environment.getActiveProfiles()).thenReturn(activeProfiles);

        ActiveProfileGuard guard = new ActiveProfileGuard();
        guard.setEnvironment(environment);

        return new GuardSetup(guard, beanFactory);
    }

    private record GuardSetup(ActiveProfileGuard guard, ConfigurableListableBeanFactory beanFactory) {}
}
