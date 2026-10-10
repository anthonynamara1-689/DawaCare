package com.mycompany.dawacare.service;

import com.mycompany.dawacare.dao.MedicationDao;
import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.util.Session;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Objects;

public class MedicationService {
    private static final String FAIL = "Something went wrong talking to the database. Please try again.";
    private final MedicationDao medicationDao = new MedicationDao();

    public List<Medication> getAll() throws ServiceException {
        requireAdmin();
        try {
            return medicationDao.findAll();
        } catch (SQLException e) {
            throw new ServiceException("Could not load medications.");
        }
    }

    public List<CategoryOption> getCategories() throws ServiceException {
        requireAdmin();
        try {
            return medicationDao.findCategories();
        } catch (SQLException e) {
            throw new ServiceException("Could not load categories.");
        }
    }

    /** Adds a new medication (id == null) or updates an existing one. */
    public void save(Medication input) throws ServiceException {
        User admin = requireAdmin();

        String name = required(input.name(), "Name", 150);
        String form = required(input.dosageForm(), "Dosage form", 50);
        String strength = required(input.strength(), "Strength", 50);
        String generic = optional(input.genericName(), "Generic name", 150);
        String manufacturer = optional(input.manufacturer(), "Manufacturer", 150);
        String description = optional(input.description(), "Description", 2000);

        Medication clean = new Medication(input.medicationId(), input.categoryId(), null,
                name, generic, form, strength, manufacturer, description, 0);

        try {
            if (clean.medicationId() == null) {
                medicationDao.insert(clean, admin.getUserId());
                return;
            }

            Medication current = medicationDao.findById(clean.medicationId());
            if (current == null)
                throw new ServiceException("This medication no longer exists.");

            boolean identityChanged = !Objects.equals(current.name(), name)
                    || !Objects.equals(current.dosageForm(), form)
                    || !Objects.equals(current.strength(), strength);
            if (current.usageCount() > 0 && identityChanged)
                throw new ServiceException("This medication is already used in prescriptions, so its name, "
                        + "form and strength can't change. Add a new medication instead.");

            medicationDao.update(clean, admin.getUserId());
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ServiceException("A medication with this name, strength and form already exists.");
        } catch (SQLException e) {
            throw new ServiceException(FAIL);
        }
    }

    public void delete(int medicationId) throws ServiceException {
        User admin = requireAdmin();
        try {
            Medication current = medicationDao.findById(medicationId);
            if (current == null)
                throw new ServiceException("This medication no longer exists.");
            if (current.usageCount() > 0)
                throw new ServiceException("This medication is used in " + current.usageCount()
                        + " prescription item(s) and can't be deleted.");
            if (medicationDao.delete(medicationId, admin.getUserId()) == 0)
                throw new ServiceException("This medication could not be deleted.");
        } catch (SQLException e) {
            throw new ServiceException(FAIL);
        }
    }

    // ===================== Helpers =====================

    private User requireAdmin() throws ServiceException {
        User u = Session.getUser();
        if (u == null || u.getRole() != Role.ADMIN)
            throw new ServiceException("Only administrators can do this.");
        return u;
    }

    private static String required(String value, String field, int max) throws ServiceException {
        if (value == null || value.isBlank())
            throw new ServiceException(field + " is required.");
        String v = value.trim();
        if (v.length() > max)
            throw new ServiceException(field + " must be " + max + " characters or fewer.");
        return v;
    }

    private static String optional(String value, String field, int max) throws ServiceException {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (v.length() > max)
            throw new ServiceException(field + " must be " + max + " characters or fewer.");
        return v;
    }
}