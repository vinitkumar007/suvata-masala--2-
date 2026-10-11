package com.suvata.masala.service;

/** Admin token galat hai, ya admin feature configure nahi hai. */
public class AdminAccessException extends RuntimeException {

    private final boolean disabled;

    public AdminAccessException(String message, boolean disabled) {
        super(message);
        this.disabled = disabled;
    }

    /** true ho to matlab ADMIN_TOKEN set hi nahi hai. */
    public boolean isDisabled() {
        return disabled;
    }
}
