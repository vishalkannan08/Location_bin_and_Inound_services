package com.company.wms.locationbin.exception;

/** Maps to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    private final String code;

    public DuplicateResourceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
