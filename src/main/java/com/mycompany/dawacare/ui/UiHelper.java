package com.mycompany.dawacare.ui;

import com.mycompany.dawacare.service.ServiceException;
import java.awt.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class UiHelper {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private UiHelper() {}

    @FunctionalInterface
    public interface ServiceCall<T> {
        T call() throws ServiceException;
    }

    @FunctionalInterface
    public interface ServiceAction {
        void run() throws ServiceException;
    }

    /** Runs a service call in the background, then passes the result to onSuccess on the UI thread. */
    public static <T> void load(Component parent, ServiceCall<T> call, Consumer<T> onSuccess) {
        new SwingWorker<T, Void>() {
            private String error;

            @Override
            protected T doInBackground() {
                try {
                    return call.call();
                } catch (ServiceException ex) {
                    error = ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                T result = null;
                try {
                    result = get();
                } catch (Exception ex) {
                    error = "Unexpected error: " + ex.getMessage();
                }
                if (error != null) {
                    showError(parent, error);
                    return;
                }
                onSuccess.accept(result);
            }
        }.execute();
    }

    /** Runs an action with no result (approve, remove...) in the background. */
    public static void run(Component parent, ServiceAction action, Runnable onSuccess) {
        load(parent, () -> { action.run(); return null; }, ignored -> onSuccess.run());
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "DawaCare", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    /** Attaches the model, allows one selected row, and hides column 0 (the ID). */
    public static void setupTable(JTable table, DefaultTableModel model) {
        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.removeColumn(table.getColumnModel().getColumn(0));
    }

    /** ID from column 0 of the selected row, or -1 after asking the user to select one. */
    public static int selectedId(Component parent, JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(parent, "Please select a row first.");
            return -1;
        }
        return (Integer) table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);
    }

    public static String fmt(LocalDateTime t) {
        return t == null ? "-" : t.format(FMT);
    }
}