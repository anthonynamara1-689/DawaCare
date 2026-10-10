package com.mycompany.dawacare;

import com.mycompany.dawacare.ui.LoginFrame;
import javax.swing.*;

public class DawaCare {
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }
        
        com.mycompany.dawacare.jobs.MissedDoseJob.start();

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}