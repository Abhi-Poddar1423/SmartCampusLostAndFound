package com.campus.lostfound.dao;

import com.campus.lostfound.config.DBConnection;
import com.campus.lostfound.models.User;
import com.campus.lostfound.utils.PasswordUtil;
import com.campus.lostfound.utils.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO (Data Access Object)
 * 
 * Handles all database operations for the 'users' table using PreparedStatements.
 * Ensures SQL injection safety and robust connection management.
 */
public class UserDAO {

    // Cached schema check to ensure backward compatibility before and after migration
    private static volatile Boolean profileColumnsAvailable = null;

    public static void resetSchemaCache() {
        profileColumnsAvailable = null;
    }

    private static boolean areProfileColumnsPresent(Connection conn) {
        if (profileColumnsAvailable != null) {
            return profileColumnsAvailable;
        }
        synchronized (UserDAO.class) {
            if (profileColumnsAvailable != null) {
                return profileColumnsAvailable;
            }
            try {
                java.sql.DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "users", "roll_number")) {
                    profileColumnsAvailable = rs.next();
                }
            } catch (SQLException e) {
                profileColumnsAvailable = false;
            }
        }
        return profileColumnsAvailable != null && profileColumnsAvailable;
    }

    private String getSelectColumns(boolean hasProfile) {
        if (hasProfile) {
            return "user_id, name, email, roll_number, department, semester, phone_number, phone_public, password, role, created_at";
        } else {
            return "user_id, name, email, password, role, created_at";
        }
    }

    /**
     * Registers a new user in the 'users' table.
     * The password must already be hashed using PasswordUtil.hashPassword() before calling this.
     * 
     * @param user User object containing name, email, hashed password, and role
     * @return true if registration succeeded, false otherwise
     */
    public boolean register(User user) {
        // Backend Security Guard: Public / standard registration must ALWAYS assign STUDENT role.
        // Ordinary users can NEVER choose or force ADMIN during standard registration.
        // Admin account must only be initialized via AdminSetup.
        user.setRole(User.Role.STUDENT);

        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRole().name()); // Matches ENUM('STUDENT','ADMIN')

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error registering user: " + e.getMessage());
        }
        return false;
    }

    /**
     * Finds a user by their registered email address.
     * 
     * @param email User's email
     * @return User object if found, or null if not found
     */
    public User findByEmail(String email) {
        try (Connection conn = DBConnection.getConnection()) {
            boolean hasProfile = areProfileColumnsPresent(conn);
            String sql = "SELECT " + getSelectColumns(hasProfile) + " FROM users WHERE email = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error finding user by email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds a user by their unique primary key ID.
     * 
     * @param userId The ID of the user
     * @return User object if found, or null if not found
     */
    public User findById(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            boolean hasProfile = areProfileColumnsPresent(conn);
            String sql = "SELECT " + getSelectColumns(hasProfile) + " FROM users WHERE user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error finding user by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks if an email is already registered in the system.
     * 
     * @param email Email to verify
     * @return true if already in use, false otherwise
     */
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error checking email existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves all registered users (for admin management).
     * 
     * @return List of all users
     */
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            boolean hasProfile = areProfileColumnsPresent(conn);
            String sql = "SELECT " + getSelectColumns(hasProfile) + " FROM users ORDER BY user_id DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    userList.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error fetching all users: " + e.getMessage());
        }
        return userList;
    }

    /**
     * Updates a user's role (e.g. promoting a user to ADMIN).
     * 
     * @param userId ID of the user
     * @param newRole New Role to set
     * @return true if updated, false otherwise
     */
    public boolean updateUserRole(int userId, User.Role newRole) {
        int currentUserId = SessionManager.getInstance().getUserId();
        boolean isSessionAdmin = currentUserId > 0 && isActiveAdmin(currentUserId);

        // Security check: Only an active Administrator may update user roles.
        // Initial setup bootstrap has been completely removed from this method.
        // Initial setup MUST be performed strictly through executeInitialAdminBootstrap() via AdminSetup.
        if (!isSessionAdmin) {
            throw new SecurityException("Unauthorized: Only an active Administrator may update user roles.");
        }

        if (newRole == User.Role.ADMIN) {
            int adminCount = countAdminUsers();
            User target = findById(userId);
            if (adminCount > 0 && (target == null || target.getRole() != User.Role.ADMIN)) {
                System.err.println("[UserDAO] Security guard: Exactly one active Administrator is permitted in the system.");
                return false;
            }
        }

        String sql = "UPDATE users SET role = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole.name());
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating user role: " + e.getMessage());
        }
        return false;
    }

    /**
     * Executes the secure, one-time initial Administrator bootstrap.
     * 
     * Requirements enforced:
     * 1. Caller verification: Rejects calls unless invoked from com.campus.lostfound.utils.AdminSetup.
     * 2. Session check: Student sessions are strictly prohibited from bootstrapping an Administrator.
     * 3. One-Time Guarantee: Database must contain 0 active administrators; fails if an admin already exists.
     * 4. Concurrency Protection: Uses synchronized locking and SELECT ... FOR UPDATE to serialize concurrent attempts.
     * 5. Password Security: Passwords are automatically hashed with PBKDF2WithHmacSHA256.
     * 
     * @param name Admin full name
     * @param email Admin email
     * @param plainPassword Admin plain text password
     * @return true if administrator was successfully created or elevated
     * @throws SecurityException if caller or session is unauthorized
     * @throws IllegalStateException if an administrator already exists
     */
    public synchronized boolean executeInitialAdminBootstrap(String name, String email, String plainPassword) {
        // 1. Session check: Active student sessions cannot bootstrap administrator
        if (SessionManager.getInstance().isLoggedIn() && !SessionManager.getInstance().isAdmin()) {
            throw new SecurityException("Unauthorized: Cannot bootstrap Administrator while a Student session is logged in.");
        }

        // 2. Caller verification: Must originate from com.campus.lostfound.utils.AdminSetup
        boolean authorizedCaller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(frames -> frames.anyMatch(f -> f.getDeclaringClass().getName().equals("com.campus.lostfound.utils.AdminSetup")));

        if (!authorizedCaller) {
            throw new SecurityException("Unauthorized: Initial Administrator bootstrap may only be executed through com.campus.lostfound.utils.AdminSetup.");
        }

        if (name == null || name.trim().length() < 2) {
            throw new IllegalArgumentException("Administrator name must be at least 2 characters.");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid administrator email.");
        }
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new IllegalArgumentException("Administrator password must be at least 6 characters.");
        }

        String countAdminSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        String checkEmailSql = "SELECT user_id, role FROM users WHERE email = ?";
        String updateRoleSql = "UPDATE users SET name = ?, password = ?, role = 'ADMIN' WHERE user_id = ?";
        String insertAdminSql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'ADMIN')";

        Connection conn = null;
        boolean lockAcquired = false;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Cross-process and cross-thread concurrency guard via MySQL GET_LOCK
            try (PreparedStatement lockStmt = conn.prepareStatement("SELECT GET_LOCK('admin_bootstrap_lock', 10)")) {
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 1) {
                        lockAcquired = true;
                    }
                }
            }

            if (!lockAcquired) {
                throw new IllegalStateException("Could not acquire bootstrap lock. Another bootstrap process may be running.");
            }

            // Verify that NO administrator exists in the database
            int existingAdmins = 0;
            try (PreparedStatement countStmt = conn.prepareStatement(countAdminSql);
                 ResultSet rsCount = countStmt.executeQuery()) {
                if (rsCount.next()) {
                    existingAdmins = rsCount.getInt(1);
                }
            }

            if (existingAdmins > 0) {
                conn.rollback();
                throw new IllegalStateException("Initial Administrator setup has already been completed. An active Administrator already exists.");
            }

            String hashedPassword = PasswordUtil.hashPassword(plainPassword);

            // Check if user already exists with this email
            Integer existingUserId = null;
            try (PreparedStatement emailStmt = conn.prepareStatement(checkEmailSql)) {
                emailStmt.setString(1, email.trim());
                try (ResultSet rsEmail = emailStmt.executeQuery()) {
                    if (rsEmail.next()) {
                        existingUserId = rsEmail.getInt("user_id");
                    }
                }
            }

            boolean success;
            if (existingUserId != null) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateRoleSql)) {
                    updateStmt.setString(1, name.trim());
                    updateStmt.setString(2, hashedPassword);
                    updateStmt.setInt(3, existingUserId);
                    success = updateStmt.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertAdminSql)) {
                    insertStmt.setString(1, name.trim());
                    insertStmt.setString(2, email.trim());
                    insertStmt.setString(3, hashedPassword);
                    success = insertStmt.executeUpdate() > 0;
                }
            }

            if (success) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[UserDAO] Error rolling back bootstrap: " + ex.getMessage());
                }
            }
            System.err.println("[UserDAO] Database error during initial Admin bootstrap: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                if (lockAcquired) {
                    try (PreparedStatement releaseStmt = conn.prepareStatement("SELECT RELEASE_LOCK('admin_bootstrap_lock')");
                         ResultSet rs = releaseStmt.executeQuery()) {
                        // Lock released
                    } catch (SQLException e) {
                        System.err.println("[UserDAO] Error releasing bootstrap lock: " + e.getMessage());
                    }
                }
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[UserDAO] Error restoring connection auto-commit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Checks if a user is currently the active administrator.
     * 
     * @param userId User ID to check
     * @return true if currently active ADMIN, false otherwise
     */
    public boolean isActiveAdmin(int userId) {
        if (userId <= 0) return false;
        String sql = "SELECT 1 FROM users WHERE user_id = ? AND role = 'ADMIN'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Retrieves the primary key ID of the single active administrator.
     * 
     * @return User ID of active admin, or -1 if none found
     */
    public int getActiveAdminUserId() {
        String sql = "SELECT user_id FROM users WHERE role = 'ADMIN' ORDER BY user_id ASC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error retrieving active admin ID: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Deletes a user by their ID.
     * Enforces backend administrator authorization and self/last-admin protection.
     * 
     * @param userId ID of the user to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteUser(int userId) {
        int currentUserId = SessionManager.getInstance().getUserId();
        if (currentUserId <= 0 || !isActiveAdmin(currentUserId)) {
            throw new SecurityException("Unauthorized: Only an active Administrator may delete user accounts.");
        }
        if (currentUserId == userId) {
            throw new SecurityException("Security violation: Administrators cannot delete their own account.");
        }

        User target = findById(userId);
        if (target != null && target.getRole() == User.Role.ADMIN && countAdminUsers() <= 1) {
            throw new SecurityException("Security violation: Cannot delete the last active Administrator account.");
        }

        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error deleting user: " + e.getMessage());
        }
        return false;
    }

    /**
     * Counts how many users currently have the ADMIN role.
     * Used to prevent deleting or demoting the last active administrator.
     * 
     * @return Number of active admin accounts
     */
    public int countAdminUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error counting admins: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Checks if a user has any existing reported items or submitted claims.
     * Used to prevent foreign key constraint violations upon deletion.
     * 
     * @param userId User ID to check
     * @return true if related records exist, false if safe to delete
     */
    public boolean hasUserRelatedRecords(int userId) {
        String sql = "SELECT " +
                     "  (SELECT COUNT(*) FROM items WHERE reported_by = ?) + " +
                     "  (SELECT COUNT(*) FROM claims WHERE claimed_by = ?) AS total_rel";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_rel") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error checking user related records: " + e.getMessage());
        }
        return false;
    }

    /**
     * Returns the total number of registered users.
     */
    public int getTotalUsersCount() {
        String sql = "SELECT COUNT(*) FROM users";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error getting user count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Updates a user's campus profile fields (roll number, department, semester, phone, phone visibility).
     * 
     * @param user User object containing updated profile data
     * @return true if updated successfully, false otherwise
     */
    public boolean updateProfile(User user) {
        String sql = "UPDATE users SET roll_number = ?, department = ?, semester = ?, phone_number = ?, phone_public = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getRollNumber() != null && !user.getRollNumber().trim().isEmpty() ? user.getRollNumber().trim() : null);
            pstmt.setString(2, user.getDepartment() != null && !user.getDepartment().trim().isEmpty() ? user.getDepartment().trim() : null);
            pstmt.setString(3, user.getSemester() != null && !user.getSemester().trim().isEmpty() ? user.getSemester().trim() : null);
            pstmt.setString(4, user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty() ? user.getPhoneNumber().trim() : null);
            pstmt.setBoolean(5, user.isPhonePublic());
            pstmt.setInt(6, user.getUserId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating user profile: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a roll number is already assigned to a different user.
     * 
     * @param rollNumber Roll number to verify
     * @param excludeUserId User ID to exclude (current user)
     * @return true if taken by another user, false if available
     */
    public boolean isRollNumberTaken(String rollNumber, int excludeUserId) {
        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT 1 FROM users WHERE roll_number = ? AND user_id != ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rollNumber.trim());
            pstmt.setInt(2, excludeUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error checking roll number uniqueness: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the contact details of a peer user with strict backend authorization.
     * 
     * Authorization rules:
     * 1. Self-lookup: Always allowed.
     * 2. Administrator: Authorized for system moderation.
     * 3. Student-to-student: Authorized ONLY IF an approved claim links the two users on the specified item.
     *    A similarity score alone NEVER authorizes access to private contact info.
     * 4. Privacy masking: Phone number is masked (null) unless the user has set phone_public = true (or requester is Admin).
     *    Password hashes are never returned.
     * 
     * @param requesterUserId ID of student or admin requesting contact details
     * @param targetUserId ID of the student whose contact is being requested
     * @param itemId ID of the item linking the two users
     * @return Sanitized User model if authorized, or null if unauthorized / rejected
     */
    /**
     * Retrieves an authorized student contact record.
     * Enforces strict backend authorization:
     * - Requesting user must be authenticated.
     * - Requester ID must match active session (preventing forged GUI parameters).
     * - Requires an APPROVED claim on the given item.
     * - Item status must NOT be RETURNED (contact visibility is revoked upon item return).
     * - Requester must be either the approved claimant or the original item reporter (or an Admin).
     * - Strips password hash and sets name to null (name excluded from contact section).
     * - Masks phone number if phone_public == false and requester is not Admin.
     * 
     * @param requesterUserId ID of the user requesting contact
     * @param targetUserId ID of the target user whose contact is requested
     * @param itemId ID of the item linking the two users
     * @return Sanitized User model if authorized, or null if unauthorized / rejected
     */
    public User getAuthorizedContact(int requesterUserId, int targetUserId, int itemId) {
        if (requesterUserId <= 0 || targetUserId <= 0) {
            return null;
        }

        // 1. Session integrity check
        if (!SessionManager.getInstance().isLoggedIn()) {
            return null;
        }

        int sessionUserId = SessionManager.getInstance().getUserId();
        boolean isAdminRequester = SessionManager.getInstance().isAdmin();

        // Security Guard: Prohibit spoofed requesterUserId from non-admin callers
        if (!isAdminRequester && requesterUserId != sessionUserId) {
            throw new SecurityException("Security Violation: Requesting user ID does not match active authenticated session.");
        }

        // 2. Self-lookup
        if (requesterUserId == targetUserId) {
            User self = findById(targetUserId);
            if (self != null) {
                self.setPassword(null);
                self.setName(null);
            }
            return self;
        }

        // 3. Authorization check
        boolean isAuthorized = false;

        if (isAdminRequester) {
            isAuthorized = true;
        } else if (itemId > 0) {
            // Check for approved claim linking requester and target on this item, ensuring item is NOT RETURNED
            String authSql = "SELECT 1 FROM claims c " +
                             "JOIN items i ON c.item_id = i.item_id " +
                             "WHERE c.item_id = ? AND c.status = 'APPROVED' AND i.status != 'RETURNED' " +
                             "  AND ((c.claimed_by = ? AND i.reported_by = ?) OR (i.reported_by = ? AND c.claimed_by = ?))";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(authSql)) {

                pstmt.setInt(1, itemId);
                pstmt.setInt(2, requesterUserId);
                pstmt.setInt(3, targetUserId);
                pstmt.setInt(4, requesterUserId);
                pstmt.setInt(5, targetUserId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    isAuthorized = rs.next();
                }
            } catch (SQLException e) {
                System.err.println("[UserDAO] Error verifying contact authorization: " + e.getMessage());
            }
        }

        if (!isAuthorized) {
            return null; // Reject access
        }

        // 4. Authorized: Fetch target user profile
        User targetUser = findById(targetUserId);
        if (targetUser == null) {
            return null;
        }

        // 5. Security & Privacy Sanitization:
        targetUser.setPassword(null); // Never leak password hash
        targetUser.setName(null);     // Mandatory: Name excluded from contact section

        // 6. Privacy masking: Mask phone number if private and requester is not Admin
        if (!targetUser.isPhonePublic() && !isAdminRequester) {
            targetUser.setPhoneNumber(null); // Masked / Private
        }

        return targetUser;
    }

    /**
     * Resolves the authorized peer contact for an item (e.g. Finder for an approved Claimant,
     * or Claimant for the Finder).
     * Enforces active session authentication and revokes access if item is RETURNED.
     * 
     * @param requesterUserId ID of student requesting contact
     * @param itemId ID of the item
     * @return Authorized peer User, or null if no approved claim, item returned, or requester unauthorized
     */
    public User getAuthorizedPeerContactForItem(int requesterUserId, int itemId) {
        if (requesterUserId <= 0 || itemId <= 0) {
            return null;
        }

        // Session integrity check
        if (!SessionManager.getInstance().isLoggedIn()) {
            return null;
        }

        int sessionUserId = SessionManager.getInstance().getUserId();
        boolean isAdminRequester = SessionManager.getInstance().isAdmin();

        if (!isAdminRequester && requesterUserId != sessionUserId) {
            throw new SecurityException("Security Violation: Requesting user ID does not match active authenticated session.");
        }

        String sql = "SELECT i.reported_by, c.claimed_by FROM items i " +
                     "JOIN claims c ON i.item_id = c.item_id " +
                     "WHERE i.item_id = ? AND c.status = 'APPROVED' AND i.status != 'RETURNED'";

        int targetUserId = -1;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int reporterId = rs.getInt("reported_by");
                    int claimantId = rs.getInt("claimed_by");

                    if (requesterUserId == reporterId) {
                        targetUserId = claimantId; // Peer is the claimant
                    } else if (requesterUserId == claimantId) {
                        targetUserId = reporterId; // Peer is the finder
                    } else if (isAdminRequester) {
                        targetUserId = claimantId; // Admin can view claimant or reporter
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error resolving peer contact: " + e.getMessage());
        }

        if (targetUserId <= 0) {
            return null;
        }

        return getAuthorizedContact(requesterUserId, targetUserId, itemId);
    }

    /**
     * Helper method to convert a ResultSet row into a User model.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(User.Role.fromString(rs.getString("role")));
        user.setCreatedAt(rs.getTimestamp("created_at"));

        if (hasColumn(rs, "roll_number")) {
            user.setRollNumber(rs.getString("roll_number"));
        }
        if (hasColumn(rs, "department")) {
            user.setDepartment(rs.getString("department"));
        }
        if (hasColumn(rs, "semester")) {
            user.setSemester(rs.getString("semester"));
        }
        if (hasColumn(rs, "phone_number")) {
            user.setPhoneNumber(rs.getString("phone_number"));
        }
        if (hasColumn(rs, "phone_public")) {
            user.setPhonePublic(rs.getBoolean("phone_public"));
        }
        return user;
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
