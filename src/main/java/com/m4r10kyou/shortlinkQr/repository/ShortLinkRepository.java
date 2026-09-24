package com.m4r10kyou.shortlinkQr.repository;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    boolean existsByCode(String code);

    Optional<ShortLink> findByCode(String code);
}
