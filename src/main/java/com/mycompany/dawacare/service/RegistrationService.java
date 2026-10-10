package com.mycompany.dawacare.service;

import com.mycompany.dawacare.dao.RegistrationDao;
import com.mycompany.dawacare.model.*;
import static com.mycompany.dawacare.model.Role.DOCTOR;
import static com.mycompany.dawacare.model.Role.PATIENT;
import com.mycompany.dawacare.util.PasswordUtil;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class RegistrationService {
    private final RegistrationDao registrationDao = new RegistrationDao();

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
    private static final Pattern EMAIL    = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE    = Pattern.compile("^\\+?[0-9 ]{9,20}$");

    /** Returns a success message to show the user. */
    public String register(RegistrationRequest r) throws ServiceException {
        validate(r);

        UserStatus status = (r.role() == Role.DOCTOR) ? UserStatus.PENDING : UserStatus.ACTIVE;
        String hash = PasswordUtil.hash(r.password());

        try {
            registrationDao.register(r, hash, status);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ServiceException(duplicateMessage(e.getMessage()));
        } catch (SQLException e) {
            throw new ServiceException("Could not create the account. Please try again.");
        }

        return (status == UserStatus.PENDING)
            ? "Account created. An administrator must verify your license before you can log in."
            : "Account created successfully. You can now log in.";
    }

    private void validate(RegistrationRequest r) throws ServiceException {
        if (r.role() == null || r.role() == Role.ADMIN)
            throw new ServiceException("Please choose a valid account type.");
        if (isBlank(r.fullName()) || r.fullName().trim().length() > 120)
            throw new ServiceException("Please enter your full name.");
        if (r.username() == null || !USERNAME.matcher(r.username().trim()).matches())
            throw new ServiceException("Username must be 3-50 letters, numbers or underscores.");
        if (r.password() == null || r.password().length() < 8
                || !r.password().matches(".*[A-Za-z].*") || !r.password().matches(".*[0-9].*"))
            throw new ServiceException("Password must be at least 8 characters with letters and numbers.");
        if (!r.password().equals(r.confirmPassword()))
            throw new ServiceException("Passwords do not match.");
        if (!isBlank(r.email()) && !EMAIL.matcher(r.email().trim()).matches())
            throw new ServiceException("Please enter a valid email address.");
        if (!isBlank(r.phone()) && !PHONE.matcher(r.phone().trim()).matches())
            throw new ServiceException("Please enter a valid phone number.");

        switch (r.role()) {
            case PATIENT -> validatePatient(r);
            case DOCTOR -> {
                if (isBlank(r.licenseNumber()) || r.licenseNumber().trim().length() > 80)
                    throw new ServiceException("Doctors must provide a license number.");
            }
            default -> { }
        }
    }

    private void validatePatient(RegistrationRequest r) throws ServiceException {
        if (isBlank(r.dateOfBirth()))
            throw new ServiceException("Please enter your date of birth (YYYY-MM-DD).");
        try {
            LocalDate dob = LocalDate.parse(r.dateOfBirth().trim());
            if (dob.isAfter(LocalDate.now()) || dob.isBefore(LocalDate.of(1900, 1, 1)))
                throw new ServiceException("Please enter a realistic date of birth.");
        } catch (DateTimeParseException e) {
            throw new ServiceException("Date of birth must be in the format YYYY-MM-DD.");
        }
        if (isBlank(r.gender()))
            throw new ServiceException("Please select a gender.");
        if (!isBlank(r.emergencyContactPhone())
                && !PHONE.matcher(r.emergencyContactPhone().trim()).matches())
            throw new ServiceException("Please enter a valid emergency contact phone.");
    }

    private String duplicateMessage(String mysqlMessage) {
        // MySQL says e.g.: Duplicate entry 'john' for key 'users.username'
        String msg = mysqlMessage == null ? "" : mysqlMessage.toLowerCase();
        int i = msg.indexOf("for key");
        String key = (i >= 0) ? msg.substring(i) : msg;
        if (key.contains("username")) return "That username is already taken.";
        if (key.contains("email"))    return "That email is already registered.";
        if (key.contains("license"))  return "That license number is already registered.";
        return "Some of these details are already registered.";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
