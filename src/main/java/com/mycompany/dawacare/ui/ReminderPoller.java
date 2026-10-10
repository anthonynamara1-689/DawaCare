package com.mycompany.dawacare.ui;

import com.mycompany.dawacare.service.ReminderService;
import com.mycompany.dawacare.service.ServiceException;
import com.mycompany.dawacare.util.Session;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.*;

public final class ReminderPoller {
    private static final int CHECK_EVERY_MS = 60_000;
    private static final int FIRST_CHECK_MS = 3_000;

    private final JFrame owner;
    private final ReminderService reminderService = new ReminderService();
    private final Timer timer;
    private boolean checking = false;

    private ReminderPoller(JFrame owner) {
        this.owner = owner;
        this.timer = new Timer(CHECK_EVERY_MS, e -> check());
        timer.setInitialDelay(FIRST_CHECK_MS);

        owner.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                timer.stop();   // dashboard closed (logout) -> stop checking
            }
        });
        timer.start();
    }

    /** Start reminders for this dashboard window. */
    public static void attachTo(JFrame owner) {
        new ReminderPoller(owner);
    }

    private void check() {
        if (checking || !Session.isLoggedIn()) return;
        checking = true;

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                try {
                    return reminderService.checkNewReminders();
                } catch (ServiceException ex) {
                    System.err.println("Reminder check failed: " + ex.getMessage());
                    return List.of();
                }
            }

            @Override
            protected void done() {
                checking = false;
                try {
                    List<String> messages = get();
                    if (!messages.isEmpty() && owner.isDisplayable()) show(messages);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    /** Non-blocking pop-up so the user can keep working. */
    private void show(List<String> messages) {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane pane = new JOptionPane(String.join("\n", messages), JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(owner, "DawaCare reminder");
        dialog.setModal(false);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }
}