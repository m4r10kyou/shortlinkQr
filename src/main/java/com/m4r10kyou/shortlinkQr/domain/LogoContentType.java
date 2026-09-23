package com.m4r10kyou.shortlinkQr.domain;

public enum LogoContentType {
    PNG("image/png"),
    JPEG("image/jpeg"),
    GIF("image/gif");

    private final String mimeType;

    LogoContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public static LogoContentType fromMimeType(String mimeType) {
        for (LogoContentType type : values()) {
            if (type.getMimeType().equalsIgnoreCase(mimeType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type not Supported " + mimeType);
    }
}