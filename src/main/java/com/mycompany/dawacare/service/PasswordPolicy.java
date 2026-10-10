package com.mycompany.dawacare.service;

public final class PasswordPolicy {

    private PasswordPolicy() {}

    public static void validate(String password, String confirm) throws ServiceException {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*"))
            throw new ServiceException("Password must be at least 8 characters with letters and numbers.");
        if (!password.equals(confirm))
            throw new ServiceException("Passwords do not match.");
    }
}