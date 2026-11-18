package payroll.dao.impl;

import payroll.dao.DataAccessException;
import payroll.dao.PayrollResultDAO;
import payroll.PayrollResult;
import payroll.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PayrollResultDAOImpl implements PayrollResultDAO {

    private static final Logger LOGGER = Logger.getLogger(PayrollResultDAOImpl.class.getName());

    @Override
    public void saveBatch(String runId, List<PayrollResult> results) throws DataAccessException {
        if (results == null || results.isEmpty()) return;

        String sql = "INSERT INTO payroll_results (run_id, employee_id, gross_salary, deductions, net_salary) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (PayrollResult result : results) {
                    ps.setString(1, runId);
                    ps.setInt(2, result.getEmployeeId());
                    ps.setDouble(3, result.getGrossSalary());
                    ps.setDouble(4, result.getDeductions());
                    ps.setDouble(5, result.getNetSalary());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error in saveBatch for runId: " + runId, e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Rollback error", ex); }
            }
            throw new DataAccessException("Failed to save payroll batch results to database.", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    @Override
    public List<PayrollResult> findByRunId(String runId) throws DataAccessException {
        String sql = "SELECT pr.result_id, pr.run_id, pr.employee_id, e.name AS employee_name, " +
                     "pr.gross_salary, pr.deductions, pr.net_salary, pr.processed_at " +
                     "FROM payroll_results pr LEFT JOIN employees e ON pr.employee_id = e.employee_id " +
                     "WHERE pr.run_id = ? ORDER BY pr.result_id ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PayrollResult> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new PayrollResult(
                            rs.getLong("result_id"), rs.getString("run_id"),
                            rs.getInt("employee_id"), rs.getString("employee_name"),
                            rs.getBigDecimal("gross_salary").doubleValue(),
                            rs.getBigDecimal("deductions").doubleValue(),
                            rs.getBigDecimal("net_salary").doubleValue(),
                            rs.getTimestamp("processed_at").toLocalDateTime()));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve payroll results for run ID.", e);
        }
    }
}
