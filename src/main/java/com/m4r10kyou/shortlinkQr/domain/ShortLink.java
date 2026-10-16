package com.m4r10kyou.shortlinkQr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
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

    @Column(nullable = false)
    private int visitCount;

    @Column(length = 64)
    private String managementTokenHash;


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

        if(hasLogo()) {
            return Arrays.copyOf(this.logoData, this.logoData.length);
        }else{
            return null;
        }
    }

    public int getVisitCount() {
        return visitCount;
    }

    protected ShortLink() {
    }

    public ShortLink(String code, String targetUrl, Instant expiresAt,String managementTokenHash ) {
        this.code = code;
        this.targetUrl = targetUrl;
        this.expiresAt = expiresAt;

        this.createdAt = Instant.now();
        this.visitCount = 0;

        this.managementTokenHash = managementTokenHash;
    }

    public void changeExpiry(Instant expiresAt) {

        Objects.requireNonNull(expiresAt, "Expiration cannot be null");

        this.expiresAt = expiresAt;
    }

    public void removeExpiry(){

        this.expiresAt = null;
    }

    public void changeTargetUrl(String url) {
        Objects.requireNonNull(url, "Target url cannot be null");

        this.targetUrl = url;
    }

    public void attachLogo(byte[] logoData) {

        Objects.requireNonNull(logoData, "Logo cannot be null");

        if (logoData.length == 0) {

            throw new IllegalArgumentException("Logo array cannot be empty");
        }

        this.logoData = Arrays.copyOf(logoData, logoData.length);
    }

    public void removeLogo() {

        this.logoData = null;
    }

    public boolean hasLogo() {

        return null != this.logoData;
    }

    public boolean matchesManagementToken(String candidateHash) {

        if( null == managementTokenHash || null == candidateHash  || candidateHash.isBlank()){

            return false;
        }


        return MessageDigest.isEqual( managementTokenHash.getBytes(StandardCharsets.UTF_8) , candidateHash.getBytes(StandardCharsets.UTF_8));
    }

}
