package com.m4r10kyou.shortlinkQr;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration")
/* @DataJpaTest brings its own @Transactional, which would wrap every step of
   these tests in one transaction and leave nothing to interleave. These tests
   open and commit their own transactions, in a deliberate order, to reproduce
   what happens when an edit and a visit overlap. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class VisitCountConcurrencyTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private ShortLinkRepository repository;

    private TransactionTemplate editTransaction;
    private TransactionTemplate visitTransaction;

    @BeforeEach
    void setUpTransactionTemplate() {

        editTransaction = new TransactionTemplate(platformTransactionManager);

        visitTransaction = new TransactionTemplate(platformTransactionManager);
        visitTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void editingALink_whileAVisitIsRegistered_doesNotOverwriteTheCounter() {

        String code = "my-repo";
        String newTargetUrl = "https://github.com/m4r10kyou/shortlinkQr";
        String oldTargetUrl = "https://www.google.com";

        ShortLink newLink = new ShortLink(code, oldTargetUrl, null, null);

        repository.save(newLink);

        editTransaction.executeWithoutResult(status -> {

            ShortLink slFound = repository.findByCode(code).orElseThrow();

            visitTransaction.executeWithoutResult(inner -> repository.registerVisitCount(code));

            slFound.changeTargetUrl(newTargetUrl);
        });

        ShortLink shortLink = repository.findByCode(code).orElseThrow();

        assertThat(shortLink.getVisitCount()).isEqualTo(1);
        assertThat(shortLink.getTargetUrl()).isEqualTo(newTargetUrl);
    }
}
