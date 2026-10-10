/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.mycompany.dawacare.ui;

import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.service.PrescriptionService;
import com.mycompany.dawacare.service.ServiceException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PrescriptionDialog extends javax.swing.JDialog {
    
    private static final int[] INTERVALS = {1, 2, 7};   // matches cboInterval item order

    private final PrescriptionService rxService = new PrescriptionService();
    private final DoctorPatient patient;
    private final List<MedicationOption> medications = new ArrayList<>();
    private final List<PrescriptionItemRequest> items = new ArrayList<>();
    private final DefaultTableModel itemsModel = UiHelper.readOnlyModel(
        "Medication", "Dose", "Times", "Repeat", "Start", "End", "Instructions");
    private boolean saved = false;
    
    public PrescriptionDialog(java.awt.Frame parent, DoctorPatient patient) {
        super(parent, true);
        this.patient = patient;
        initComponents();
        setLocationRelativeTo(parent);

        String gender = patient.gender() == null ? "" : patient.gender();
        String age = patient.age() == null ? "" : ", " + patient.age() + " yrs";
        lblPatient.setText("Patient: " + patient.fullName() + " (" + gender + age + ")");

        tblItems.setModel(itemsModel);
        tblItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        txtDose.setText("1");
        txtTimes.setText("08:00");
        txtStart.setText(LocalDate.now().toString());
        txtDuration.setText("30");

        loadMedications();
    }

    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblPatient = new javax.swing.JLabel();
        lblDiagnosis = new javax.swing.JLabel();
        lblNotes = new javax.swing.JLabel();
        txtDiagnosis = new javax.swing.JTextField();
        txtNotes = new javax.swing.JTextField();
        pnlItem = new javax.swing.JPanel();
        lblMedication = new javax.swing.JLabel();
        cboMedication = new javax.swing.JComboBox<>();
        lblDose = new javax.swing.JLabel();
        txtDose = new javax.swing.JTextField();
        cboUnit = new javax.swing.JComboBox<>();
        lblTimes = new javax.swing.JLabel();
        txtTimes = new javax.swing.JTextField();
        lblInterval = new javax.swing.JLabel();
        cboInterval = new javax.swing.JComboBox<>();
        lblStart = new javax.swing.JLabel();
        txtStart = new javax.swing.JTextField();
        lblDuration = new javax.swing.JLabel();
        txtDuration = new javax.swing.JTextField();
        lblInstructions = new javax.swing.JLabel();
        txtInstructions = new javax.swing.JTextField();
        btnAddItem = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblItems = new javax.swing.JTable();
        btnRemoveItem = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        lblStatus = new javax.swing.JLabel();
        btnCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("New Prescription");

        lblPatient.setText("Patient:              ahereza nobert (MALE, 34 yrs) ");

        lblDiagnosis.setText("Diagnosis:");

        lblNotes.setText("Notes:");

        txtDiagnosis.addActionListener(this::txtDiagnosisActionPerformed);

        pnlItem.setBorder(javax.swing.BorderFactory.createTitledBorder("Add medication"));

        lblMedication.setText("Medication:");

        lblDose.setText("Dose:");

        cboUnit.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "tablet(s)", "capsule(s)", "ml", "mg", "drop(s)", "puff(s)", "unit(s)", "sachet(s)" }));

        lblTimes.setText("Times (24h, comma-separated):");

        lblInterval.setText("Repeat:");

        cboInterval.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Every day", "Every 2 days", "Once a week" }));

        lblStart.setText("Start (YYYY-MM-DD):");

        lblDuration.setText("Duration (days):");

        lblInstructions.setText("Instructions:");

        btnAddItem.setText("Add to List");
        btnAddItem.addActionListener(this::btnAddItemActionPerformed);

        jLabel1.setText("Unit:");

        javax.swing.GroupLayout pnlItemLayout = new javax.swing.GroupLayout(pnlItem);
        pnlItem.setLayout(pnlItemLayout);
        pnlItemLayout.setHorizontalGroup(
            pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlItemLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMedication)
                    .addComponent(lblDose)
                    .addComponent(lblInstructions)
                    .addComponent(lblInterval))
                .addGap(31, 31, 31)
                .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cboMedication, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtDose, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cboInterval, 0, 141, Short.MAX_VALUE)
                    .addComponent(txtInstructions, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(38, 38, 38)
                .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(pnlItemLayout.createSequentialGroup()
                        .addComponent(lblStart)
                        .addGap(18, 18, 18)
                        .addComponent(txtStart, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlItemLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(cboUnit, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlItemLayout.createSequentialGroup()
                        .addGap(179, 179, 179)
                        .addComponent(lblDuration))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlItemLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblTimes)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtTimes, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
                    .addComponent(txtDuration)
                    .addComponent(btnAddItem, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(37, 37, 37))
        );
        pnlItemLayout.setVerticalGroup(
            pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlItemLayout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMedication)
                    .addComponent(cboMedication, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDose)
                    .addComponent(txtDose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTimes)
                    .addComponent(txtTimes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cboUnit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(18, 18, 18)
                .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblInterval)
                    .addComponent(cboInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblStart)
                    .addComponent(txtStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblDuration)
                    .addComponent(txtDuration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlItemLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(pnlItemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblInstructions)
                            .addComponent(txtInstructions, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlItemLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                        .addComponent(btnAddItem, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(21, 21, 21))))
        );

        tblItems.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblItems);

        btnRemoveItem.setText("Remove Selected");
        btnRemoveItem.addActionListener(this::btnRemoveItemActionPerformed);

        btnSave.setText("Save Prescription");
        btnSave.addActionListener(this::btnSaveActionPerformed);

        lblStatus.setForeground(new java.awt.Color(204, 0, 0));
        lblStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(this::btnCancelActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(pnlItem, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblPatient)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblNotes)
                                    .addComponent(lblDiagnosis, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(37, 37, 37)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(txtDiagnosis, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                                    .addComponent(txtNotes))))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(66, 66, 66)
                        .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(39, 39, 39))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btnRemoveItem, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(45, 45, 45))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(lblPatient)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDiagnosis)
                    .addComponent(txtDiagnosis, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblNotes)
                    .addComponent(txtNotes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(pnlItem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnRemoveItem)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 17, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSave)
                    .addComponent(btnCancel))
                .addGap(19, 19, 19))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtDiagnosisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDiagnosisActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDiagnosisActionPerformed

    private void btnAddItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddItemActionPerformed
        addItem();
    }//GEN-LAST:event_btnAddItemActionPerformed

    private void btnRemoveItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveItemActionPerformed
        int row = tblItems.getSelectedRow();
        if (row < 0) {
            lblStatus.setText("Select a medication in the list to remove it.");
            return;
        }
        int modelRow = tblItems.convertRowIndexToModel(row);
        items.remove(modelRow);
        itemsModel.removeRow(modelRow);
        lblStatus.setText(" ");
    }//GEN-LAST:event_btnRemoveItemActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        savePrescription();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    /**
     * @param args the command line arguments
     */
        public boolean isSaved() {
        return saved;
    }

    private void loadMedications() {
        UiHelper.load(this, rxService::getMedications, meds -> {
            medications.clear();
            medications.addAll(meds);
            cboMedication.removeAllItems();
            for (MedicationOption m : meds) {
                cboMedication.addItem(m.toString());
            }
            if (meds.isEmpty()) {
                lblStatus.setText("The medication catalogue is empty.");
            }
        });
    }

    private void addItem() {
        int idx = cboMedication.getSelectedIndex();
        MedicationOption med = (idx >= 0 && idx < medications.size()) ? medications.get(idx) : null;

        if (med != null && items.stream()
                .anyMatch(i -> i.medication().medicationId() == med.medicationId())) {
            lblStatus.setText(med + " is already in this prescription.");
            return;
        }

        int interval = INTERVALS[Math.max(0, cboInterval.getSelectedIndex())];
        try {
            PrescriptionItemRequest item = rxService.buildItem(
                    med,
                    txtDose.getText(),
                    (String) cboUnit.getSelectedItem(),
                    txtTimes.getText(),
                    interval,
                    txtStart.getText(),
                    txtDuration.getText(),
                    txtInstructions.getText());

            items.add(item);
            itemsModel.addRow(new Object[]{
                item.medication().toString(),
                item.doseText(),
                item.timesText(),
                PrescriptionItemRequest.repeatText(item.intervalDays()),
                item.startDate(),
                item.endDate(),
                item.instructions() == null ? "" : item.instructions()});

            txtInstructions.setText("");
            lblStatus.setText(" ");
        } catch (ServiceException ex) {
            lblStatus.setText(ex.getMessage());
        }
    }

    private void savePrescription() {
        if (items.isEmpty()) {
            lblStatus.setText("Add at least one medication to the list first.");
            return;
        }
        if (!UiHelper.confirm(this, "Save this prescription for " + patient.fullName() + "?")) {
            return;
        }

        // Read the form on the UI thread, before going to the background
        String diagnosis = txtDiagnosis.getText();
        String notes = txtNotes.getText();
        List<PrescriptionItemRequest> toSave = List.copyOf(items);

        btnSave.setEnabled(false);   // prevents double-saving
        UiHelper.load(this, () -> {
            try {
                return rxService.createPrescription(patient.patientId(), diagnosis, notes, toSave);
            } finally {
                SwingUtilities.invokeLater(() -> btnSave.setEnabled(true));
            }
        }, message -> {
            saved = true;
            JOptionPane.showMessageDialog(this, message, "Prescription saved",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddItem;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnRemoveItem;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<String> cboInterval;
    private javax.swing.JComboBox<String> cboMedication;
    private javax.swing.JComboBox<String> cboUnit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblDiagnosis;
    private javax.swing.JLabel lblDose;
    private javax.swing.JLabel lblDuration;
    private javax.swing.JLabel lblInstructions;
    private javax.swing.JLabel lblInterval;
    private javax.swing.JLabel lblMedication;
    private javax.swing.JLabel lblNotes;
    private javax.swing.JLabel lblPatient;
    private javax.swing.JLabel lblStart;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblTimes;
    private javax.swing.JPanel pnlItem;
    private javax.swing.JTable tblItems;
    private javax.swing.JTextField txtDiagnosis;
    private javax.swing.JTextField txtDose;
    private javax.swing.JTextField txtDuration;
    private javax.swing.JTextField txtInstructions;
    private javax.swing.JTextField txtNotes;
    private javax.swing.JTextField txtStart;
    private javax.swing.JTextField txtTimes;
    // End of variables declaration//GEN-END:variables
}
