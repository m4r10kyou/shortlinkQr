package com.m4r10kyou.shortlinkQr.repository;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    boolean existsByCode(String code);

    Optional<ShortLink> findByCode(String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ShortLink s SET s.visitCount = s.visitCount + 1 WHERE s.code = :code")
    void registerVisitCount(@Param("code") String code);
}
