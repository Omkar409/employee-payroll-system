package payroll.service;

import payroll.*;
import payroll.dao.DataAccessException;
import payroll.dao.EmployeeDAO;
import payroll.service.exception.ValidationException;
import payroll.util.InputValidator;

import java.util.List;
import java.util.Optional;

public class EmployeeService {

    private final EmployeeDAO employeeDAO;
    private final PayrollCalculator calculator;

    public EmployeeService(EmployeeDAO employeeDAO, PayrollCalculator calculator) {
        this.employeeDAO = employeeDAO;
        this.calculator = calculator;
    }

    public int addEmployee(Employee e) throws ValidationException, DataAccessException {
        validateEmployee(e);
        User current = SessionContext.getCurrentUser();
        int createdByUserId = current != null ? current.getUserId() : 0;
        return employeeDAO.insert(e, createdByUserId);
    }

    public void addBatch(List<Employee> employees) throws DataAccessException {
        User current = SessionContext.getCurrentUser();
        int createdByUserId = current != null ? current.getUserId() : 0;
        employeeDAO.insertBatch(employees, createdByUserId);
    }

    public Optional<Employee> findById(int id) throws DataAccessException {
        if (id <= 0) return Optional.empty();
        return employeeDAO.findById(id);
    }

    public List<Employee> listAll() throws DataAccessException {
        return employeeDAO.findAll();
    }

    public List<Employee> searchByName(String keyword) throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) return employeeDAO.findAll();
        return employeeDAO.searchByName(keyword.trim());
    }

    public void updateEmployee(Employee e) throws ValidationException, DataAccessException {
        if (e.getEmployeeId() <= 0) throw new ValidationException("Invalid employee ID for update.");
        validateEmployee(e);
        if (!employeeDAO.update(e))
            throw new DataAccessException("Employee with ID " + e.getEmployeeId() + " was not found for update.");
    }

    public void deleteEmployee(int id) throws SecurityException, DataAccessException {
        if (!SessionContext.hasRole(Role.ADMIN))
            throw new SecurityException("Access denied: only ADMIN users can delete employees.");
        if (!employeeDAO.deleteById(id))
            throw new DataAccessException("Employee with ID " + id + " was not found.");
    }

    public PayrollResult calculatePayroll(int employeeId) throws DataAccessException {
        Optional<Employee> opt = employeeDAO.findById(employeeId);
        if (opt.isEmpty()) throw new DataAccessException("Employee with ID " + employeeId + " not found.");
        return calculator.calculate(opt.get());
    }

    public int count() throws DataAccessException { return employeeDAO.count(); }

    private void validateEmployee(Employee e) throws ValidationException {
        if (e == null) throw new ValidationException("Employee data cannot be null.");
        InputValidator.validateName(e.getName());
        if (e.getBasicSalary() < 0) throw new ValidationException("Basic salary cannot be negative.");
        if (e.getBasicSalary() > 999_999_999.99) throw new ValidationException("Basic salary exceeds maximum.");
        if (e instanceof PermanentEmployee pe) {
            if (pe.getBonus() < 0) throw new ValidationException("Bonus cannot be negative.");
        } else if (e instanceof ContractEmployee ce) {
            if (ce.getContractAllowance() < 0) throw new ValidationException("Contract allowance cannot be negative.");
            if (ce.getContractMonths() <= 0) throw new ValidationException("Contract duration must be at least 1 month.");
        } else {
            throw new ValidationException("Unknown employee type: " + e.getEmployeeType());
        }
    }
}
