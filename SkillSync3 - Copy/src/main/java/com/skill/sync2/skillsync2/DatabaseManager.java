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

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:skillsync.db");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        dataSource = new HikariDataSource(config);
    }

    public static Connection connect() throws SQLException { return dataSource.getConnection(); }
    public static void disconnect() { if (dataSource != null && !dataSource.isClosed()) dataSource.close(); }

    // ─── Schema Initialization ────────────────────────────────────────────────

    public static void initialize() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS students (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL, password TEXT NOT NULL, skill TEXT NOT NULL, bio TEXT DEFAULT 'Hello! I am using SkillSync.', last_seen DATETIME DEFAULT CURRENT_TIMESTAMP, is_online INTEGER DEFAULT 0);");
            stmt.execute("CREATE TABLE IF NOT EXISTS projects (id INTEGER PRIMARY KEY AUTOINCREMENT, owner_name TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, required_role TEXT NOT NULL, responsibilities TEXT NOT NULL, due_date TEXT DEFAULT 'Not Specified');");
            stmt.execute("CREATE TABLE IF NOT EXISTS invitations (id INTEGER PRIMARY KEY AUTOINCREMENT, receiver_name TEXT NOT NULL, sender_info TEXT NOT NULL, message TEXT NOT NULL, status TEXT DEFAULT 'PENDING', type TEXT DEFAULT 'TEAM', related_title TEXT DEFAULT '', is_read INTEGER DEFAULT 0);");
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT NOT NULL, receiver TEXT NOT NULL, content TEXT NOT NULL, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, is_read INTEGER DEFAULT 0);");
            stmt.execute("CREATE TABLE IF NOT EXISTS portfolio (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, file_name TEXT NOT NULL, file_path TEXT NOT NULL);");
            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, team_id TEXT NOT NULL, creator TEXT NOT NULL, title TEXT NOT NULL, assigned_to TEXT DEFAULT 'Unassigned', status TEXT DEFAULT 'PENDING', due_date TEXT DEFAULT 'None');");
            try { stmt.execute("ALTER TABLE tasks ADD COLUMN due_date TEXT DEFAULT 'None';"); } catch (SQLException ignored) {}
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ─── Password Utility ─────────────────────────────────────────────────────

    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    // ─── Student / Auth ───────────────────────────────────────────────────────

    public static boolean verifyLogin(String name, String password) {
        String sql = "SELECT 1 FROM students WHERE name = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, hashPassword(password));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    public static void addStudent(String name, String password, String skill) throws SQLException {
        String sql = "INSERT INTO students(name, password, skill) VALUES(?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, hashPassword(password)); ps.setString(3, skill); ps.executeUpdate();
        }
    }

    public static Student getStudentProfile(String username) {
        String sql = "SELECT name, skill, bio FROM students WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Student(rs.getString("name"), rs.getString("skill"), rs.getString("bio"));
            }
        } catch (SQLException e) { return new Student("NETWORK_ERROR", "", ""); }
        return null;
    }

    public static void updateProfile(String username, String newSkills, String newBio) throws SQLException {
        String sql = "UPDATE students SET skill = ?, bio = ? WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newSkills); ps.setString(2, newBio); ps.setString(3, username); ps.executeUpdate();
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
            } catch (SQLException e) { conn.rollback(); throw e; }
        }
    }

    private static void exec(Connection conn, String sql, String... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            ps.executeUpdate();
        }
    }

    // ─── Search & Recommendations ─────────────────────────────────────────────

    public static List<Student> searchStudents(String nameQuery, List<String> skills) {
        List<Student> results = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT name, skill, bio FROM students WHERE 1=1 ");
        if (nameQuery != null && !nameQuery.trim().isEmpty()) sb.append("AND LOWER(name) LIKE LOWER(?) ");
        if (skills != null) { for (int i = 0; i < skills.size(); i++) sb.append("AND LOWER(skill) LIKE LOWER(?) "); }
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            int i = 1;
            if (nameQuery != null && !nameQuery.trim().isEmpty()) ps.setString(i++, "%" + nameQuery.trim() + "%");
            if (skills != null) { for (String s : skills) ps.setString(i++, "%" + s + "%"); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(new Student(rs.getString("name"), rs.getString("skill"), rs.getString("bio")));
            }
        } catch (SQLException ignored) {}
        return results;
    }

    public static List<Student> getRecommendations(String currentUser) {
        List<Student> results = new ArrayList<>();
        String sql = "SELECT name, skill, bio FROM students WHERE name != ? ORDER BY RANDOM() LIMIT 3";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(new Student(rs.getString("name"), rs.getString("skill"), rs.getString("bio")));
            }
        } catch (SQLException ignored) {}
        return results;
    }

    public static String getRandomMatch(String currentUser, String desiredSkill) {
        String sql = "SELECT name FROM students WHERE skill LIKE ? AND name != ? ORDER BY RANDOM() LIMIT 1";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + desiredSkill + "%"); ps.setString(2, currentUser);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("name"); }
        } catch (SQLException ignored) {}
        return null;
    }

    // ─── Projects ─────────────────────────────────────────────────────────────

    public static void addProject(String ownerName, String title, String description, String role, String responsibilities, String dueDate) throws SQLException {
        String sql = "INSERT INTO projects(owner_name, title, description, required_role, responsibilities, due_date) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerName); ps.setString(2, title); ps.setString(3, description);
            ps.setString(4, role); ps.setString(5, responsibilities); ps.setString(6, dueDate); ps.executeUpdate();
        }
    }

    public static List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT id, owner_name, title, description, required_role, responsibilities, due_date FROM projects ORDER BY id DESC";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) projects.add(new Project(rs.getInt("id"), rs.getString("owner_name"), rs.getString("title"), rs.getString("description"), rs.getString("required_role"), rs.getString("responsibilities"), rs.getString("due_date")));
        } catch (SQLException ignored) {}
        return projects;
    }

    public static void deleteProject(int id) throws SQLException {
        // First look up the project title so we can cascade-delete everything tied to it
        String title = null;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("SELECT title FROM projects WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) title = rs.getString("title"); }
        }
        if (title == null) return; // already gone
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
            } catch (SQLException e) { conn.rollback(); throw e; }
        }
    }

    // ─── Invitations ──────────────────────────────────────────────────────────

    public static boolean sendInvite(String receiver, String senderInfo, String message, String type, String relatedTitle) throws SQLException {
        String checkSql = "TEAM".equals(type)
                ? "SELECT COUNT(*) FROM invitations WHERE ((receiver_name = ? AND sender_info = ?) OR (receiver_name = ? AND sender_info = ?)) AND type = 'TEAM'"
                : "SELECT COUNT(*) FROM invitations WHERE receiver_name = ? AND sender_info = ? AND type = 'PROJECT' AND related_title = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(checkSql)) {
            if ("TEAM".equals(type)) { ps.setString(1, receiver); ps.setString(2, senderInfo); ps.setString(3, senderInfo); ps.setString(4, receiver); }
            else { ps.setString(1, receiver); ps.setString(2, senderInfo); ps.setString(3, relatedTitle); }
            try (ResultSet rs = ps.executeQuery()) { if (rs.next() && rs.getInt(1) > 0) return false; }
        }
        String sql = "INSERT INTO invitations(receiver_name, sender_info, message, type, related_title) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receiver); ps.setString(2, senderInfo); ps.setString(3, message);
            ps.setString(4, type); ps.setString(5, relatedTitle); ps.executeUpdate(); return true;
        }
    }

    public static List<Invitation> getInvitations(String receiverName) {
        List<Invitation> invites = new ArrayList<>();
        String sql = "SELECT id, receiver_name, sender_info, message, status, type, related_title FROM invitations WHERE receiver_name = ? AND status != 'REJECTED' ORDER BY id DESC";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receiverName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) invites.add(new Invitation(rs.getInt("id"), rs.getString("receiver_name"), rs.getString("sender_info"), rs.getString("message"), rs.getString("status"), rs.getString("type"), rs.getString("related_title")));
            }
        } catch (SQLException ignored) {}
        return invites;
    }

    public static void updateInviteStatus(int id, String status) throws SQLException {
        String sql = "UPDATE invitations SET status = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public static int getUnreadInviteCount(String username) {
        String sql = "SELECT COUNT(*) FROM invitations WHERE receiver_name = ? AND is_read = 0 AND status != 'REJECTED'";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException ignored) {}
        return 0;
    }

    public static void markInvitesAsRead(String username) {
        String sql = "UPDATE invitations SET is_read = 1 WHERE receiver_name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username); ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public static List<String> getRecentActivity(String username) {
        List<String> activity = new ArrayList<>();
        String sql = "SELECT sender_info, type FROM invitations WHERE receiver_name = ? AND sender_info != ? ORDER BY id DESC LIMIT 5";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username); ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) activity.add(rs.getString("sender_info") + ("TEAM".equals(rs.getString("type")) ? " sent a team invite." : " applied to your project."));
            }
        } catch (SQLException ignored) {}
        return activity;
    }

    // ─── Teaming Up ───────────────────────────────────────────────────────────

    public static boolean checkIsTeamedUp(String user1, String user2) {
        String sql = "SELECT COUNT(*) FROM invitations WHERE type = 'TEAM' AND status = 'ACCEPTED' AND ((receiver_name = ? AND sender_info = ?) OR (receiver_name = ? AND sender_info = ?))";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user1); ps.setString(2, user2); ps.setString(3, user2); ps.setString(4, user1);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1) > 0; }
        } catch (SQLException ignored) {}
        return false;
    }

    public static void unpairUsers(String user1, String user2) throws SQLException {
        String sql = "DELETE FROM invitations WHERE type = 'TEAM' AND status = 'ACCEPTED' AND ((receiver_name = ? AND sender_info = ?) OR (receiver_name = ? AND sender_info = ?))";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user1); ps.setString(2, user2); ps.setString(3, user2); ps.setString(4, user1); ps.executeUpdate();
        }
    }

    public static List<String> getUserTeams(String username) {
        // Use a LinkedHashSet to preserve insertion order while eliminating duplicates.
        // Duplicates arise when a user both owns a project AND has an accepted application
        // for the same project title, which caused the same team to appear twice in chat.
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        try (Connection conn = connect()) {
            try (PreparedStatement p1 = conn.prepareStatement("SELECT title FROM projects WHERE owner_name = ?")) {
                p1.setString(1, username);
                try (ResultSet rs = p1.executeQuery()) { while (rs.next()) seen.add(rs.getString("title")); }
            }
            // Only pull accepted memberships whose parent project still exists (prevents ghost chats)
            try (PreparedStatement p2 = conn.prepareStatement(
                    "SELECT i.related_title FROM invitations i " +
                            "WHERE i.sender_info = ? AND i.type = 'PROJECT' AND i.status = 'ACCEPTED' " +
                            "AND EXISTS (SELECT 1 FROM projects p WHERE p.title = i.related_title)")) {
                p2.setString(1, username);
                try (ResultSet rs = p2.executeQuery()) { while (rs.next()) seen.add(rs.getString("related_title")); }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return new ArrayList<>(seen);
    }

    // ─── Messages ─────────────────────────────────────────────────────────────

    public static void sendMessage(String sender, String receiver, String content) throws SQLException {
        String sql = "INSERT INTO messages(sender, receiver, content) VALUES(?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender); ps.setString(2, receiver); ps.setString(3, content); ps.executeUpdate();
        }
    }

    public static List<Message> getChatHistory(String u1, String u2) {
        List<Message> history = new ArrayList<>();
        String sql = u2.startsWith("#")
                ? "SELECT sender, receiver, content, timestamp FROM messages WHERE receiver = ? ORDER BY id ASC"
                : "SELECT sender, receiver, content, timestamp FROM messages WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY id ASC";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (u2.startsWith("#")) ps.setString(1, u2);
            else { ps.setString(1, u1); ps.setString(2, u2); ps.setString(3, u2); ps.setString(4, u1); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) history.add(new Message(rs.getString("sender"), rs.getString("receiver"), rs.getString("content"), rs.getString("timestamp")));
            }
        } catch (SQLException e) { return new ArrayList<>(); }
        return history;
    }

    public static List<String> getChatContacts(String username) {
        List<String> contacts = new ArrayList<>();
        String sql = "SELECT DISTINCT sender AS contact FROM messages WHERE receiver = ? UNION SELECT DISTINCT receiver AS contact FROM messages WHERE sender = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username); ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) contacts.add(rs.getString("contact")); }
        } catch (SQLException ignored) {}
        return contacts;
    }

    public static int getUnreadMessageCount(String username) {
        String sql = "SELECT COUNT(*) FROM messages WHERE receiver = ? AND is_read = 0";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException ignored) {}
        return 0;
    }

    public static void markMessagesAsRead(String sender, String receiver) {
        String sql = "UPDATE messages SET is_read = 1 WHERE sender = ? AND receiver = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender); ps.setString(2, receiver); ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // ─── Presence / Heartbeat ─────────────────────────────────────────────────

    public static void sendHeartbeat(String username) {
        String sql = "UPDATE students SET last_seen = CURRENT_TIMESTAMP WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, username); ps.executeUpdate(); } catch (SQLException ignored) {}
    }

    public static void forceOffline(String username) {
        String sql = "UPDATE students SET last_seen = datetime('now', '-1 day') WHERE name = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, username); ps.executeUpdate(); } catch (SQLException ignored) {}
    }

    public static Set<String> getOnlineUsers() {
        Set<String> online = new HashSet<>();
        String sql = "SELECT name FROM students WHERE last_seen >= datetime('now', '-30 seconds')";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) online.add(rs.getString("name"));
        } catch (SQLException ignored) {}
        return online;
    }

    public static String getLastSeenStatus(String username) {
        return "Active recently";
    }

    // ─── Portfolio ────────────────────────────────────────────────────────────

    public static void addPortfolioItem(String username, String fileName, String filePath) throws SQLException {
        String sql = "INSERT INTO portfolio(username, file_name, file_path) VALUES(?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username); ps.setString(2, fileName); ps.setString(3, filePath); ps.executeUpdate();
        }
    }

    public static List<PortfolioItem> getPortfolioItems(String username) {
        List<PortfolioItem> items = new ArrayList<>();
        String sql = "SELECT id, username, file_name, file_path FROM portfolio WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new PortfolioItem(rs.getInt("id"), rs.getString("username"), rs.getString("file_name"), rs.getString("file_path")));
            }
        } catch (SQLException ignored) {}
        return items;
    }

    public static void deletePortfolioItem(int id) throws SQLException {
        String sql = "DELETE FROM portfolio WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); ps.executeUpdate(); }
    }

    // ─── Tasks ────────────────────────────────────────────────────────────────

    public static void addTask(String teamId, String creator, String title, String dueDate) throws SQLException {
        String sql = "INSERT INTO tasks(team_id, creator, title, due_date) VALUES(?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId); ps.setString(2, creator); ps.setString(3, title); ps.setString(4, dueDate); ps.executeUpdate();
        }
    }

    public static List<Task> getTasks(String teamId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, team_id, creator, title, assigned_to, status, due_date FROM tasks WHERE team_id = ? ORDER BY id DESC";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String assignedTo = rs.getString("assigned_to"); if (assignedTo == null) assignedTo = "Unassigned";
                    String status = rs.getString("status"); if (status == null) status = "PENDING";
                    tasks.add(new Task(rs.getInt("id"), rs.getString("team_id"), rs.getString("creator"), rs.getString("title"), assignedTo, status, rs.getString("due_date")));
                }
            }
        } catch (SQLException e) { return new ArrayList<>(); }
        return tasks;
    }

    public static void updateTaskStatus(int id, String status) throws SQLException {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public static boolean claimTask(int taskId, String username) {
        String sql = "UPDATE tasks SET assigned_to = ?, status = 'IN_PROGRESS' WHERE id = ? AND (assigned_to = 'Unassigned' OR assigned_to IS NULL)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username); ps.setInt(2, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static void deleteTask(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); ps.executeUpdate(); }
    }

    public static double getProjectPulse(String teamId) {
        String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END) as done_count FROM tasks WHERE team_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total"); if (total == 0) return 0.0;
                    return ((double) rs.getInt("done_count") / total) * 100;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }
}