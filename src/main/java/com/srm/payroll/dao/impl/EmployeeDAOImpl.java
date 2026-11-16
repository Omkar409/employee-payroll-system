package com.srm.payroll.dao.impl;

import com.srm.payroll.config.DatabaseConnection;
import com.srm.payroll.dao.DataAccessException;
import com.srm.payroll.dao.EmployeeDAO;
import com.srm.payroll.model.ContractEmployee;
import com.srm.payroll.model.Employee;
import com.srm.payroll.model.EmployeeType;
import com.srm.payroll.model.PermanentEmployee;

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
        String sql = "INSERT INTO employees (name, basic_salary, employee_type, bonus, " +
                     "contract_allowance, contract_months, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindEmployee(ps, employee);
            if (createdByUserId > 0) {
                ps.setInt(7, createdByUserId);
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    employee.setEmployeeId(id);
                    employee.setCreatedBy(createdByUserId > 0 ? createdByUserId : null);
                    return id;
                }
            }
            throw new DataAccessException("Employee inserted but failed to retrieve generated employee ID.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error inserting employee: " + employee.getName(), e);
            throw new DataAccessException("Failed to insert employee into database: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Employee> findById(int employeeId) throws DataAccessException {
        String sql = "SELECT employee_id, name, basic_salary, employee_type, bonus, contract_allowance, " +
                     "contract_months, created_by, created_at, updated_at FROM employees WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error finding employee by id: " + employeeId, e);
            throw new DataAccessException("Failed to find employee with ID " + employeeId, e);
        }
    }

    @Override
    public List<Employee> findAll() throws DataAccessException {
        String sql = "SELECT employee_id, name, basic_salary, employee_type, bonus, contract_allowance, " +
                     "contract_months, created_by, created_at, updated_at FROM employees ORDER BY employee_id ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Employee> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error loading all employees", e);
            throw new DataAccessException("Failed to retrieve employee list from database.", e);
        }
    }

    @Override
    public List<Employee> findPage(int offset, int limit) throws DataAccessException {
        String sql = "SELECT employee_id, name, basic_salary, employee_type, bonus, contract_allowance, " +
                     "contract_months, created_by, created_at, updated_at FROM employees " +
                     "ORDER BY employee_id ASC LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<Employee> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error fetching paged employees (offset=" + offset + ", limit=" + limit + ")", e);
            throw new DataAccessException("Failed to fetch employee page from database.", e);
        }
    }

    @Override
    public List<Employee> searchByName(String keyword) throws DataAccessException {
        String sql = "SELECT employee_id, name, basic_salary, employee_type, bonus, contract_allowance, " +
                     "contract_months, created_by, created_at, updated_at FROM employees " +
                     "WHERE name LIKE ? ORDER BY employee_id ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<Employee> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error searching employees by keyword: " + keyword, e);
            throw new DataAccessException("Failed to search employees in database.", e);
        }
    }

    @Override
    public boolean update(Employee employee) throws DataAccessException {
        String sql = "UPDATE employees SET name = ?, basic_salary = ?, employee_type = ?, " +
                     "bonus = ?, contract_allowance = ?, contract_months = ? WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindEmployee(ps, employee);
            ps.setInt(7, employee.getEmployeeId());

            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error updating employee ID: " + employee.getEmployeeId(), e);
            throw new DataAccessException("Failed to update employee in database: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(int employeeId) throws DataAccessException {
        String sql = "DELETE FROM employees WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error deleting employee ID: " + employeeId, e);
            throw new DataAccessException("Failed to delete employee from database: " + e.getMessage(), e);
        }
    }

    @Override
    public int count() throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM employees";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error counting employees", e);
            throw new DataAccessException("Failed to count employees in database.", e);
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
            throw new IllegalArgumentException("Unsupported employee subclass: " + e.getClass().getName());
        }
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("employee_id");
        String name = rs.getString("name");
        double basic = rs.getBigDecimal("basic_salary").doubleValue();
        String typeStr = rs.getString("employee_type");
        EmployeeType type = EmployeeType.valueOf(typeStr);

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
        if (!rs.wasNull()) {
            emp.setCreatedBy(createdByVal);
        }
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            emp.setCreatedAt(createdTs.toLocalDateTime());
        }
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) {
            emp.setUpdatedAt(updatedTs.toLocalDateTime());
        }
        return emp;
    }
}

