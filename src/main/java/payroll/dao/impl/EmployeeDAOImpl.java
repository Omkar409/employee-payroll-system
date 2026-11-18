package payroll.dao.impl;

import payroll.*;
import payroll.dao.DataAccessException;
import payroll.dao.EmployeeDAO;
import payroll.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmployeeDAOImpl implements EmployeeDAO {

    private static final Logger LOGGER = Logger.getLogger(EmployeeDAOImpl.class.getName());

    @Override
    public int insert(Employee employee, int createdByUserId) throws DataAccessException {
        String sql = "INSERT INTO employees (name, basic_salary, employee_type, bonus, contract_allowance, contract_months, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEmployee(ps, employee);
            if (createdByUserId > 0) ps.setInt(7, createdByUserId);
            else ps.setNull(7, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    employee.setEmployeeId(id);
                    employee.setCreatedBy(createdByUserId > 0 ? createdByUserId : null);
                    return id;
                }
            }
            throw new DataAccessException("Employee inserted but failed to retrieve generated ID.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error inserting employee: " + employee.getName(), e);
            throw new DataAccessException("Failed to insert employee: " + e.getMessage(), e);
        }
    }

    @Override
    public void insertBatch(List<Employee> employees, int createdByUserId) throws DataAccessException {
        if (employees == null || employees.isEmpty()) return;
        String sql = "INSERT INTO employees (name, basic_salary, employee_type, bonus, contract_allowance, contract_months, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int count = 0;
                for (Employee e : employees) {
                    bindEmployee(ps, e);
                    if (createdByUserId > 0) ps.setInt(7, createdByUserId);
                    else ps.setNull(7, Types.INTEGER);
                    ps.addBatch();
                    count++;
                    if (count % 500 == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
                conn.commit();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error in insertBatch", e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Rollback error", ex); }
            }
            throw new DataAccessException("Failed to insert employee batch into database: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public Optional<Employee> findById(int employeeId) throws DataAccessException {
        String sql = "SELECT employee_id, name, basic_salary, employee_type, bonus, contract_allowance, contract_months, created_by, created_at, updated_at FROM employees WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find employee with ID " + employeeId, e);
        }
    }

    @Override
    public List<Employee> findAll() throws DataAccessException {
        String sql = "SELECT employee_id, name, basic_salary, employee_type, bonus, contract_allowance, contract_months, created_by, created_at, updated_at FROM employees ORDER BY employee_id ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Employee> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve employee list.", e);
        }
    }

    @Override
    public List<Employee> findPage(int offset, int limit) throws DataAccessException {
        String sql = "SELECT employee_id, name, basic_salary, employee_type, bonus, contract_allowance, contract_months, created_by, created_at, updated_at FROM employees ORDER BY employee_id ASC LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Employee> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve employee page.", e);
        }
    }

    @Override
    public List<Employee> searchByName(String keyword) throws DataAccessException {
        String sql = "SELECT employee_id, name, basic_salary, employee_type, bonus, contract_allowance, contract_months, created_by, created_at, updated_at FROM employees WHERE name LIKE ? ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Employee> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to search employees.", e);
        }
    }

    @Override
    public boolean update(Employee employee) throws DataAccessException {
        String sql = "UPDATE employees SET name = ?, basic_salary = ?, employee_type = ?, bonus = ?, contract_allowance = ?, contract_months = ? WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindEmployee(ps, employee);
            ps.setInt(7, employee.getEmployeeId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update employee: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(int employeeId) throws DataAccessException {
        String sql = "DELETE FROM employees WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete employee ID " + employeeId, e);
        }
    }

    @Override
    public int count() throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM employees";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count employees.", e);
        }
    }

    private void bindEmployee(PreparedStatement ps, Employee e) throws SQLException {
        ps.setString(1, e.getName());
        ps.setDouble(2, e.getBasicSalary());
        ps.setString(3, e.getEmployeeType().name());
        if (e instanceof PermanentEmployee pe) {
            ps.setDouble(4, pe.getBonus());
            ps.setNull(5, Types.DECIMAL);
            ps.setNull(6, Types.SMALLINT);
        } else if (e instanceof ContractEmployee ce) {
            ps.setNull(4, Types.DECIMAL);
            ps.setDouble(5, ce.getContractAllowance());
            ps.setInt(6, ce.getContractMonths());
        } else {
            throw new IllegalArgumentException("Unsupported employee type: " + e.getClass().getName());
        }
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("employee_id");
        String name = rs.getString("name");
        double basic = rs.getBigDecimal("basic_salary").doubleValue();
        EmployeeType type = EmployeeType.valueOf(rs.getString("employee_type"));

        Employee emp;
        if (type == EmployeeType.PERMANENT) {
            double bonus = rs.getBigDecimal("bonus") != null ? rs.getBigDecimal("bonus").doubleValue() : 0.0;
            emp = new PermanentEmployee(id, name, basic, bonus);
        } else {
            double allowance = rs.getBigDecimal("contract_allowance") != null ? rs.getBigDecimal("contract_allowance").doubleValue() : 0.0;
            int months = rs.getInt("contract_months");
            emp = new ContractEmployee(id, name, basic, allowance, months);
        }

        int createdByVal = rs.getInt("created_by");
        if (!rs.wasNull()) emp.setCreatedBy(createdByVal);

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) emp.setCreatedAt(createdTs.toLocalDateTime());

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) emp.setUpdatedAt(updatedTs.toLocalDateTime());

        return emp;
    }
}
