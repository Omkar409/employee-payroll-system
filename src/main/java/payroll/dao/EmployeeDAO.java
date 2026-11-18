package payroll.dao;

import payroll.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeDAO {
    int insert(Employee employee, int createdByUserId) throws DataAccessException;
    Optional<Employee> findById(int employeeId) throws DataAccessException;
    List<Employee> findAll() throws DataAccessException;
    List<Employee> findPage(int offset, int limit) throws DataAccessException;
    List<Employee> searchByName(String keyword) throws DataAccessException;
    boolean update(Employee employee) throws DataAccessException;
    boolean deleteById(int employeeId) throws DataAccessException;
    int count() throws DataAccessException;
}
