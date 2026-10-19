package com.m4r10kyou.shortlinkQr;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.repository.ShortLinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration")
public class ShortLinkPersistenceTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShortLinkRepository repository;

    @Test
    void shortLinkIsPersistedAndReadBack() {

        String code ="my-repo";
        String targetUrl = "https://github.com/m4r10kyou/shortlinkQr";

        ShortLink newLink = new ShortLink(code,targetUrl,null,null );

        ShortLink savedLink = repository.save(newLink);

        entityManager.flush();
        entityManager.clear();

        ShortLink retrievedLink = repository.findByCode(savedLink.getCode()).orElse(null);

        assertThat(retrievedLink).isNotNull();
        assertThat(retrievedLink.getCode()).isEqualTo(code);
        assertThat(retrievedLink.getTargetUrl()).isEqualTo(targetUrl);
        assertThat(retrievedLink.getVisitCount()).isEqualTo(0);
        assertThat(retrievedLink).isNotSameAs(savedLink);
    }

    @Test
    void logoBytesSurviveTheRoundTrip() {

        String code ="my-repo-with-logo";
        String targetUrl = "https://github.com/m4r10kyou/shortlinkQr";
        byte[] logo = { (byte) 0x89, 0x50, 0x00, 0x1A, (byte) 0xFF, 0x0D };

        ShortLink newLink = new ShortLink(code,targetUrl,null,null );

        newLink.attachLogo(logo);

        ShortLink savedLink = repository.save(newLink);

        entityManager.flush();
        entityManager.clear();

        ShortLink retrievedLink = repository.findByCode(savedLink.getCode()).orElse(null);

        assertThat(retrievedLink).isNotNull();
        assertThat(retrievedLink.getCode()).isEqualTo(code);
        assertThat(retrievedLink.getTargetUrl()).isEqualTo(targetUrl);
        assertThat(retrievedLink.getVisitCount()).isEqualTo(0);
        assertThat(retrievedLink.getLogoData()).isEqualTo(logo);
        assertThat(retrievedLink).isNotSameAs(savedLink);
    }

    @Test
    void expiresAtSurvivesTheRoundTrip() {

        String code ="my-repo-with-expiration";
        String targetUrl = "https://github.com/m4r10kyou/shortlinkQr";
        Instant expiresAt = Instant.parse("2027-03-15T10:30:45.123456Z");

        ShortLink newLink = new ShortLink(code,targetUrl,expiresAt,null );

        ShortLink savedLink = repository.save(newLink);

        entityManager.flush();
        entityManager.clear();

        ShortLink retrievedLink = repository.findByCode(savedLink.getCode()).orElse(null);

        assertThat(retrievedLink).isNotNull();
        assertThat(retrievedLink.getCode()).isEqualTo(code);
        assertThat(retrievedLink.getTargetUrl()).isEqualTo(targetUrl);
        assertThat(retrievedLink.getVisitCount()).isEqualTo(0);
        assertThat(retrievedLink.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(retrievedLink).isNotSameAs(savedLink);

    }

    @Test
    void codeMustBeUnique() {

        String code ="my-repo-duplicate";
        String targetUrl = "https://github.com/m4r10kyou/shortlinkQr";

        ShortLink newLink = new ShortLink(code,targetUrl,null,null );

        repository.save(newLink);

        ShortLink duplicate = new ShortLink(code,targetUrl,null,null );

        assertThatThrownBy(() -> {
            repository.save(duplicate);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_short_links_code");
    }

}
