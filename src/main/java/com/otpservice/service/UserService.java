package com.otpservice.service;
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.otpservice.dao.UserDao;
import com.otpservice.model.User;
import com.otpservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDao;
    public UserService() {
        this.userDao = new UserDao();
    }
    public User register(String username, String password, User.Role role) {
        logger.info("Registering new user with username: {} and role: {}", username, role);
        if (userDao.findByUsername(username).isPresent()) {
            logger.warn("User with username {} already exists", username);
            throw new IllegalArgumentException("User with username " + username + " already exists");
        }
        if (role == User.Role.ADMIN && userDao.existsByRole(User.Role.ADMIN)) {
            logger.warn("Admin user already exists, cannot create another one");
            throw new IllegalArgumentException("Admin user already exists, cannot create another one");
        }
        String hashedPassword = hashPassword(password);
        User user = new User(username, hashedPassword, role);
        User savedUser = userDao.save(user);
        logger.info("User successfully registered with ID: {}", savedUser.getId());
        return savedUser;
    }
    public String authenticate(String username, String password) {
        logger.info("Attempting to authenticate user: {}", username);
        try {
            logger.debug("Finding user by username in database");
            Optional<User> userOptional = userDao.findByUsername(username);
            if (userOptional.isEmpty()) {
                logger.warn("Authentication failed: user {} not found", username);
                throw new IllegalArgumentException("Invalid username or password");
            }
            User user = userOptional.get();
            logger.debug("User found: {}, checking password", user.getId());
            logger.debug("Verifying password");
            if (!verifyPassword(password, user.getPassword())) {
                logger.warn("Authentication failed: wrong password for user {}", username);
                throw new IllegalArgumentException("Invalid username or password");
            }
            logger.debug("Password verified, generating JWT token");
            String token = JwtUtil.generateToken(user);
            logger.info("User {} successfully authenticated", username);
            return token;
        } catch (Exception e) {
            logger.error("Unexpected error during authentication for user {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }
    public List<User> getAllNonAdminUsers() {
        logger.info("Getting all non-admin users");
        return userDao.findAllNonAdminUsers();
    }
    public boolean deleteUser(Long userId) {
        logger.info("Deleting user with ID: {}", userId);
        Optional<User> userOptional = userDao.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("Cannot delete: user with ID {} not found", userId);
            return false;
        }
        User user = userOptional.get();
        if (user.getRole() == User.Role.ADMIN) {
            logger.warn("Cannot delete admin user with ID: {}", userId);
            throw new IllegalArgumentException("Cannot delete admin user");
        }
        boolean deleted = userDao.deleteById(userId);
        if (deleted) {
            logger.info("User with ID {} successfully deleted", userId);
        } else {
            logger.warn("Failed to delete user with ID: {}", userId);
        }
        return deleted;
    }
    public Optional<User> getUserByUsername(String username) {
        logger.debug("Finding user by username: {}", username);
        return userDao.findByUsername(username);
    }
    public Optional<User> getUserById(Long userId) {
        logger.debug("Finding user by ID: {}", userId);
        return userDao.findById(userId);
    }
    private String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }
    private boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified;
    }
} 