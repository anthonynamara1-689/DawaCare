package com.mycompany.dawacare.service;

import com.mycompany.dawacare.dao.AdminDao;
import com.mycompany.dawacare.model.*;
import com.mycompany.dawacare.util.Session;
import java.sql.SQLException;
import java.util.List;

public class AdminService {
    private final AdminDao adminDao = new AdminDao();

    public List<PendingDoctor> getPendingDoctors() throws ServiceException {
        requireAdmin();
        try {
            return adminDao.findPendingDoctors();
        } catch (SQLException e) {
            throw new ServiceException("Could not load pending doctors.");
        }
    }

    public List<UserSummary> getAllUsers() throws ServiceException {
        requireAdmin();
        try {
            return adminDao.findAllUsers();
        } catch (SQLException e) {
            throw new ServiceException("Could not load users.");
        }
    }

    public void approveDoctor(int doctorUserId) throws ServiceException {
        User admin = requireAdmin();
        try {
            if (!adminDao.approveDoctor(doctorUserId, admin.getUserId()))
                throw new ServiceException("This doctor is no longer pending.");
        } catch (SQLException e) {
            throw new ServiceException("Could not approve the doctor.");
        }
    }

    public void rejectDoctor(int doctorUserId) throws ServiceException {
        User admin = requireAdmin();
        try {
            if (!adminDao.rejectDoctor(doctorUserId, admin.getUserId()))
                throw new ServiceException("This doctor is no longer pending.");
        } catch (SQLException e) {
            throw new ServiceException("Could not reject the doctor.");
        }
    }

    public void activateUser(int userId) throws ServiceException {
        User admin = requireAdmin();
        if (userId == admin.getUserId())
            throw new ServiceException("You cannot change your own account.");
        try {
            if (!adminDao.activateUser(userId, admin.getUserId()))
                throw new ServiceException("Cannot activate this account. It may be an admin, "
                    + "already active, or a doctor whose license was never approved.");
        } catch (SQLException e) {
            throw new ServiceException("Could not activate the account.");
        }
    }

    public void deactivateUser(int userId) throws ServiceException {
        User admin = requireAdmin();
        if (userId == admin.getUserId())
            throw new ServiceException("You cannot change your own account.");
        try {
            if (!adminDao.deactivateUser(userId, admin.getUserId()))
                throw new ServiceException("Cannot deactivate this account. It may be an admin, "
                    + "already inactive, or still pending.");
        } catch (SQLException e) {
            throw new ServiceException("Could not deactivate the account.");
        }
    }

    private User requireAdmin() throws ServiceException {
        User u = Session.getUser();
        if (u == null || u.getRole() != Role.ADMIN)
            throw new ServiceException("Only administrators can do this.");
        return u;
    }
    
    public List<AuditEntry> getAuditLog() throws ServiceException {
        requireAdmin();
        try {
            return adminDao.findAuditLog(500);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the audit log.");
        }
    }
}