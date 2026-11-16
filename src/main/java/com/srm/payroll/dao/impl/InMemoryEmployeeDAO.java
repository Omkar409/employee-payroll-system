package com.srm.payroll.dao.impl;

import com.srm.payroll.dao.DataAccessException;
import com.srm.payroll.dao.EmployeeDAO;
import com.srm.payroll.model.Employee;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryEmployeeDAO implements EmployeeDAO {

    private final Map<Integer, Employee> storage = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(1000);

    @Override
    public int insert(Employee employee, int createdByUserId) throws DataAccessException {
        int id = idSequence.incrementAndGet();
        employee.setEmployeeId(id);
        if (createdByUserId > 0) {
            employee.setCreatedBy(createdByUserId);
        }
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        storage.put(id, employee);
        return id;
    }

    @Override
    public Optional<Employee> findById(int employeeId) throws DataAccessException {
        return Optional.ofNullable(storage.get(employeeId));
    }

    @Override
    public List<Employee> findAll() throws DataAccessException {
        List<Employee> list = new ArrayList<>(storage.values());
        list.sort(Comparator.comparingInt(Employee::getEmployeeId));
        return list;
    }

    @Override
    public List<Employee> findPage(int offset, int limit) throws DataAccessException {
        List<Employee> all = findAll();
        if (offset >= all.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(offset + limit, all.size());
        return all.subList(offset, toIndex);
    }

    @Override
    public List<Employee> searchByName(String keyword) throws DataAccessException {
        String lower = keyword.toLowerCase();
        List<Employee> matches = new ArrayList<>();
        for (Employee e : storage.values()) {
            if (e.getName().toLowerCase().contains(lower)) {
                matches.add(e);
            }
        }
        matches.sort(Comparator.comparingInt(Employee::getEmployeeId));
        return matches;
    }

    @Override
    public boolean update(Employee employee) throws DataAccessException {
        if (!storage.containsKey(employee.getEmployeeId())) {
            return false;
        }
        employee.setUpdatedAt(LocalDateTime.now());
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
}

