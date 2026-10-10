package com.mycompany.dawacare.service;

import com.mycompany.dawacare.dao.PrescriptionDao;
import com.mycompany.dawacare.dao.ProfileDao;
import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.util.Session;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class PrescriptionService {
    private static final String FAIL = "Something went wrong talking to the database. Please try again.";
    private static final Pattern TIME = Pattern.compile("^([01]?\\d|2[0-3]):[0-5]\\d$");
    private static final int MAX_TIMES_PER_DAY = 6;
    private static final int MAX_DURATION_DAYS = 365;

    private final PrescriptionDao rxDao = new PrescriptionDao();
    private final ProfileDao profileDao = new ProfileDao();

    public List<DoctorPatient> getMyPatients() throws ServiceException {
        int doctorId = currentDoctorId();
        return db(() -> rxDao.findPatientsOfDoctor(doctorId), "Could not load your patients.");
    }

    public List<MedicationOption> getMedications() throws ServiceException {
        currentDoctorId();
        return db(rxDao::findMedications, "Could not load medications.");
    }

    /** Validates one medication line from the dialog. No database access. */
    public PrescriptionItemRequest buildItem(MedicationOption medication, String doseText, String unit,
                                             String timesText, int intervalDays, String startText,
                                             String durationText, String instructions)
            throws ServiceException {
        if (medication == null)
            throw new ServiceException("Please choose a medication.");

        BigDecimal dose;
        try {
            dose = new BigDecimal(doseText.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ServiceException("Dose must be a number, for example 1 or 0.5.");
        }
        if (dose.signum() <= 0 || dose.compareTo(new BigDecimal("10000")) > 0)
            throw new ServiceException("Dose must be greater than 0.");
        if (dose.scale() > 2)
            throw new ServiceException("Dose can have at most 2 decimal places.");

        if (unit == null || unit.isBlank())
            throw new ServiceException("Please choose a dose unit.");

        List<LocalTime> times = parseTimes(timesText);

        if (intervalDays < 1 || intervalDays > 30)
            throw new ServiceException("Please choose how often to repeat.");

        LocalDate start;
        try {
            start = LocalDate.parse(startText.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ServiceException("Start date must be in the format YYYY-MM-DD.");
        }
        if (start.isBefore(LocalDate.now()))
            throw new ServiceException("Start date cannot be in the past.");

        int duration;
        try {
            duration = Integer.parseInt(durationText.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ServiceException("Duration must be a whole number of days.");
        }
        if (duration < 1 || duration > MAX_DURATION_DAYS)
            throw new ServiceException("Duration must be between 1 and " + MAX_DURATION_DAYS + " days.");

        LocalDate end = start.plusDays(duration - 1);

        String instr = (instructions == null || instructions.isBlank()) ? null : instructions.trim();
        if (instr != null && instr.length() > 500)
            throw new ServiceException("Instructions must be 500 characters or fewer.");

        PrescriptionItemRequest item = new PrescriptionItemRequest(
                medication, dose, unit, times, intervalDays, start, end, instr);

        if (DoseGenerator.generate(item, LocalDateTime.now()).isEmpty())
            throw new ServiceException("This schedule has no upcoming doses. "
                    + "Check the start date and the dose times.");
        return item;
    }

    /** Saves the whole prescription and generates every dose. Returns a success message. */
    public String createPrescription(int patientId, String diagnosis, String notes,
                                     List<PrescriptionItemRequest> items) throws ServiceException {
        User user = requireDoctor();
        int doctorId = doctorIdOf(user);

        if (items == null || items.isEmpty())
            throw new ServiceException("Add at least one medication.");
        if (diagnosis != null && diagnosis.trim().length() > 255)
            throw new ServiceException("Diagnosis must be 255 characters or fewer.");
        if (!db(() -> rxDao.isActivePatientOfDoctor(doctorId, patientId), FAIL))
            throw new ServiceException("This patient is not on your patient list.");

        LocalDateTime now = LocalDateTime.now();
        List<List<LocalDateTime>> dosesPerItem = new ArrayList<>();
        int total = 0;
        for (PrescriptionItemRequest item : items) {
            List<LocalDateTime> doses = DoseGenerator.generate(item, now);
            if (doses.isEmpty())
                throw new ServiceException(item.medication() + " has no upcoming doses.");
            dosesPerItem.add(doses);
            total += doses.size();
        }

        String dx = blankToNull(diagnosis);
        String nt = blankToNull(notes);
        db(() -> rxDao.createPrescription(patientId, doctorId, dx, nt, items, dosesPerItem, user.getUserId()),
           "Could not save the prescription.");

        return "Prescription saved: " + items.size() + " medication(s), " + total + " scheduled doses.";
    }

    public List<PrescriptionSummary> getMyPrescriptions() throws ServiceException {
        int doctorId = currentDoctorId();
        return db(() -> rxDao.findPrescriptionsOfDoctor(doctorId), "Could not load prescriptions.");
    }

    public List<PrescriptionItemView> getPrescriptionItems(int prescriptionId) throws ServiceException {
        int doctorId = currentDoctorId();
        return db(() -> rxDao.findItems(prescriptionId, doctorId), "Could not load prescription items.");
    }

    public void cancelPrescription(int prescriptionId) throws ServiceException {
        User user = requireDoctor();
        int doctorId = doctorIdOf(user);
        if (!db(() -> rxDao.cancelPrescription(prescriptionId, doctorId, user.getUserId()), FAIL))
            throw new ServiceException("Only active prescriptions can be cancelled.");
    }

    // ===================== Helpers =====================

    private List<LocalTime> parseTimes(String text) throws ServiceException {
        if (text == null || text.isBlank())
            throw new ServiceException("Enter at least one dose time, for example 08:00.");
        TreeSet<LocalTime> times = new TreeSet<>();   // sorted, no duplicates
        for (String part : text.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) continue;
            if (!TIME.matcher(s).matches())
                throw new ServiceException("'" + s + "' is not a valid time. Use 24-hour format like 08:00 or 20:30.");
            times.add(LocalTime.parse(s.length() == 4 ? "0" + s : s));   // "8:00" -> "08:00"
        }
        if (times.isEmpty())
            throw new ServiceException("Enter at least one dose time, for example 08:00.");
        if (times.size() > MAX_TIMES_PER_DAY)
            throw new ServiceException("At most " + MAX_TIMES_PER_DAY + " doses per day.");
        return new ArrayList<>(times);
    }

    private User requireDoctor() throws ServiceException {
        User u = Session.getUser();
        if (u == null || u.getRole() != Role.DOCTOR)
            throw new ServiceException("Only doctors can do this.");
        return u;
    }

    private int currentDoctorId() throws ServiceException {
        return doctorIdOf(requireDoctor());
    }

    private int doctorIdOf(User u) throws ServiceException {
        Integer id = db(() -> profileDao.doctorIdOf(u.getUserId()), FAIL);
        if (id == null) throw new ServiceException("Your doctor profile was not found.");
        return id;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
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
}
