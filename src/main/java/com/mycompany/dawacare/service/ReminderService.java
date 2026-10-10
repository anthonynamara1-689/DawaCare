package com.mycompany.dawacare.service;

import com.mycompany.dawacare.dao.ProfileDao;
import com.mycompany.dawacare.dao.ReminderDao;
import com.mycompany.dawacare.model.ReminderCandidate;
import com.mycompany.dawacare.model.Role;
import com.mycompany.dawacare.model.User;
import com.mycompany.dawacare.util.Session;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReminderService {
    private static final int REMIND_BEFORE_MINUTES = 15;
    private static final int REMIND_AFTER_MINUTES = 30;
    private static final int MISSED_LOOKBACK_HOURS = 24;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM");

    private final ReminderDao reminderDao = new ReminderDao();
    private final ProfileDao profileDao = new ProfileDao();

    /** Messages for reminders the current user has NOT seen before. */
    public List<String> checkNewReminders() throws ServiceException {
        User u = Session.getUser();
        List<String> messages = new ArrayList<>();
        if (u == null) return messages;

        LocalDateTime now = LocalDateTime.now();
        try {
            if (u.getRole() == Role.PATIENT) {
                Integer patientId = profileDao.patientIdOf(u.getUserId());
                if (patientId == null) return messages;
                for (ReminderCandidate r : reminderDao.findDueDoses(patientId,
                        now.minusMinutes(REMIND_AFTER_MINUTES), now.plusMinutes(REMIND_BEFORE_MINUTES))) {
                    String msg = "Time for " + r.medication() + " - " + r.doseText()
                               + " at " + r.scheduledAt().format(TIME) + ".";
                    if (reminderDao.insertNotification(u.getUserId(), "DOSE_REMINDER", msg, r.doseId()) == 1) {
                        messages.add(msg);
                    }
                }
            } else if (u.getRole() == Role.CAREGIVER) {
                Integer caregiverId = profileDao.caregiverIdOf(u.getUserId());
                if (caregiverId == null) return messages;
                for (ReminderCandidate r : reminderDao.findRecentMissedForCaregiver(caregiverId,
                        now.minusHours(MISSED_LOOKBACK_HOURS))) {
                    String msg = r.patientName() + " missed " + r.medication() + " (due "
                               + r.scheduledAt().format(TIME) + " on " + r.scheduledAt().format(DAY) + ").";
                    if (reminderDao.insertNotification(u.getUserId(), "MISSED_DOSE", msg, r.doseId()) == 1) {
                        messages.add(msg);
                    }
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not check reminders.");
        }
        return messages;
    }
}