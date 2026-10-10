package com.mycompany.dawacare.service;

import com.mycompany.dawacare.dao.DoseDao;
import com.mycompany.dawacare.dao.ProfileDao;
import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.util.Session;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DoseService {
    private static final String FAIL = "Something went wrong talking to the database. Please try again.";
    private static final int EARLY_MINUTES = 60;   // can record up to 1 hour before
    private static final int LATE_HOURS = 24;      // ...and up to 24 hours after

    private final DoseDao doseDao = new DoseDao();
    private final ProfileDao profileDao = new ProfileDao();

    /** Today's doses plus adherence. patientId is ignored for patients (they always see their own). */
    public DoseOverview getOverview(Integer requestedPatientId) throws ServiceException {
        int patientId = resolvePatient(requestedPatientId);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        return db(() -> new DoseOverview(
                doseDao.findDosesBetween(patientId, today.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                doseDao.stats(patientId, now.minusDays(7), now),
                doseDao.stats(patientId, now.minusDays(30), now),
                doseDao.statsByMedication(patientId, now.minusDays(30), now)),
            "Could not load dose records.");
    }

    public void markTaken(Integer requestedPatientId, int doseId) throws ServiceException {
        record(requestedPatientId, doseId, "TAKEN", null);
    }

    public void markSkipped(Integer requestedPatientId, int doseId, String reason) throws ServiceException {
        String r = (reason == null || reason.isBlank()) ? null : reason.trim();
        if (r != null && r.length() > 255)
            throw new ServiceException("The reason must be 255 characters or fewer.");
        record(requestedPatientId, doseId, "SKIPPED", r);
    }

    public List<PatientOption> getMyActivePatients() throws ServiceException {
        int caregiverId = currentCaregiverId();
        return db(() -> doseDao.findActivePatientsOfCaregiver(caregiverId), "Could not load your patients.");
    }

    // ===================== Helpers =====================

    private void record(Integer requestedPatientId, int doseId, String status, String reason)
            throws ServiceException {
        User user = Session.getUser();
        if (user != null && user.getRole() == Role.DOCTOR)
            throw new ServiceException("Doctors can view dose records but cannot record doses.");

        int patientId = resolvePatient(requestedPatientId);
        LocalDateTime now = LocalDateTime.now();
        int rows = db(() -> doseDao.markDose(doseId, patientId, status, reason, user.getUserId(),
                                             now.minusHours(LATE_HOURS), now.plusMinutes(EARLY_MINUTES)),
                      FAIL);
        if (rows == 0)
            throw new ServiceException("This dose can't be recorded right now. Doses can be recorded "
                    + "from 1 hour before their time until 24 hours after, and only once.");
    }

    private int resolvePatient(Integer requestedPatientId) throws ServiceException {
        User u = Session.getUser();
        if (u == null)
            throw new ServiceException("Please log in again.");

        if (u.getRole() == Role.PATIENT) {
            Integer id = db(() -> profileDao.patientIdOf(u.getUserId()), FAIL);
            if (id == null) throw new ServiceException("Your patient profile was not found.");
            return id;
        }

        if (u.getRole() == Role.CAREGIVER) {
            if (requestedPatientId == null)
                throw new ServiceException("Please choose a patient.");
            int caregiverId = currentCaregiverId();
            if (!db(() -> doseDao.isActiveCaregiverOf(caregiverId, requestedPatientId), FAIL))
                throw new ServiceException("You no longer have access to this patient's records.");
            return requestedPatientId;
        }

        if (u.getRole() == Role.DOCTOR) {
            if (requestedPatientId == null)
                throw new ServiceException("Please choose a patient.");
            Integer doctorId = db(() -> profileDao.doctorIdOf(u.getUserId()), FAIL);
            if (doctorId == null)
                throw new ServiceException("Your doctor profile was not found.");
            if (!db(() -> doseDao.isActiveDoctorOf(doctorId, requestedPatientId), FAIL))
                throw new ServiceException("This patient is no longer on your patient list.");
            return requestedPatientId;
        }

        throw new ServiceException("You are not allowed to view dose records here.");
    }

    private int currentCaregiverId() throws ServiceException {
        User u = Session.getUser();
        if (u == null || u.getRole() != Role.CAREGIVER)
            throw new ServiceException("Only caregivers can do this.");
        Integer id = db(() -> profileDao.caregiverIdOf(u.getUserId()), FAIL);
        if (id == null) throw new ServiceException("Your caregiver profile was not found.");
        return id;
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T run() throws SQLException;
    }

    private <T> T db(SqlCall<T> call, String failMessage) throws ServiceException {
        try {
            return call.run();
        } catch (SQLException e) {
            throw new ServiceException(failMessage);
        }
    }
    
        /** Gathers everything for the PDF. Same access rules as viewing doses. */
    public AdherenceReportData getReportData(Integer requestedPatientId, int days) throws ServiceException {
        if (days < 1 || days > 365)
            throw new ServiceException("The report period must be between 1 and 365 days.");

        int patientId = resolvePatient(requestedPatientId);
        User user = Session.getUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = LocalDate.now().minusDays(days - 1).atStartOfDay();
        String generatedBy = user.getFullName() + " (" + user.getRole().name().toLowerCase() + ")";

        return db(() -> {
            AdherenceReportData data = new AdherenceReportData(
                doseDao.findPatientInfo(patientId),
                from, now, now, generatedBy,
                doseDao.stats(patientId, from, now),
                doseDao.statsByMedication(patientId, from, now),
                doseDao.findCurrentMedications(patientId),
                doseDao.findDosesBetween(patientId, from, now));
            doseDao.logReport(user.getUserId(), patientId, days);
            return data;
        }, "Could not gather the report data.");
    }
}