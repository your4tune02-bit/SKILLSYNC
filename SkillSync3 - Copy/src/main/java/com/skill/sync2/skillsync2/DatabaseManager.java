package com.skill.sync2.skillsync2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseManager {

    // ─── Connection Pool ──────────────────────────────────────────────────────

    private static HikariDataSource dataSource;

    private static synchronized void ensureDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:skillsync.db");
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1);
            // Keep connections alive and handle timeouts gracefully
            config.setConnectionTimeout(10_000);
            config.setIdleTimeout(30_000);
            config.setMaxLifetime(60_000);
            dataSource = new HikariDataSource(config);
        }
    }

    public static Connection connect() throws SQLException {
        ensureDataSource();
        return dataSource.getConnection();
    }

    public static void disconnect() {
        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
    }

    // ─── Schema Initialization ────────────────────────────────────────────────

    public static void initialize() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Enable WAL mode for better SQLite concurrency (read/write don't block each
            // other)
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA foreign_keys=ON;");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS students (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL, password TEXT NOT NULL, email TEXT UNIQUE DEFAULT NULL, skill TEXT NOT NULL, bio TEXT DEFAULT 'Hello! I am using SkillSync.', last_seen DATETIME DEFAULT CURRENT_TIMESTAMP, is_online INTEGER DEFAULT 0);");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS projects (id INTEGER PRIMARY KEY AUTOINCREMENT, owner_name TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, required_role TEXT NOT NULL, responsibilities TEXT NOT NULL, due_date TEXT DEFAULT 'Not Specified');");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS invitations (id INTEGER PRIMARY KEY AUTOINCREMENT, receiver_name TEXT NOT NULL, sender_info TEXT NOT NULL, message TEXT NOT NULL, status TEXT DEFAULT 'PENDING', type TEXT DEFAULT 'TEAM', related_title TEXT DEFAULT '', is_read INTEGER DEFAULT 0);");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT NOT NULL, receiver TEXT NOT NULL, content TEXT NOT NULL, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, is_read INTEGER DEFAULT 0);");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS portfolio (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, file_name TEXT NOT NULL, file_path TEXT NOT NULL);");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, team_id TEXT NOT NULL, creator TEXT NOT NULL, title TEXT NOT NULL, assigned_to TEXT DEFAULT 'Unassigned', status TEXT DEFAULT 'PENDING', due_date TEXT DEFAULT 'None');");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS otp_codes (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT NOT NULL, code TEXT NOT NULL, type TEXT NOT NULL, created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS email_verified (username TEXT PRIMARY KEY, verified INTEGER DEFAULT 0);");

            // Migration guards: safely add columns that may be missing in older databases
            tryAlter(stmt, "ALTER TABLE tasks ADD COLUMN due_date TEXT DEFAULT 'None';");
            tryAlter(stmt, "ALTER TABLE students ADD COLUMN last_seen DATETIME DEFAULT CURRENT_TIMESTAMP;");
            tryAlter(stmt, "ALTER TABLE students ADD COLUMN is_online INTEGER DEFAULT 0;");
            tryAlter(stmt, "ALTER TABLE students ADD COLUMN email TEXT UNIQUE DEFAULT NULL;");
            tryAlter(stmt, "ALTER TABLE invitations ADD COLUMN is_read INTEGER DEFAULT 0;");
            tryAlter(stmt, "ALTER TABLE invitations ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP;");
            tryAlter(stmt, "ALTER TABLE messages ADD COLUMN is_read INTEGER DEFAULT 0;");
            // Project status: ACTIVE (default), DONE (owner closed it), EXPIRED (past due
            // date)
            tryAlter(stmt, "ALTER TABLE projects ADD COLUMN status TEXT DEFAULT 'ACTIVE';");
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] FATAL: Failed to initialize schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Silently ignores "duplicate column" errors from ALTER TABLE — safe migration
     * helper.
     */
    private static void tryAlter(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException ignored) {
        }
    }

    // ─── Password Utility ─────────────────────────────────────────────────────

    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // ─── Student / Auth ───────────────────────────────────────────────────────

    /**
     * Accepts either a username or an email address as the first argument.
     * Returns the resolved username on success, or null on failure.
     */
    public static String verifyLogin(String usernameOrEmail, String password) {
        String hashedPass = hashPassword(password);
        // Try direct username match first
        String byName = "SELECT name FROM students WHERE name = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(byName)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, hashedPass);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("name");
            }
        } catch (SQLException ignored) {
        }
        // Fall back to email match
        String byEmail = "SELECT name FROM students WHERE LOWER(email) = LOWER(?) AND password = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(byEmail)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, hashedPass);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("name");
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    /** Returns true if the given email is already registered. */
    public static boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM students WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    /** Returns the username associated with an email, or null if not found. */
    public static String getUsernameByEmail(String email) {
        String sql = "SELECT name FROM students WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("name");
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    /**
     * Changes a user's password looked up by email (used in forgot-password flow).
     */
    public static boolean changePasswordByEmail(String email, String newPassword) {
        String sql = "UPDATE students SET password = ? WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(newPassword));
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void addStudent(String name, String password, String email, String skill) throws SQLException {
        String sql = "INSERT INTO students(name, password, email, skill) VALUES(?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, hashPassword(password));
            ps.setString(3, email.isBlank() ? null : email);
            ps.setString(4, skill);
            ps.executeUpdate();
        }
    }

    public static Student getStudentProfile(String username) {
        String sql = "SELECT name, skill, bio FROM students WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return new Student(rs.getString("name"), rs.getString("skill"), rs.getString("bio"));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] getStudentProfile failed: " + e.getMessage());
            return new Student("NETWORK_ERROR", "", "");
        }
        return null; // user simply doesn't exist
    }

    public static void updateProfile(String username, String newSkills, String newBio) throws SQLException {
        String sql = "UPDATE students SET skill = ?, bio = ? WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newSkills);
            ps.setString(2, newBio);
            ps.setString(3, username);
            ps.executeUpdate();
        }
    }

    public static boolean changePassword(String username, String oldPassword, String newPassword) {
        if (verifyLogin(username, oldPassword) == null)
            return false;
        String sql = "UPDATE students SET password = ? WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(newPassword));
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void deleteAccount(String username) throws SQLException {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                exec(conn, "DELETE FROM messages WHERE sender = ? OR receiver = ?", username, username);
                exec(conn, "DELETE FROM invitations WHERE sender_info = ? OR receiver_name = ?", username, username);
                exec(conn, "DELETE FROM portfolio WHERE username = ?", username);
                exec(conn, "DELETE FROM projects WHERE owner_name = ?", username);
                exec(conn, "DELETE FROM tasks WHERE creator = ? OR assigned_to = ?", username, username);
                exec(conn, "DELETE FROM students WHERE name = ?", username);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void exec(Connection conn, String sql, String... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++)
                ps.setString(i + 1, params[i]);
            ps.executeUpdate();
        }
    }

    // ─── Search & Recommendations ─────────────────────────────────────────────

    public static List<Student> searchStudents(String nameQuery, List<String> skills) {
        List<Student> results = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT name, skill, bio FROM students WHERE 1=1 ");
        if (nameQuery != null && !nameQuery.trim().isEmpty())
            sb.append("AND LOWER(name) LIKE LOWER(?) ");
        if (skills != null) {
            for (int i = 0; i < skills.size(); i++)
                sb.append("AND LOWER(skill) LIKE LOWER(?) ");
        }
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            int i = 1;
            if (nameQuery != null && !nameQuery.trim().isEmpty())
                ps.setString(i++, "%" + nameQuery.trim() + "%");
            if (skills != null) {
                for (String s : skills)
                    ps.setString(i++, "%" + s + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    results.add(new Student(rs.getString("name"), rs.getString("skill"), rs.getString("bio")));
            }
        } catch (SQLException ignored) {
        }
        return results;
    }

    public static List<Student> getRecommendations(String currentUser) {
        List<Student> results = new ArrayList<>();
        String sql = "SELECT name, skill, bio FROM students WHERE name != ? ORDER BY RANDOM() LIMIT 3";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    results.add(new Student(rs.getString("name"), rs.getString("skill"), rs.getString("bio")));
            }
        } catch (SQLException ignored) {
        }
        return results;
    }

    public static String getRandomMatch(String currentUser, String desiredSkill) {
        String sql = "SELECT name FROM students WHERE skill LIKE ? AND name != ? ORDER BY RANDOM() LIMIT 1";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + desiredSkill + "%");
            ps.setString(2, currentUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("name");
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    // ─── Projects ─────────────────────────────────────────────────────────────

    public static void addProject(String ownerName, String title, String description, String role,
            String responsibilities, String dueDate) throws SQLException {
        String sql = "INSERT INTO projects(owner_name, title, description, required_role, responsibilities, due_date) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerName);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setString(4, role);
            ps.setString(5, responsibilities);
            ps.setString(6, dueDate);
            ps.executeUpdate();
        }
    }

    public static List<Project> getAllProjects() {
        // Auto-expire any ACTIVE projects whose due_date has passed (format:
        // yyyy-MM-dd)
        try (Connection conn = connect();
                PreparedStatement expire = conn.prepareStatement(
                        "UPDATE projects SET status = 'EXPIRED' " +
                                "WHERE status = 'ACTIVE' AND due_date != 'Not Specified' AND due_date != '' " +
                                "AND due_date IS NOT NULL AND date(due_date) < date('now')")) {
            expire.executeUpdate();
        } catch (SQLException ignored) {
        }

        List<Project> projects = new ArrayList<>();
        String sql = "SELECT id, owner_name, title, description, required_role, responsibilities, due_date, status FROM projects ORDER BY id DESC";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                projects.add(new Project(rs.getInt("id"), rs.getString("owner_name"), rs.getString("title"),
                        rs.getString("description"), rs.getString("required_role"), rs.getString("responsibilities"),
                        rs.getString("due_date"), rs.getString("status")));
        } catch (SQLException ignored) {
        }
        return projects;
    }

    public static Project getProjectByTitle(String title) {
        String sql = "SELECT id, owner_name, title, description, required_role, responsibilities, due_date, status FROM projects WHERE title = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return new Project(rs.getInt("id"), rs.getString("owner_name"), rs.getString("title"),
                            rs.getString("description"), rs.getString("required_role"),
                            rs.getString("responsibilities"), rs.getString("due_date"), rs.getString("status"));
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    public static void deleteProject(int id) throws SQLException {
        // First look up the project title so we can cascade-delete everything tied to
        // it
        String title = null;
        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement("SELECT title FROM projects WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    title = rs.getString("title");
            }
        }
        if (title == null)
            return; // already gone
        deleteProjectByTitle(title);
    }

    /**
     * Full cascade delete for a project identified by title.
     * Removes: the project row, all group-chat messages (#title), all tasks,
     * and all project-type invitations referencing this title.
     * Called by deleteProject(int) and can also be called when a project is
     * marked as "done" so the group chat disappears automatically.
     */
    public static void deleteProjectByTitle(String title) throws SQLException {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                // 1. Delete group-chat messages (receiver is "#<title>")
                exec(conn, "DELETE FROM messages WHERE receiver = ?", "#" + title);
                // 2. Delete all tasks for this team
                exec(conn, "DELETE FROM tasks WHERE team_id = ?", title);
                // 3. Delete all invitations (applications) related to this project
                exec(conn, "DELETE FROM invitations WHERE type = 'PROJECT' AND related_title = ?", title);
                // 4. Delete the project itself
                exec(conn, "DELETE FROM projects WHERE title = ?", title);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Marks a project as DONE (owner-initiated). Squad members can no longer
     * interact with the project's chat or task board after this.
     */
    public static void markProjectDone(int projectId) throws SQLException {
        String sql = "UPDATE projects SET status = 'DONE' WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        }
    }

    /**
     * Returns true when this project is locked (DONE or EXPIRED).
     * Used by controllers to gate chat, task-board, and apply actions.
     */
    public static boolean isProjectLocked(String title) {
        String sql = "SELECT status FROM projects WHERE title = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String s = rs.getString("status");
                    return "DONE".equals(s) || "EXPIRED".equals(s);
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    // ─── Invitations ──────────────────────────────────────────────────────────

    public static boolean sendInvite(String receiver, String senderInfo, String message, String type,
            String relatedTitle) throws SQLException {
        String checkSql = "TEAM".equals(type)
                ? "SELECT COUNT(*) FROM invitations WHERE ((receiver_name = ? AND sender_info = ?) OR (receiver_name = ? AND sender_info = ?)) AND type = 'TEAM'"
                : "SELECT COUNT(*) FROM invitations WHERE receiver_name = ? AND sender_info = ? AND type = 'PROJECT' AND related_title = ?";
        String insertSql = "INSERT INTO invitations(receiver_name, sender_info, message, type, related_title) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                // Check for duplicates within the same transaction to prevent race conditions
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    if ("TEAM".equals(type)) {
                        check.setString(1, receiver);
                        check.setString(2, senderInfo);
                        check.setString(3, senderInfo);
                        check.setString(4, receiver);
                    } else {
                        check.setString(1, receiver);
                        check.setString(2, senderInfo);
                        check.setString(3, relatedTitle);
                    }
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    insert.setString(1, receiver);
                    insert.setString(2, senderInfo);
                    insert.setString(3, message);
                    insert.setString(4, type);
                    insert.setString(5, relatedTitle);
                    insert.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static List<Invitation> getInvitations(String receiverName) {
        List<Invitation> invites = new ArrayList<>();
        String sql = "SELECT id, receiver_name, sender_info, message, status, type, related_title, created_at FROM invitations WHERE receiver_name = ? AND status != 'REJECTED' ORDER BY id DESC";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receiverName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    invites.add(new Invitation(rs.getInt("id"), rs.getString("receiver_name"),
                            rs.getString("sender_info"), rs.getString("message"), rs.getString("status"),
                            rs.getString("type"), rs.getString("related_title"), rs.getString("created_at")));
            }
        } catch (SQLException ignored) {
        }
        return invites;
    }

    public static void updateInviteStatus(int id, String status) throws SQLException {
        String sql = "UPDATE invitations SET status = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public static int getUnreadInviteCount(String username) {
        String sql = "SELECT COUNT(*) FROM invitations WHERE receiver_name = ? AND is_read = 0 AND status != 'REJECTED'";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public static void markInvitesAsRead(String username) {
        String sql = "UPDATE invitations SET is_read = 1 WHERE receiver_name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public static List<String> getRecentActivity(String username) {
        List<String> activity = new ArrayList<>();
        String sql = "SELECT sender_info, type FROM invitations WHERE receiver_name = ? ORDER BY id DESC LIMIT 5";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    activity.add(
                            rs.getString("sender_info") + ("TEAM".equals(rs.getString("type")) ? " sent a team invite."
                                    : " applied to your project."));
            }
        } catch (SQLException ignored) {
        }
        return activity;
    }

    // ─── Teaming Up ───────────────────────────────────────────────────────────

    public static boolean checkIsTeamedUp(String user1, String user2) {
        String sql = "SELECT COUNT(*) FROM invitations WHERE type = 'TEAM' AND status = 'ACCEPTED' AND ((receiver_name = ? AND sender_info = ?) OR (receiver_name = ? AND sender_info = ?))";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user1);
            ps.setString(2, user2);
            ps.setString(3, user2);
            ps.setString(4, user1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public static void unpairUsers(String user1, String user2) throws SQLException {
        String sql = "DELETE FROM invitations WHERE type = 'TEAM' AND status = 'ACCEPTED' AND ((receiver_name = ? AND sender_info = ?) OR (receiver_name = ? AND sender_info = ?))";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user1);
            ps.setString(2, user2);
            ps.setString(3, user2);
            ps.setString(4, user1);
            ps.executeUpdate();
        }
    }

    public static List<String> getUserTeams(String username) {
        // Use a LinkedHashSet to preserve insertion order while eliminating duplicates.
        // Duplicates arise when a user both owns a project AND has an accepted
        // application
        // for the same project title, which caused the same team to appear twice in
        // chat.
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        try (Connection conn = connect()) {
            try (PreparedStatement p1 = conn.prepareStatement("SELECT title FROM projects WHERE owner_name = ?")) {
                p1.setString(1, username);
                try (ResultSet rs = p1.executeQuery()) {
                    while (rs.next())
                        seen.add(rs.getString("title"));
                }
            }
            // Only pull accepted memberships whose parent project still exists (prevents
            // ghost chats)
            try (PreparedStatement p2 = conn.prepareStatement(
                    "SELECT i.related_title FROM invitations i " +
                            "WHERE i.sender_info = ? AND i.type = 'PROJECT' AND i.status = 'ACCEPTED' " +
                            "AND EXISTS (SELECT 1 FROM projects p WHERE p.title = i.related_title)")) {
                p2.setString(1, username);
                try (ResultSet rs = p2.executeQuery()) {
                    while (rs.next())
                        seen.add(rs.getString("related_title"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(seen);
    }

    public static List<String> getTeamMembers(String projectTitle) {
        List<String> members = new ArrayList<>();
        try (Connection conn = connect()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT owner_name FROM projects WHERE title = ?")) {
                ps.setString(1, projectTitle);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        members.add(rs.getString("owner_name"));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT sender_info FROM invitations WHERE type = 'PROJECT' AND status = 'ACCEPTED' AND related_title = ?")) {
                ps.setString(1, projectTitle);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String n = rs.getString("sender_info");
                        if (!members.contains(n))
                            members.add(n);
                    }
                }
            }
        } catch (SQLException ignored) {
        }
        return members;
    }

    public static void kickTeamMember(String projectTitle, String username) throws SQLException {
        String sql = "DELETE FROM invitations WHERE type = 'PROJECT' AND status = 'ACCEPTED' AND related_title = ? AND sender_info = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectTitle);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    // ─── Messages ─────────────────────────────────────────────────────────────

    public static void sendMessage(String sender, String receiver, String content) throws SQLException {
        String sql = "INSERT INTO messages(sender, receiver, content) VALUES(?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    public static List<Message> getChatHistory(String u1, String u2) {
        List<Message> history = new ArrayList<>();
        String sql = u2.startsWith("#")
                ? "SELECT sender, receiver, content, timestamp FROM messages WHERE receiver = ? ORDER BY id ASC"
                : "SELECT sender, receiver, content, timestamp FROM messages WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY id ASC";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (u2.startsWith("#"))
                ps.setString(1, u2);
            else {
                ps.setString(1, u1);
                ps.setString(2, u2);
                ps.setString(3, u2);
                ps.setString(4, u1);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    history.add(new Message(rs.getString("sender"), rs.getString("receiver"), rs.getString("content"),
                            rs.getString("timestamp")));
            }
        } catch (SQLException e) {
            return new ArrayList<>();
        }
        return history;
    }

    public static List<String> getChatContacts(String username) {
        List<String> contacts = new ArrayList<>();
        String sql = "SELECT DISTINCT sender AS contact FROM messages WHERE receiver = ? UNION SELECT DISTINCT receiver AS contact FROM messages WHERE sender = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    contacts.add(rs.getString("contact"));
            }
        } catch (SQLException ignored) {
        }
        return contacts;
    }

    public static int getUnreadMessageCount(String username) {
        String sql = "SELECT COUNT(*) FROM messages WHERE receiver = ? AND is_read = 0";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public static void markMessagesAsRead(String sender, String receiver) {
        String sql = "UPDATE messages SET is_read = 1 WHERE sender = ? AND receiver = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    // ─── Presence / Heartbeat ─────────────────────────────────────────────────

    public static void sendHeartbeat(String username) {
        String sql = "UPDATE students SET last_seen = CURRENT_TIMESTAMP WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public static void forceOffline(String username) {
        String sql = "UPDATE students SET last_seen = datetime('now', '-1 day') WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public static Set<String> getOnlineUsers() {
        Set<String> online = new HashSet<>();
        String sql = "SELECT name FROM students WHERE last_seen >= datetime('now', '-30 seconds')";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                online.add(rs.getString("name"));
        } catch (SQLException ignored) {
        }
        return online;
    }

    public static String getLastSeenStatus(String username) {
        String sql = "SELECT (julianday('now') - julianday(last_seen)) * 86400 as seconds_ago FROM students WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double secs = rs.getDouble("seconds_ago");
                    if (secs < 30)
                        return "\uD83D\uDFE2 Online now";
                    if (secs < 120)
                        return "Active just now";
                    if (secs < 3600)
                        return "Active " + (int) (secs / 60) + "m ago";
                    if (secs < 86400)
                        return "Active " + (int) (secs / 3600) + "h ago";
                    return "Last seen " + (int) (secs / 86400) + "d ago";
                }
            }
        } catch (SQLException ignored) {
        }
        return "Unknown";
    }

    // ─── Portfolio ────────────────────────────────────────────────────────────

    public static void addPortfolioItem(String username, String fileName, String filePath) throws SQLException {
        String sql = "INSERT INTO portfolio(username, file_name, file_path) VALUES(?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, fileName);
            ps.setString(3, filePath);
            ps.executeUpdate();
        }
    }

    public static List<PortfolioItem> getPortfolioItems(String username) {
        List<PortfolioItem> items = new ArrayList<>();
        String sql = "SELECT id, username, file_name, file_path FROM portfolio WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    items.add(new PortfolioItem(rs.getInt("id"), rs.getString("username"), rs.getString("file_name"),
                            rs.getString("file_path")));
            }
        } catch (SQLException ignored) {
        }
        return items;
    }

    public static void deletePortfolioItem(int id) throws SQLException {
        String sql = "DELETE FROM portfolio WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Tasks ────────────────────────────────────────────────────────────────

    public static void addTask(String teamId, String creator, String title, String dueDate) throws SQLException {
        String sql = "INSERT INTO tasks(team_id, creator, title, due_date) VALUES(?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, creator);
            ps.setString(3, title);
            ps.setString(4, dueDate);
            ps.executeUpdate();
        }
    }

    public static List<Task> getTasks(String teamId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, team_id, creator, title, assigned_to, status, due_date FROM tasks WHERE team_id = ? ORDER BY id DESC";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String assignedTo = rs.getString("assigned_to");
                    if (assignedTo == null)
                        assignedTo = "Unassigned";
                    String status = rs.getString("status");
                    if (status == null)
                        status = "PENDING";
                    tasks.add(new Task(rs.getInt("id"), rs.getString("team_id"), rs.getString("creator"),
                            rs.getString("title"), assignedTo, status, rs.getString("due_date")));
                }
            }
        } catch (SQLException e) {
            return new ArrayList<>();
        }
        return tasks;
    }

    public static void updateTaskStatus(int id, String status) throws SQLException {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public static boolean claimTask(int taskId, String username) {
        String sql = "UPDATE tasks SET assigned_to = ?, status = 'IN_PROGRESS' WHERE id = ? AND (assigned_to = 'Unassigned' OR assigned_to IS NULL)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void deleteTask(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static double getProjectPulse(String teamId) {
        String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END) as done_count FROM tasks WHERE team_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    if (total == 0)
                        return 0.0;
                    return ((double) rs.getInt("done_count") / total) * 100;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // ─── OTP & Email Verification ─────────────────────────────────────────────

    /**
     * Returns the email address registered to the given username, or null if not
     * found.
     */
    public static String getEmailByUsername(String username) {
        String sql = "SELECT email FROM students WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("email");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Stores a one-time password code for the given email and type (e.g. "SIGNUP",
     * "RESET").
     * Replaces any existing code for the same email+type.
     */
    public static void storeOtp(String email, String code, String type) {
        String del = "DELETE FROM otp_codes WHERE email = ? AND type = ?";
        String ins = "INSERT INTO otp_codes (email, code, type) VALUES (?, ?, ?)";
        try (Connection conn = connect()) {
            try (PreparedStatement ps = conn.prepareStatement(del)) {
                ps.setString(1, email);
                ps.setString(2, type);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                ps.setString(1, email);
                ps.setString(2, code);
                ps.setString(3, type);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifies an OTP code for the given email and type.
     * Returns true if a matching, unexpired (≤15 min) code exists, and deletes it
     * on success.
     */
    public static boolean verifyOtp(String email, String code, String type) {
        String sql = "SELECT id FROM otp_codes WHERE email = ? AND code = ? AND type = ? "
                + "AND created_at >= datetime('now', '-15 minutes')";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, code);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    try (PreparedStatement del = conn.prepareStatement(
                            "DELETE FROM otp_codes WHERE id = ?")) {
                        del.setInt(1, id);
                        del.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Marks the given username's email as verified in the email_verified table. */
    public static void markEmailVerified(String username) {
        String sql = "INSERT OR REPLACE INTO email_verified (username, verified) VALUES (?, 1)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}