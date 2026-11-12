package com.srm.payroll.dao;

import com.srm.payroll.model.User;
import java.util.Optional;

public interface UserDAO {
    int insert(User user) throws DataAccessException;
    Optional<User> findByUsername(String username) throws DataAccessException;
    boolean existsByUsername(String username) throws DataAccessException;
    boolean existsByEmail(String email) throws DataAccessException;
    void updateLastLogin(int userId) throws DataAccessException;
}
