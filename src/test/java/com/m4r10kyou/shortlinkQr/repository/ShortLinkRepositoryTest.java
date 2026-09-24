package com.m4r10kyou.shortlinkQr.repository;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ShortLinkRepositoryTest {

    @Autowired
    ShortLinkRepository repository;

    @BeforeEach
    void createShortLinkMocks(){

        ShortLink slWinterDimondi = new ShortLink("DimondiWinter26",
                "http://www.dimondi-restaurant.com/2026/NovFeb/menu",null);

        ShortLink slSummerDimondi = new ShortLink("DimondiSummer26",
                "http://www.dimondi-restaurant.com/2026/JunSep/menu",null);

        List<ShortLink> slEntities = List.of(slSummerDimondi, slWinterDimondi);

        repository.saveAllAndFlush(slEntities);
    }

    @Test
    void returnsTrueWhenCodeExists(){

        boolean existMenuWinter = repository.existsByCode("DimondiWinter26");

        assertThat(existMenuWinter).isTrue();
    }

    @Test
    void returnsFalseWhenCodeDoesNotExist(){

        boolean existMenuAutumn = repository.existsByCode("DimondiAutumn26");

        assertThat(existMenuAutumn).isFalse();
    }

    @Test
    void findsLinkByItsCode(){

        Optional<ShortLink> found = repository.findByCode("DimondiSummer26");

        assertThat(found).isPresent();
        assertThat(found.get().getTargetUrl()).isEqualTo("http://www.dimondi-restaurant.com/2026/JunSep/menu");
    }


    @Test
    void returnsEmptyWhenCodeNotFound(){

        Optional<ShortLink> found = repository.findByCode("DimondiSpring26");

        assertThat(found).isEmpty();
    }

}
