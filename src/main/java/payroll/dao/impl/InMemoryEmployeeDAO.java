package payroll.dao.impl;

import payroll.Employee;
import payroll.dao.DataAccessException;
import payroll.dao.EmployeeDAO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory implementation of EmployeeDAO.
 * Used for testing or offline operation without a live database.
 */
public class InMemoryEmployeeDAO implements EmployeeDAO {

    private final Map<Integer, Employee> storage = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(100);

    @Override
    public int insert(Employee employee, int createdByUserId) throws DataAccessException {
        int id = employee.getEmployeeId();
        if (id <= 0) {
            id = idSequence.incrementAndGet();
            employee.setEmployeeId(id);
        } else if (storage.containsKey(id)) {
            throw new DataAccessException("Employee ID " + id + " already exists in memory repository.");
        }
        if (createdByUserId > 0) {
            employee.setCreatedBy(createdByUserId);
        }
        storage.put(id, employee);
        return id;
    }

    @Override
    public void insertBatch(List<Employee> employees, int createdByUserId) throws DataAccessException {
        if (employees == null || employees.isEmpty()) return;
        for (Employee e : employees) {
            insert(e, createdByUserId);
        }
    }

    @Override
    public Optional<Employee> findById(int employeeId) throws DataAccessException {
        return Optional.ofNullable(storage.get(employeeId));
    }

    @Override
    public List<Employee> findAll() throws DataAccessException {
        return storage.values().stream()
                .sorted(Comparator.comparingInt(Employee::getEmployeeId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Employee> findPage(int offset, int limit) throws DataAccessException {
        return storage.values().stream()
                .sorted(Comparator.comparingInt(Employee::getEmployeeId))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Employee> searchByName(String keyword) throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String lower = keyword.trim().toLowerCase();
        return storage.values().stream()
                .filter(e -> e.getName().toLowerCase().contains(lower))
                .sorted(Comparator.comparingInt(Employee::getEmployeeId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean update(Employee employee) throws DataAccessException {
        if (!storage.containsKey(employee.getEmployeeId())) {
            return false;
        }
        storage.put(employee.getEmployeeId(), employee);
        return true;
    }

    @Override
    public boolean deleteById(int employeeId) throws DataAccessException {
        return storage.remove(employeeId) != null;
    }

    @Override
    public int count() throws DataAccessException {
        return storage.size();
    }

    public void clear() {
        storage.clear();
    }
}
