package com.mycompany.dawacare.util;

import com.mycompany.dawacare.model.Role;
import com.mycompany.dawacare.model.User;

public final class Session {
    private static User currentUser;

    private Session() {}

    public static void start(User user) { currentUser = user; }
    public static void end()            { currentUser = null; }
    public static User getUser()        { return currentUser; }
    public static boolean isLoggedIn()  { return currentUser != null; }
    public static boolean hasRole(Role r) {
        return currentUser != null && currentUser.getRole() == r;
    }
}