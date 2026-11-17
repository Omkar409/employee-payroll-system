package payroll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thread-safe in-memory storage for employee records.
 * Demonstrates:
 * - Java Collections Framework: HashMap and ArrayList
 * - Synchronization: synchronized methods preventing race conditions
 * - Modern Java API: Optional<Employee> for safe nullable lookups
 * - Defensive copying: returning a new ArrayList in findAll()
 */
public class EmployeeRepository {
    private final Map<Integer, Employee> employees = new HashMap<>();

    public synchronized void add(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null.");
        }
        if (employees.containsKey(employee.getId())) {
            throw new IllegalArgumentException("Employee ID already exists: " + employee.getId());
        }
        employees.put(employee.getId(), employee);
    }

    public synchronized Optional<Employee> findById(int id) {
        return Optional.ofNullable(employees.get(id));
    }

    public synchronized List<Employee> findAll() {
        return new ArrayList<>(employees.values());
    }

    public synchronized boolean delete(int id) {
        return employees.remove(id) != null;
    }

    public synchronized int count() {
        return employees.size();
    }

    public synchronized void clear() {
        employees.clear();
    }
}
