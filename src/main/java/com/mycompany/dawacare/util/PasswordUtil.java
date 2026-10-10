package com.mycompany.dawacare.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Turns a plain password into a secure hash for storing in the database
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    // Checks a typed password against the stored hash (used at login)
    public static boolean verify(String plain, String hash) {
        return BCrypt.checkpw(plain, hash);
    }
}