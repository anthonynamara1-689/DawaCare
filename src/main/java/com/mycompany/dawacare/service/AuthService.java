package com.mycompany.dawacare.service;

import com.mycompany.dawacare.dao.UserDao;
import com.mycompany.dawacare.model.User;
import com.mycompany.dawacare.util.PasswordUtil;
import com.mycompany.dawacare.util.Session;
import java.sql.SQLException;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public User login(String username, String password) throws AuthException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new AuthException("Please enter your username and password.");
        }

        User user;
        try {
            user = userDao.findByUsername(username.trim());
        } catch (SQLException e) {
            throw new AuthException("Cannot reach the database. Please try again.");
        }

        // Same message for "no such user" and "wrong password",
        // so attackers can't discover which usernames exist.
        if (user == null || !passwordMatches(password, user.getPasswordHash())) {
            throw new AuthException("Invalid username or password.");
        }

        switch (user.getStatus()) {
            case PENDING  -> throw new AuthException("Your account is awaiting admin approval.");
            case INACTIVE -> throw new AuthException("This account has been deactivated.");
            case ACTIVE   -> { }
        }

        try {
            userDao.updateLastLogin(user.getUserId());
        } catch (SQLException ignored) {
            // Not critical: login still succeeds
        }

        Session.start(user);
        return user;
    }

    public void logout() {
        Session.end();
    }

    private boolean passwordMatches(String plain, String hash) {
        try {
            return PasswordUtil.verify(plain, hash);
        } catch (IllegalArgumentException e) {
            return false;  // stored value isn't a valid BCrypt hash
        }
    }
    
    public void changePassword(String currentPassword, String newPassword, String confirmPassword)
            throws ServiceException {
        User sessionUser = Session.getUser();
        if (sessionUser == null)
            throw new ServiceException("Please log in again.");
        if (currentPassword == null || currentPassword.isEmpty())
            throw new ServiceException("Please enter your current password.");

        User fresh;   // re-read from the database, in case the hash changed
        try {
            fresh = userDao.findByUsername(sessionUser.getUsername());
        } catch (SQLException e) {
            throw new ServiceException("Cannot reach the database. Please try again.");
        }
        if (fresh == null || !passwordMatches(currentPassword, fresh.getPasswordHash()))
            throw new ServiceException("Your current password is incorrect.");

        PasswordPolicy.validate(newPassword, confirmPassword);
        if (passwordMatches(newPassword, fresh.getPasswordHash()))
            throw new ServiceException("The new password must be different from your current one.");

        try {
            userDao.updatePassword(fresh.getUserId(), PasswordUtil.hash(newPassword));
        } catch (SQLException e) {
            throw new ServiceException("Could not change the password. Please try again.");
        }
    }
}