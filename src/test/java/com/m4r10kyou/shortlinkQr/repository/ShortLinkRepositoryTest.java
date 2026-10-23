package com.m4r10kyou.shortlinkQr.repository;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShortLinkRepositoryTest {

    private final String hashToken = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";

    @Autowired
    private ShortLinkRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

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

    /*
     * The only assertion in this class about its own configuration rather
     * than about the repository. It exists because nothing else would
     * notice if the annotations above were removed: the suite would go
     * green again against an anonymous database that
     * @AutoConfigureTestDatabase invents, which is exactly the state this
     * class was in until somebody looked at the log.
     *
     * The name is what discriminates. A replaced datasource is named with a
     * UUID; the configured one is "shortlink". This does not assert the
     * PostgreSQL compatibility mode, because H2 reports the URL without its
     * settings. The mode lives in application-test.properties; all this
     * proves is that the test reached that file.
     */
    @Test
    void dataSource_withTheTestProfileActive_isTheConfiguredOne() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();

            assertThat(jdbcUrl)
                    .as("DataSource uses configured 'shortlink' database instead of generated UUID")
                    .contains("shortlink");
        }
    }

}
