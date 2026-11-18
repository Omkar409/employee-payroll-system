package payroll.service;

import payroll.*;
import payroll.dao.DataAccessException;
import payroll.dao.UserDAO;
import payroll.service.exception.AuthenticationException;
import payroll.service.exception.DuplicateUserException;
import payroll.service.exception.ValidationException;
import payroll.util.InputValidator;
import payroll.util.PasswordHasher;

import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User register(String username, char[] password, String fullName,
                         String email, Role role)
            throws ValidationException, DuplicateUserException, DataAccessException {

        InputValidator.validateUsername(username);
        InputValidator.validateStrongPassword(password);
        InputValidator.validateNonBlank(fullName, "Full Name");
        InputValidator.validateEmail(email);

        if (role == null) role = Role.HR;

        if (userDAO.existsByUsername(username))
            throw new DuplicateUserException("Username '" + username + "' is already taken.");
        if (userDAO.existsByEmail(email))
            throw new DuplicateUserException("Email '" + email + "' is already registered.");

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(password, salt);
        User user = new User(username, hash, salt, fullName, email, role);

        try {
            int generatedId = userDAO.insert(user);
            user.setUserId(generatedId);
            return user;
        } catch (DataAccessException e) {
            if (e.getCause() != null && e.getCause().getMessage() != null &&
                    (e.getCause().getMessage().contains("Duplicate") || e.getMessage().contains("already exists")))
                throw new DuplicateUserException("A user with this username or email already exists.", e);
            throw e;
        }
    }

    public User login(String username, char[] password)
            throws AuthenticationException, DataAccessException {

        if (username == null || username.trim().isEmpty() || password == null || password.length == 0)
            throw new AuthenticationException("Invalid username or password.");

        Optional<User> optUser = userDAO.findByUsername(username.trim());
        if (optUser.isEmpty())
            throw new AuthenticationException("Invalid username or password.");

        User user = optUser.get();
        if (!user.isActive())
            throw new AuthenticationException("Account is deactivated. Please contact an administrator.");

        if (!PasswordHasher.verify(password, user.getPasswordSalt(), user.getPasswordHash()))
            throw new AuthenticationException("Invalid username or password.");

        userDAO.updateLastLogin(user.getUserId());
        return user;
    }
}
