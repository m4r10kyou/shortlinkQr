package com.m4r10kyou.shortlinkQr;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.repository.ShortLinkRepository;
import com.m4r10kyou.shortlinkQr.service.CreatedShortLink;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(properties = "shortlink.base-url=http://short.test")
@Testcontainers
@ActiveProfiles("integration")
public class ShortLinkDeletePersistenceTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ShortLinkService shortLinkService;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Test
    void deleteShortLink_withAValidToken_persistsTheDeletion() {

        String code = "repo2delete";
        String targetUrl = "https://github.com/m4r10kyou/shortlinkQr";
        String mngToken;

        CreatedShortLink createdShortLink = shortLinkService.createLink(targetUrl, code, null);
        mngToken = createdShortLink.rawToken();

        shortLinkService.deleteShortLink(code, mngToken);

        ShortLink deletedShortLink = shortLinkRepository.findByCode(code).orElseThrow();

        assertThat(deletedShortLink.isDeleted()).isTrue();
    }
}
