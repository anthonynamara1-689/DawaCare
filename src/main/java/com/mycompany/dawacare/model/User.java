package com.mycompany.dawacare.model;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;

    public User(int userId, String username, String passwordHash, String fullName,
                String email, String phone, Role role, UserStatus status) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }

    public int getUserId()          { return userId; }
    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName()     { return fullName; }
    public String getEmail()        { return email; }
    public String getPhone()        { return phone; }
    public Role getRole()           { return role; }
    public UserStatus getStatus()   { return status; }
}