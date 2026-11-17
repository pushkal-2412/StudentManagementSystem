package controller;

import dao.DashboardDAO;
import model.DashboardStats;

import java.sql.SQLException;

/** Loads dashboard KPIs. */
public class DashboardController {

    private final DashboardDAO dao = new DashboardDAO();

    public DashboardStats load() throws SQLException { return dao.load(); }
}
