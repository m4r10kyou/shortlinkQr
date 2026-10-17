package com.m4r10kyou.shortlinkQr.repository;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ShortLinkRepositoryTest {

    private final String hashToken = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";

    @Autowired
    private ShortLinkRepository repository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUpShortLinkFixtures() {

        ShortLink slWinterDimondi = new ShortLink("DimondiWinter26",
                "http://www.dimondi-restaurant.com/2026/NovFeb/menu", null,hashToken);

        ShortLink slSummerDimondi = new ShortLink("DimondiSummer26",
                "http://www.dimondi-restaurant.com/2026/JunSep/menu", null,hashToken);

        List<ShortLink> slEntities = List.of(slSummerDimondi, slWinterDimondi);

        repository.saveAllAndFlush(slEntities);
    }

    @Test
    void existsByCode_withExistingCode_returnsTrue() {

        boolean existMenuWinter = repository.existsByCode("DimondiWinter26");

        assertThat(existMenuWinter).isTrue();
    }

    @Test
    void existsByCode_withUnknownCode_returnsFalse() {

        boolean existMenuAutumn = repository.existsByCode("DimondiAutumn26");

        assertThat(existMenuAutumn).isFalse();
    }

    @Test
    void findByCode_withExistingCode_returnsTheLink() {

        Optional<ShortLink> found = repository.findByCode("DimondiSummer26");

        assertThat(found).isPresent();
        assertThat(found.get().getTargetUrl()).isEqualTo("http://www.dimondi-restaurant.com/2026/JunSep/menu");
    }


    @Test
    void findByCode_withUnknownCode_returnsEmpty() {

        Optional<ShortLink> found = repository.findByCode("DimondiSpring26");

        assertThat(found).isEmpty();
    }

    @Test
    void registerVisitCount_withExistingCode_increasesCountByOne() {

        Optional<ShortLink> found = repository.findByCode("DimondiSummer26");
        int visitsOrigin = found.orElseThrow().getVisitCount();

        repository.registerVisitCount("DimondiSummer26");

        Optional<ShortLink> afterVisit = repository.findByCode("DimondiSummer26");

        assertThat(afterVisit.orElseThrow().getVisitCount()).isEqualTo(visitsOrigin + 1);
    }

    @Test
    void findByCode_returnsALinkThatStillMatchesItsManagementToken(){

        entityManager.clear();
        Optional<ShortLink> found = repository.findByCode("DimondiSummer26");

        assertThat(found.orElseThrow().matchesManagementToken(hashToken)).isTrue();
    }

}
