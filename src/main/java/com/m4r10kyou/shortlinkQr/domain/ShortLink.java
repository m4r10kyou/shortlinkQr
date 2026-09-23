package com.m4r10kyou.shortlinkQr.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "short_links")
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 2048)
    private String targetUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant expiresAt;

    @Lob
    private byte[] logoData;

    @Enumerated(EnumType.STRING)
    private LogoContentType logoContentType;

    @Column(nullable = false)
    private int visitCount;


    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public byte[] getLogoData() {
        return logoData;
    }

    public LogoContentType getLogoContentType() {
        return logoContentType;
    }

    public int getVisitCount() {
        return visitCount;
    }

    protected ShortLink() {
    }

    public ShortLink(String code, String targetUrl, Instant expiresAt) {
        this.code = code;
        this.targetUrl = targetUrl;
        this.expiresAt = expiresAt;

        this.createdAt = Instant.now();
        this.visitCount = 0;
    }

    public void changeExpiry(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void changeTargetUrl(String url){

        this.targetUrl = url;
    }

    public void attachLogo(byte[] logoData, LogoContentType contentType){

        Objects.requireNonNull(logoData, "Logo cannot be null");
        Objects.requireNonNull(contentType, "ContentType cannot be null");

        this.logoData = logoData;
        this.logoContentType = contentType;
    }

    public void removeLogo(){

        this.logoContentType = null;
        this.logoData = null;
    }

    public void recordVisit(){
        this.visitCount++;
    }

    public boolean isExpired(){
        Instant expiration = this.expiresAt;

        if(null == expiration){

            return false;
        }

        return Instant.now().isAfter(expiration);
    }

}
