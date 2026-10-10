package com.mycompany.dawacare.service;

import com.mycompany.dawacare.dao.CareLinkDao;
import com.mycompany.dawacare.dao.ProfileDao;
import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.util.Session;
import java.sql.SQLException;
import java.util.List;

public class CareTeamService {
    private static final String FAIL = "Something went wrong talking to the database. Please try again.";

    private final CareLinkDao linkDao = new CareLinkDao();
    private final ProfileDao profileDao = new ProfileDao();

    // ===================== Patient side =====================

    public List<CareLink> getMyDoctors() throws ServiceException {
        int patientId = currentPatientId();
        return db(() -> linkDao.findDoctorsOfPatient(patientId), "Could not load your doctors.");
    }

    public List<DoctorOption> getAvailableDoctors() throws ServiceException {
        int patientId = currentPatientId();
        return db(() -> linkDao.findAvailableDoctors(patientId), "Could not load the list of doctors.");
    }

    public void addDoctor(int doctorId) throws ServiceException {
        int patientId = currentPatientId();
        if (!db(() -> linkDao.isAvailableDoctor(doctorId), FAIL))
            throw new ServiceException("That doctor is not available.");

        String status = db(() -> linkDao.doctorLinkStatus(doctorId, patientId), FAIL);
        if ("ACTIVE".equals(status))
            throw new ServiceException("This doctor is already on your care team.");

        if (status == null) {
            db(() -> linkDao.insertDoctorLink(doctorId, patientId), FAIL);
        } else {
            db(() -> linkDao.reactivateDoctorLink(doctorId, patientId), FAIL);  // was ENDED before
        }
    }

    public void removeDoctor(int linkId) throws ServiceException {
        int patientId = currentPatientId();
        if (db(() -> linkDao.endDoctorLink(linkId, patientId), FAIL) == 0)
            throw new ServiceException("This doctor is no longer on your care team.");
    }

    public List<CareLink> getMyCaregivers() throws ServiceException {
        int patientId = currentPatientId();
        return db(() -> linkDao.findCaregiversOfPatient(patientId), "Could not load your caregivers.");
    }

    public void approveCaregiver(int linkId) throws ServiceException {
        int patientId = currentPatientId();
        if (db(() -> linkDao.approveCaregiver(linkId, patientId), FAIL) == 0)
            throw new ServiceException("This request is no longer pending.");
    }

    /** Declines a pending request or removes an active caregiver. */
    public void removeCaregiver(int linkId) throws ServiceException {
        int patientId = currentPatientId();
        if (db(() -> linkDao.revokeByPatient(linkId, patientId), FAIL) == 0)
            throw new ServiceException("This caregiver is no longer linked to you.");
    }

    // ===================== Caregiver side =====================

    public List<CareLink> getMyPatients() throws ServiceException {
        int caregiverId = currentCaregiverId();
        return db(() -> linkDao.findPatientsOfCaregiver(caregiverId), "Could not load your patients.");
    }

    public void requestPatient(String username, String relationship) throws ServiceException {
        int caregiverId = currentCaregiverId();

        if (username == null || username.isBlank())
            throw new ServiceException("Please enter the patient's username.");
        if (relationship != null && relationship.trim().length() > 100)
            throw new ServiceException("Relationship must be 100 characters or fewer.");

        String rel = (relationship == null || relationship.isBlank()) ? null : relationship.trim();

        Integer patientId = db(() -> linkDao.findActivePatientIdByUsername(username.trim()), FAIL);
        if (patientId == null)
            throw new ServiceException("No active patient has that username.");

        String status = db(() -> linkDao.caregiverLinkStatus(caregiverId, patientId), FAIL);
        if ("ACTIVE".equals(status))
            throw new ServiceException("You are already linked to this patient.");
        if ("PENDING".equals(status))
            throw new ServiceException("A request is already waiting for this patient's approval.");

        if (status == null) {
            db(() -> linkDao.insertCaregiverRequest(caregiverId, patientId, rel), FAIL);
        } else {
            db(() -> linkDao.renewCaregiverRequest(caregiverId, patientId, rel), FAIL);  // was REVOKED
        }
    }

    public void cancelPatientLink(int linkId) throws ServiceException {
        int caregiverId = currentCaregiverId();
        if (db(() -> linkDao.revokeByCaregiver(linkId, caregiverId), FAIL) == 0)
            throw new ServiceException("This link no longer exists.");
    }

    // ===================== Helpers =====================

    private int currentPatientId() throws ServiceException {
        User u = requireRole(Role.PATIENT);
        Integer id = db(() -> profileDao.patientIdOf(u.getUserId()), FAIL);
        if (id == null) throw new ServiceException("Your patient profile was not found.");
        return id;
    }

    private int currentCaregiverId() throws ServiceException {
        User u = requireRole(Role.CAREGIVER);
        Integer id = db(() -> profileDao.caregiverIdOf(u.getUserId()), FAIL);
        if (id == null) throw new ServiceException("Your caregiver profile was not found.");
        return id;
    }

    private User requireRole(Role role) throws ServiceException {
        User u = Session.getUser();
        if (u == null || u.getRole() != role)
            throw new ServiceException("You are not allowed to do this.");
        return u;
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T run() throws SQLException;
    }

    /** Runs a DAO call and turns database errors into a friendly message. */
    private <T> T db(SqlCall<T> call, String failMessage) throws ServiceException {
        try {
            return call.run();
        } catch (SQLException e) {
            throw new ServiceException(failMessage);
        }
    }
}