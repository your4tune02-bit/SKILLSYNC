package com.skill.sync2.skillsync2;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.Polygon;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import org.kordamp.ikonli.javafx.FontIcon;

public class HelloController {

    // ─── FXML Injections ─────────────────────────────────────────────────────

    @FXML private BorderPane mainDashboard;
    @FXML private StackPane toastContainer;
    @FXML private VBox splashScreen;
    @FXML private Label splashText;
    @FXML private ImageView splashLogoView;
    @FXML private FontIcon themeIcon;

    @FXML private MenuButton notificationBell;
    @FXML private javafx.scene.shape.Circle notificationBadge;
    @FXML private Label loggedInUserLabel;
    @FXML private Pane sidebarContainer;
    @FXML private Button hamburgerBtn;
    @FXML private MenuButton settingsBtn;
    @FXML private FontIcon settingsIcon;
    @FXML private CheckMenuItem themeToggleItem;

    @FXML private Button btnNavHome, btnNavProj, btnNavSearch, btnNavInbox, btnNavChat, btnNavWorkspace, btnNavProfile;
    @FXML private VBox homeView, projectView, createProjectView, searchView, inboxView, workspaceView, profileView, publicProfileView;
    @FXML private HBox chatView;

    @FXML private StackPane authOverlay;
    @FXML private VBox loginCard, signupCard, forgotPasswordCard;
    @FXML private TextField loginNameInput, signupNameInput, signupEmailInput;
    @FXML private PasswordField loginPasswordInput, signupPasswordInput;
    @FXML private FlowPane signupSkillsPane;
    @FXML private TextField forgotPasswordEmailInput, forgotPasswordOtpInput;
    @FXML private PasswordField forgotPasswordNewPassInput;
    @FXML private Label forgotPasswordSubtitle, publicProfileLastSeenLbl;
    @FXML private VBox forgotPasswordStep1, forgotPasswordStep2;
    @FXML private TextField loginPasswordVisible, signupPasswordVisible;

    @FXML private Label homeWelcomeLbl, statProjectsLbl, statInvitesLbl, statMessagesLbl;
    @FXML private HBox recommendationsBox;
    @FXML private TextField projectTitleInput;
    @FXML private DatePicker projectDueDateInput;
    @FXML private TextArea projectGoalInput, projectResponsibilitiesInput;
    @FXML private FlowPane projectRolePane;
    @FXML private ListView<Project> projectListView;

    @FXML private TextField searchNameInput;
    @FXML private FlowPane searchSkillPane;
    @FXML private ListView<Student> resultsList;
    @FXML private ListView<Invitation> teamInvitesListView, projectAppsListView;

    @FXML private Label chatHeaderLabel;
    @FXML private ListView<String> chatContactsList;
    @FXML private TextField chatMessageInput;
    @FXML private ListView<Message> chatListView;

    @FXML private ComboBox<String> workspaceTeamSelector;
    @FXML private TextField workspaceNewTaskInput;
    @FXML private DatePicker workspaceTaskDeadline;
    @FXML private ListView<Task> pendingTaskList, progressTaskList, doneTaskList;
    @FXML private ProgressBar workspacePulseBar;
    @FXML private Label pulseLabel;

    @FXML private FlowPane profileSkillsPane;
    @FXML private TextArea profileBioInput;
    @FXML private ListView<PortfolioItem> myPortfolioListView;

    @FXML private StackPane publicProfileAvatarPane;
    @FXML private Label publicProfileNameLbl, publicProfileSkillsLbl, publicProfileBioLbl;
    @FXML private HBox publicProfileActionBox;
    @FXML private ListView<PortfolioItem> publicPortfolioListView;

    // ─── State ────────────────────────────────────────────────────────────────

    private String currentUser = null;
    private String currentChatUser = "";
    private String displayedChatUser = "";
    private Node previousView = null;
    private boolean isSidebarExpanded = false;
    private boolean isDarkMode = true;
    private boolean isLoginPassVisible = false;
    private boolean isSignupPassVisible = false;

    private Timeline backgroundPoller;
    private Pane particlePane;
    private AnimationTimer particleTimer;
    private final Random random = new Random();
    private Set<String> onlineUsersCache = new java.util.HashSet<>();

    private final Preferences prefs = Preferences.userNodeForPackage(HelloController.class);
    private final String UPLOAD_DIR = System.getProperty("user.home") + File.separator + ".skillsync" + File.separator + "uploads";
    private final String[] SKILL_LIST = {"Java", "Python", "UI/UX", "SQL", "Web Dev", "C++", "C#", "Mobile Dev", "DevOps", "AI/ML"};

    // ─── Initialization ───────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        authOverlay.setVisible(false);
        new File(UPLOAD_DIR).mkdirs();

        populateSkillPanes();
        setupCustomListCells();
        setupListPlaceholders();
        setupBackgroundPoller();
        setupDatePickers();

        notificationBell.setOnShowing(e -> {
            notificationBadge.setVisible(false);
            CompletableFuture.runAsync(() -> DatabaseManager.markInvitesAsRead(currentUser));
        });
        settingsBtn.setOnShowing(e -> {
            RotateTransition rt = new RotateTransition(Duration.millis(400), settingsIcon);
            rt.setByAngle(90); rt.play();
        });

        Platform.runLater(this::runSplashSequence);
    }

    private void populateSkillPanes() {
        for (String skill : SKILL_LIST) {
            signupSkillsPane.getChildren().add(skillBtn(skill, null));
            projectRolePane.getChildren().add(skillBtn(skill, null));
            searchSkillPane.getChildren().add(skillBtn(skill, e -> executeSearch()));
            profileSkillsPane.getChildren().add(skillBtn(skill, null));
        }
    }

    private ToggleButton skillBtn(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        ToggleButton btn = new ToggleButton(text);
        btn.getStyleClass().add("skill-btn");
        if (action != null) btn.setOnAction(action);
        return btn;
    }

    private void setupListPlaceholders() {
        projectListView.setPlaceholder(placeholder("It's crickets in here... 🦗 Be a trailblazer and start a Squad!", "#94a3b8"));
        teamInvitesListView.setPlaceholder(placeholder("Inbox is lookin' dusty. 🕸️ Go scout some talent!", null));
        projectAppsListView.setPlaceholder(placeholder("No one's knocking at the door yet. 👀", null));
        chatListView.setPlaceholder(placeholder("Pretty quiet... drop a 'yo' to start the chat! 💬", null));
        pendingTaskList.setPlaceholder(placeholder("Nothing on the radar. Chill time? 🏖️", null));
        progressTaskList.setPlaceholder(placeholder("Nobody is cooking anything right now. 🍳", null));
        doneTaskList.setPlaceholder(placeholder("Zero wins on the board yet. Let's get to work! 🚀", null));
    }

    private Label placeholder(String text, String colorOverride) {
        Label lbl = new Label(text);
        String color = colorOverride != null ? colorOverride : "#A39DBE";
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-style: italic;");
        return lbl;
    }

    private void setupBackgroundPoller() {
        backgroundPoller = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            if (currentUser == null) return;
            CompletableFuture.supplyAsync(() -> DatabaseManager.getStudentProfile(currentUser))
                    .thenAccept(profile -> Platform.runLater(() -> {
                        if (profile == null) {
                            showToast("Session Terminated: Account no longer exists.", true);
                            forceLogoutCleanup();
                        } else if (!"NETWORK_ERROR".equals(profile.getName())) {
                            if (!prefs.getBoolean("ghost_hide_online", false))
                                CompletableFuture.runAsync(() -> DatabaseManager.sendHeartbeat(currentUser));
                            pollNotifications();
                            refreshBell();
                            if (chatView.isVisible()) refreshChatHistory(false);
                            if (inboxView.isVisible()) refreshInbox();
                            if (workspaceView.isVisible()) refreshWorkspaceTasks();
                            if (homeView.isVisible()) refreshRecommendations();
                            if (searchView.isVisible()) executeSearch();
                        }
                    }));
        }));
        backgroundPoller.setCycleCount(Animation.INDEFINITE);
    }

    private void setupDatePickers() {
        // ── Shared factory: disable + tint past dates ──────────────────────────
        javafx.util.Callback<DatePicker, DateCell> pastDateGuard = picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) { setDisable(true); return; }
                boolean isPast = date.isBefore(LocalDate.now());
                setDisable(isPast);
                // Inline style only for past cells; clear for future so CSS rules apply cleanly
                setStyle(isPast
                        ? "-fx-background-color: rgba(185,28,28,0.18); -fx-text-fill: #b91c1c; -fx-opacity: 0.55; -fx-cursor: default;"
                        : "");
            }
        };

        projectDueDateInput.setDayCellFactory(pastDateGuard);
        workspaceTaskDeadline.setDayCellFactory(pastDateGuard);

        // ── Block non-date characters typed directly into the DatePicker editors ──
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> dateCharFilter = change -> {
            String newText = change.getControlNewText();
            // Allow only digits and '/' or '-' separators, max 10 chars (yyyy-MM-dd)
            if (newText.matches("[\\d/\\-]{0,10}")) return change;
            return null;
        };
        projectDueDateInput.getEditor().setTextFormatter(new javafx.scene.control.TextFormatter<>(dateCharFilter));
        workspaceTaskDeadline.getEditor().setTextFormatter(new javafx.scene.control.TextFormatter<>(dateCharFilter));

        // ── Prevent typing a past date into either picker ──────────────────────
        projectDueDateInput.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                Platform.runLater(() -> {
                    projectDueDateInput.setValue(oldVal);
                    showToast("Due date cannot be in the past! Choose today or later.", true);
                });
            }
        });

        workspaceTaskDeadline.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBefore(LocalDate.now())) {
                Platform.runLater(() -> {
                    workspaceTaskDeadline.setValue(oldVal);
                    showToast("Task deadline cannot be in the past! Choose today or later.", true);
                });
            }
        });

        // Start with tomorrow as default for the project form
        projectDueDateInput.setValue(LocalDate.now().plusDays(1));
    }

    private void runSplashSequence() {
        splashLogoView.setScaleX(0.5);
        splashLogoView.setScaleY(0.5);
        splashLogoView.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.2), splashLogoView);
        fadeIn.setToValue(1.0);

        ScaleTransition bounce = new ScaleTransition(Duration.seconds(1.2), splashLogoView);
        bounce.setToX(1.0);
        bounce.setToY(1.0);
        bounce.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.4, 1.0));

        StackPane logoContainer = (StackPane) splashLogoView.getParent();
        DropShadow glow = (DropShadow) logoContainer.getEffect();

        Timeline pulseGlow = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 20),
                        new KeyValue(glow.spreadProperty(), 0.1)),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(glow.radiusProperty(), 55),
                        new KeyValue(glow.spreadProperty(), 0.4))
        );
        pulseGlow.setAutoReverse(true);
        pulseGlow.setCycleCount(Animation.INDEFINITE);

        // Play initial animations
        new ParallelTransition(fadeIn, bounce).play();
        pulseGlow.play();

        setupSidebarClipping();
        setupKeyboardShortcuts();
        startParticleEngine(prefs.get("particleEffect", "OFF"));

        // FIXED: Use normal PauseTransition + setOnFinished (cannot extend final class)
        PauseTransition pause = new PauseTransition(Duration.seconds(3.5));

        pause.setOnFinished(e -> {
            pulseGlow.stop();

            isDarkMode = prefs.getBoolean("darkMode", true);
            applyTheme();

            String savedUser = prefs.get("currentUser", null);

            if (savedUser != null) {
                CompletableFuture.supplyAsync(() -> DatabaseManager.getStudentProfile(savedUser))
                        .thenAccept(profile -> Platform.runLater(() -> {
                            if (profile == null) {
                                prefs.remove("currentUser");
                                authOverlay.setVisible(true);
                                showToast("Session expired. Please log in again.", true);
                            }
                            else if ("NETWORK_ERROR".equals(profile.getName())) {
                                authOverlay.setVisible(true);
                                showToast("Connection failed. Check your database.", true);
                            }
                            else {
                                loginSuccess(savedUser);
                            }
                            fadeSplash();
                        }));
            } else {
                authOverlay.setVisible(true);
                fadeSplash();
            }
        });

        pause.play();
    }

    private void fadeSplash() {
        FadeTransition ft = new FadeTransition(Duration.millis(800), splashScreen);
        ft.setFromValue(1.0); ft.setToValue(0.0);
        ft.setOnFinished(e -> splashScreen.setVisible(false)); ft.play();
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    @FXML void onLoginSubmit() {
        String username = loginNameInput.getText().trim();
        String pass = isLoginPassVisible ? loginPasswordVisible.getText() : loginPasswordInput.getText();

        loginNameInput.getStyleClass().remove("input-error");
        loginPasswordInput.getStyleClass().remove("input-error");

        if (username.isEmpty()) loginNameInput.getStyleClass().add("input-error");
        if (pass.isEmpty()) loginPasswordInput.getStyleClass().add("input-error");
        if (username.isEmpty() || pass.isEmpty()) { showToast("Please enter your credentials.", true); return; }
        if (!hasMeaningfulContent(username)) { loginNameInput.getStyleClass().add("input-error"); showToast("Username contains invalid characters.", true); return; }

        loginCard.setDisable(true);
        CompletableFuture.supplyAsync(() -> DatabaseManager.verifyLogin(username, pass))
                .thenAccept(valid -> Platform.runLater(() -> {
                    loginCard.setDisable(false);
                    if (valid) { loginSuccess(username); showToast("Welcome back, " + username + "! 🎉", false); }
                    else {
                        loginNameInput.getStyleClass().add("input-error");
                        loginPasswordInput.getStyleClass().add("input-error");
                        showToast("Invalid username or password.", true);
                    }
                }));
    }

    @FXML void onSignupSubmit() {
        String username = signupNameInput.getText().trim();
        String pass = isSignupPassVisible ? signupPasswordVisible.getText() : signupPasswordInput.getText();

        signupNameInput.getStyleClass().remove("input-error");
        signupPasswordInput.getStyleClass().remove("input-error");

        List<String> skills = new ArrayList<>();
        for (Node n : signupSkillsPane.getChildren())
            if (((ToggleButton) n).isSelected()) skills.add(((ToggleButton) n).getText());

        if (username.isEmpty()) { signupNameInput.getStyleClass().add("input-error"); showToast("Username is required.", true); return; }
        if (!isValidUsername(username)) { signupNameInput.getStyleClass().add("input-error"); showToast("Username must start with a letter and contain only letters, digits, _ or - (min 3 chars).", true); return; }
        if (!pass.matches("^(?=.*[0-9])(?=.*[A-Z]).{8,}$")) { signupPasswordInput.getStyleClass().add("input-error"); showToast("Password: 8+ chars, 1 uppercase, 1 number.", true); return; }
        if (skills.isEmpty()) { showToast("Select at least one skill.", true); return; }

        signupCard.setDisable(true);
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.addStudent(username, pass, String.join(", ", skills));
                Platform.runLater(() -> {
                    signupCard.setDisable(false);
                    loginNameInput.setText(username); switchToLogin();
                    showToast("Account created! Please log in. 🚀", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> { signupCard.setDisable(false); showToast("Username already taken!", true); });
            }
        });
    }

    private void loginSuccess(String username) {
        currentUser = username;
        prefs.put("currentUser", username);
        CompletableFuture.runAsync(() -> DatabaseManager.sendHeartbeat(currentUser));
        loggedInUserLabel.setText("User: " + currentUser);
        authOverlay.setVisible(false);
        mainDashboard.setVisible(true);
        homeWelcomeLbl.setText("Welcome Back! 🚀");

        loginNameInput.clear(); loginPasswordInput.clear();
        signupNameInput.clear(); signupPasswordInput.clear();

        refreshProjectList(); refreshInbox(); executeSearch();
        refreshRecommendations(); refreshBell();
        backgroundPoller.play();
        Platform.runLater(this::setupKeyboardShortcuts);
        navToHome();
    }

    @FXML void onLogoutClick() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Log Out"); confirm.setHeaderText(null);
        confirm.setContentText("Heading out? We'll keep your seat warm! 🍕");
        applyDialogTheme(confirm.getDialogPane());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            CompletableFuture.runAsync(() -> DatabaseManager.forceOffline(currentUser));
            forceLogoutCleanup();
        }
    }

    private void forceLogoutCleanup() {
        currentUser = null; currentChatUser = ""; displayedChatUser = "";
        prefs.remove("currentUser"); backgroundPoller.stop();

        resultsList.getItems().clear(); projectListView.getItems().clear();
        teamInvitesListView.getItems().clear(); projectAppsListView.getItems().clear();
        chatListView.getItems().clear(); chatContactsList.getItems().clear();
        pendingTaskList.getItems().clear(); progressTaskList.getItems().clear(); doneTaskList.getItems().clear();

        chatHeaderLabel.setText("Select a contact to start chatting");
        btnNavInbox.setText("Pings & Invites"); btnNavChat.setText("Squad Chat");
        if (isSidebarExpanded) toggleSidebar();
        mainDashboard.setVisible(false); authOverlay.setVisible(true); switchToLogin();
    }

    @FXML void onSendResetCodeSubmit() { showToast("Password recovery is disabled in Local Mode.", true); }
    @FXML void onVerifyResetCodeSubmit() { showToast("Password recovery is disabled in Local Mode.", true); }

    @FXML void switchToForgotPassword() { loginCard.setVisible(false); signupCard.setVisible(false); forgotPasswordCard.setVisible(true); }
    @FXML void switchToSignup() { loginCard.setVisible(false); forgotPasswordCard.setVisible(false); signupCard.setVisible(true); }
    @FXML void switchToLogin() {
        signupCard.setVisible(false); forgotPasswordCard.setVisible(false); loginCard.setVisible(true);
        forgotPasswordStep2.setVisible(false); forgotPasswordStep2.setManaged(false);
        forgotPasswordStep1.setVisible(true); forgotPasswordStep1.setManaged(true);
        forgotPasswordSubtitle.setText("Enter your email to receive a secure reset link.");
        forgotPasswordEmailInput.clear(); forgotPasswordOtpInput.clear(); forgotPasswordNewPassInput.clear();
    }

    @FXML void toggleLoginPassword() {
        isLoginPassVisible = !isLoginPassVisible;
        if (isLoginPassVisible) { loginPasswordVisible.setText(loginPasswordInput.getText()); loginPasswordVisible.setVisible(true); loginPasswordVisible.setManaged(true); loginPasswordInput.setVisible(false); loginPasswordInput.setManaged(false); }
        else { loginPasswordInput.setText(loginPasswordVisible.getText()); loginPasswordInput.setVisible(true); loginPasswordInput.setManaged(true); loginPasswordVisible.setVisible(false); loginPasswordVisible.setManaged(false); }
    }

    @FXML void toggleSignupPassword() {
        isSignupPassVisible = !isSignupPassVisible;
        if (isSignupPassVisible) { signupPasswordVisible.setText(signupPasswordInput.getText()); signupPasswordVisible.setVisible(true); signupPasswordVisible.setManaged(true); signupPasswordInput.setVisible(false); signupPasswordInput.setManaged(false); }
        else { signupPasswordInput.setText(signupPasswordVisible.getText()); signupPasswordInput.setVisible(true); signupPasswordInput.setManaged(true); signupPasswordVisible.setVisible(false); signupPasswordVisible.setManaged(false); }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML protected void navToHome() { switchView(homeView, btnNavHome); refreshHomeDashboardStats(); }
    @FXML protected void navToProjects() { switchView(projectView, btnNavProj); refreshProjectList(); }
    @FXML protected void navToSearch() { switchView(searchView, btnNavSearch); }
    @FXML protected void navToInbox() { switchView(inboxView, btnNavInbox); refreshInbox(); }
    @FXML protected void navToChat() { switchView(chatView, btnNavChat); refreshChatContacts(); }
    @FXML protected void navToWorkspace() {
        switchView(workspaceView, btnNavWorkspace);
        CompletableFuture.supplyAsync(() -> DatabaseManager.getUserTeams(currentUser))
                .thenAccept(teams -> Platform.runLater(() -> {
                    String current = workspaceTeamSelector.getValue();
                    workspaceTeamSelector.getItems().setAll(teams);
                    if (current != null && teams.contains(current)) workspaceTeamSelector.setValue(current);
                    else if (!teams.isEmpty()) workspaceTeamSelector.setValue(teams.get(0));
                    refreshWorkspaceTasks();
                }));
    }
    @FXML protected void navToProfile() {
        switchView(profileView, btnNavProfile); refreshMyPortfolio();
        CompletableFuture.supplyAsync(() -> DatabaseManager.getStudentProfile(currentUser))
                .thenAccept(student -> Platform.runLater(() -> {
                    if (student == null) return;
                    profileBioInput.setText(student.getBio());
                    for (Node n : profileSkillsPane.getChildren())
                        ((ToggleButton) n).setSelected(student.getSkills().contains(((ToggleButton) n).getText()));
                }));
    }

    private void switchView(Node target, Button activeBtn) {
        for (Node v : new Node[]{homeView, projectView, createProjectView, searchView, inboxView, chatView, workspaceView, profileView, publicProfileView})
            v.setVisible(false);
        for (Button b : new Button[]{btnNavHome, btnNavProj, btnNavSearch, btnNavInbox, btnNavChat, btnNavWorkspace, btnNavProfile})
            b.getStyleClass().remove("nav-btn-active");

        target.setVisible(true);
        if (activeBtn != null) activeBtn.getStyleClass().add("nav-btn-active");

        FadeTransition fade = new FadeTransition(Duration.millis(250), target); fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(250), target); slide.setFromY(15); slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void setupSidebarClipping() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarContainer.prefWidthProperty());
        clip.heightProperty().bind(sidebarContainer.heightProperty());
        sidebarContainer.setClip(clip);
        sidebarContainer.minWidthProperty().bind(sidebarContainer.prefWidthProperty());
        sidebarContainer.maxWidthProperty().bind(sidebarContainer.prefWidthProperty());
        sidebarContainer.setPrefWidth(0); isSidebarExpanded = false;
    }

    @FXML private void toggleSidebar() {
        isSidebarExpanded = !isSidebarExpanded;
        new Timeline(new KeyFrame(Duration.millis(280), new KeyValue(sidebarContainer.prefWidthProperty(), isSidebarExpanded ? 220 : 0, Interpolator.EASE_BOTH))).play();
        RotateTransition rt = new RotateTransition(Duration.millis(280), hamburgerBtn); rt.setToAngle(isSidebarExpanded ? 90 : 0); rt.play();
    }

    private void setupKeyboardShortcuts() {
        Scene scene = mainDashboard.getScene(); if (scene == null) return;
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN), this::navToHome);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN), this::navToProjects);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), this::navToSearch);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN), this::navToInbox);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), () -> { if (publicProfileView.isVisible() || createProjectView.isVisible()) onBackFromProfile(); });
    }

    // ─── Notifications & Home ─────────────────────────────────────────────────

    private void pollNotifications() {
        CompletableFuture.supplyAsync(() -> new Object[]{
                DatabaseManager.getUnreadInviteCount(currentUser),
                DatabaseManager.getUnreadMessageCount(currentUser),
                DatabaseManager.getOnlineUsers()
        }).thenAccept(r -> Platform.runLater(() -> {
            int inv = (int) r[0]; int msg = (int) r[1];
            onlineUsersCache = (Set<String>) r[2];
            btnNavInbox.setText(inv > 0 ? "Pings & Invites (" + inv + ")" : "Pings & Invites");
            btnNavChat.setText(msg > 0 ? "Squad Chat (" + msg + ")" : "Squad Chat");
            if (homeView.isVisible()) { statInvitesLbl.setText(String.valueOf(inv)); statMessagesLbl.setText(String.valueOf(msg)); }
        }));
    }

    private void refreshHomeDashboardStats() {
        CompletableFuture.supplyAsync(() -> {
            List<Project> all = DatabaseManager.getAllProjects(); if (all == null) return 0;
            return (int) all.stream().filter(p -> {
                String d = p.getDueDate(); if (d == null || "Not Specified".equals(d)) return true;
                try { return LocalDate.parse(d).isAfter(LocalDate.now().minusDays(1)); } catch (Exception e) { return true; }
            }).count();
        }).thenAccept(n -> Platform.runLater(() -> statProjectsLbl.setText(String.valueOf(n))));
        pollNotifications();
    }

    private void refreshBell() {
        CompletableFuture.supplyAsync(() -> DatabaseManager.getRecentActivity(currentUser))
                .thenAccept(activities -> Platform.runLater(() -> {
                    notificationBell.getItems().clear();
                    if (activities.isEmpty()) {
                        Label lbl = new Label("No recent activity."); lbl.setStyle("-fx-text-fill: -color-text-sub; -fx-padding: 5 10;");
                        CustomMenuItem item = new CustomMenuItem(lbl); item.setHideOnClick(true);
                        notificationBell.getItems().add(item); notificationBadge.setVisible(false);
                    } else {
                        if (!notificationBell.isShowing()) notificationBadge.setVisible(true);
                        for (String act : activities) {
                            Label lbl = new Label(act); lbl.setStyle("-fx-text-fill: -color-text-main; -fx-font-weight: bold; -fx-padding: 5 10;");
                            CustomMenuItem item = new CustomMenuItem(lbl); item.setHideOnClick(true);
                            notificationBell.getItems().add(item);
                        }
                    }
                }));
    }

    private void refreshRecommendations() {
        CompletableFuture.supplyAsync(() -> DatabaseManager.getRecommendations(currentUser))
                .thenAccept(recs -> Platform.runLater(() -> {
                    recommendationsBox.getChildren().clear();
                    for (Student s : recs) {
                        VBox card = new VBox(6); card.getStyleClass().add("card"); card.setAlignment(Pos.CENTER); card.setStyle("-fx-padding: 14 22;"); card.setPrefWidth(210); card.setMaxWidth(210);
                        Label name = new Label(s.getName()); name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-text-main;");
                        Label skill = new Label(s.getSkills().split(",")[0].trim()); skill.setStyle("-fx-text-fill: #8b5cf6; -fx-font-weight: bold; -fx-font-size: 12px;");
                        Button view = new Button("View Profile"); view.getStyleClass().add("action-btn"); view.setStyle("-fx-padding: 5 14; -fx-font-size: 11px;");
                        view.setOnAction(e -> showPublicProfile(s.getName()));
                        card.getChildren().addAll(getAvatar(s.getName(), 42, 17), name, skill, view);
                        recommendationsBox.getChildren().add(card);
                    }
                }));
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    @FXML private void executeSearch() {
        String query = searchNameInput.getText().trim();
        List<String> skills = new ArrayList<>();
        for (Node n : searchSkillPane.getChildren()) if (((ToggleButton) n).isSelected()) skills.add(((ToggleButton) n).getText());
        CompletableFuture.supplyAsync(() -> DatabaseManager.searchStudents(query, skills))
                .thenAccept(r -> Platform.runLater(() -> { resultsList.getItems().setAll(r); }));
    }

    @FXML private void onClearSearchFilters() {
        searchNameInput.clear();
        for (Node n : searchSkillPane.getChildren()) ((ToggleButton) n).setSelected(false);
        executeSearch();
    }

    @FXML private void onSurpriseMeClick() {
        String skill = "";
        for (Node n : searchSkillPane.getChildren()) { if (((ToggleButton) n).isSelected()) { skill = ((ToggleButton) n).getText(); break; } }
        if (skill.isEmpty()) { showToast("Select a skill filter first!", true); return; }
        final String targetSkill = skill;
        CompletableFuture.supplyAsync(() -> DatabaseManager.getRandomMatch(currentUser, targetSkill))
                .thenAccept(match -> Platform.runLater(() -> {
                    if (match == null) { showToast("No users found with '" + targetSkill + "' skill.", true); return; }
                    final String msg = "⚡ SkillSync Smart Match! Looking for a " + targetSkill + " expert. Let's team up!";
                    CompletableFuture.runAsync(() -> {
                        try {
                            boolean ok = DatabaseManager.sendInvite(match, currentUser, msg, "TEAM", "");
                            Platform.runLater(() -> showToast(ok ? "Match found! Invite sent to " + match + " 🎯" : "Match found, but invite already exists.", !ok));
                        } catch (Exception ignored) {}
                    });
                }));
    }

    // ─── Projects ─────────────────────────────────────────────────────────────

    private void refreshProjectList() {
        CompletableFuture.supplyAsync(DatabaseManager::getAllProjects)
                .thenAccept(p -> Platform.runLater(() -> projectListView.getItems().setAll(p)));
    }

    @FXML protected void showCreateProjectForm() { switchView(createProjectView, btnNavProj); }

    @FXML protected void onPostProjectClick() {
        String title = projectTitleInput.getText().trim();
        String goal = projectGoalInput.getText().trim();
        String resp = projectResponsibilitiesInput.getText().trim();
        LocalDate selectedDate = projectDueDateInput.getValue();
        String dueDate = selectedDate != null ? selectedDate.toString() : "Not Specified";
        List<String> roles = new ArrayList<>();
        for (Node n : projectRolePane.getChildren()) if (((ToggleButton) n).isSelected()) roles.add(((ToggleButton) n).getText());

        if (title.length() < 5) { showToast("Project title is too short (min 5 chars).", true); return; }
        if (!hasMeaningfulContent(title, 3)) { showToast("Project title must contain real words, not just symbols or spaces.", true); return; }
        if (roles.isEmpty()) { showToast("Select at least one required skill.", true); return; }
        if (roles.size() > 4) { showToast("Maximum 4 skills allowed.", true); return; }
        if (goal.length() < 15) { showToast("Goal description is too short (min 15 chars).", true); return; }
        if (!hasMeaningfulContent(goal, 8)) { showToast("Goal must contain meaningful text, not just symbols.", true); return; }
        if (resp.length() < 10) { showToast("Responsibilities too short (min 10 chars).", true); return; }
        if (!hasMeaningfulContent(resp, 5)) { showToast("Responsibilities must contain meaningful text.", true); return; }
        if (selectedDate != null && selectedDate.isBefore(LocalDate.now())) {
            showToast("Due date cannot be in the past! Please choose today or a future date.", true);
            projectDueDateInput.requestFocus();
            createProjectView.setDisable(false);   // important: re-enable form
            return;
        }

        createProjectView.setDisable(true);
        if (!confirm("Publish Project", "Ready to go live? 🚀", "Post \"" + title + "\" so others can find and apply?")) {
            createProjectView.setDisable(false);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.addProject(currentUser, title, goal, String.join(", ", roles), resp, dueDate);
                Platform.runLater(() -> {
                    createProjectView.setDisable(false);
                    projectTitleInput.clear(); projectGoalInput.clear(); projectResponsibilitiesInput.clear(); projectDueDateInput.setValue(null);
                    projectRolePane.getChildren().forEach(n -> ((ToggleButton) n).setSelected(false));
                    refreshProjectList(); showToast("Project published! 🚀", false); navToProjects();
                });
            } catch (Exception e) { Platform.runLater(() -> { createProjectView.setDisable(false); showToast("Failed to post project.", true); }); }
        });
    }

    // ─── Inbox ────────────────────────────────────────────────────────────────

    @FXML protected void refreshInbox() {
        if (currentUser == null) return;
        CompletableFuture.supplyAsync(() -> { DatabaseManager.markInvitesAsRead(currentUser); return DatabaseManager.getInvitations(currentUser); })
                .thenAccept(invites -> Platform.runLater(() -> {
                    teamInvitesListView.getItems().clear(); projectAppsListView.getItems().clear();
                    for (Invitation inv : invites)
                        (("PROJECT".equals(inv.getType())) ? projectAppsListView : teamInvitesListView).getItems().add(inv);
                }));
    }

    private void handleInviteAction(Invitation invite, String newStatus) {
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.updateInviteStatus(invite.getId(), newStatus);
                if ("ACCEPTED".equals(newStatus)) {
                    String msg = "PROJECT".equals(invite.getType())
                            ? "I accepted your application for: " + invite.getRelatedTitle() + "!"
                            : "I accepted your team invitation! Let's collaborate. 🤝";
                    DatabaseManager.sendMessage(currentUser, invite.getSenderInfo(), msg);
                }
                Platform.runLater(this::refreshInbox);
            } catch (Exception ignored) {}
        });
    }

    // ─── Chat ─────────────────────────────────────────────────────────────────

    @FXML void onSendChatMessage() {
        String selected = chatContactsList.getSelectionModel().getSelectedItem();
        if (selected != null) currentChatUser = selected;
        String text = chatMessageInput.getText().trim();
        if (currentChatUser.isEmpty() || text.isEmpty()) return;
        if (!hasMeaningfulContent(text)) { showToast("Message must contain real text.", true); return; }
        chatMessageInput.clear();
        CompletableFuture.runAsync(() -> {
            try { DatabaseManager.sendMessage(currentUser, currentChatUser, text); Platform.runLater(() -> refreshChatHistory(true)); }
            catch (Exception ignored) {}
        });
    }

    @FXML void onSendChatAttachment() {
        String selected = chatContactsList.getSelectionModel().getSelectedItem();
        if (selected != null) currentChatUser = selected;
        if (currentChatUser.isEmpty()) { showToast("Select a contact first.", true); return; }
        FileChooser fc = new FileChooser(); fc.setTitle("Select File to Send");
        File file = fc.showOpenDialog(chatMessageInput.getScene().getWindow());
        if (file == null) return;
        showToast("Uploading attachment...", false);
        CompletableFuture.runAsync(() -> {
            try {
                File dest = new File(UPLOAD_DIR, System.currentTimeMillis() + "_" + file.getName());
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                DatabaseManager.sendMessage(currentUser, currentChatUser, "[FILE]" + dest.getAbsolutePath() + "|" + file.getName());
                Platform.runLater(() -> { refreshChatHistory(true); showToast("Attachment sent! 📎", false); });
            } catch (Exception e) { Platform.runLater(() -> showToast("Failed to send attachment.", true)); }
        });
    }

    private void refreshChatContacts() {
        CompletableFuture.supplyAsync(() -> {
            List<String> contacts = DatabaseManager.getChatContacts(currentUser);
            List<String> teams = DatabaseManager.getUserTeams(currentUser);
            for (String t : teams) { String tag = "#" + t; if (!contacts.contains(tag)) contacts.add(0, tag); }
            return contacts;
        }).thenAccept(contacts -> Platform.runLater(() -> {
            String selected = chatContactsList.getSelectionModel().getSelectedItem();
            chatContactsList.getItems().setAll(contacts);
            if (selected != null && contacts.contains(selected)) chatContactsList.getSelectionModel().select(selected);
        }));
    }

    private void refreshChatHistory(boolean scrollToBottom) {
        if (currentChatUser.isEmpty()) return;
        final String target = currentChatUser;
        CompletableFuture.supplyAsync(() -> DatabaseManager.getChatHistory(currentUser, target))
                .thenAccept(history -> Platform.runLater(() -> {
                    if (!target.equals(currentChatUser) || history == null) return;
                    boolean newContact = !target.equals(displayedChatUser);
                    int uiCount = chatListView.getItems().size(); int dbCount = history.size();
                    if (newContact) {
                        chatListView.getItems().setAll(history); displayedChatUser = target;
                        if (!chatListView.getItems().isEmpty()) chatListView.scrollTo(chatListView.getItems().size() - 1);
                    } else if (dbCount > uiCount) {
                        chatListView.getItems().addAll(history.subList(uiCount, dbCount));
                        if (scrollToBottom || dbCount - uiCount < 3) chatListView.scrollTo(chatListView.getItems().size() - 1);
                    } else if (dbCount < uiCount) {
                        chatListView.getItems().setAll(history);
                    }
                }));
    }

    // ─── Public Profile ───────────────────────────────────────────────────────

    private void showPublicProfile(String username) {
        previousView = Stream.of(projectView, searchView, inboxView, chatView, workspaceView, profileView, homeView)
                .filter(Node::isVisible).findFirst().orElse(homeView);

        CompletableFuture.supplyAsync(() -> {
            Student s = DatabaseManager.getStudentProfile(username);
            boolean teamed = s != null && currentUser != null && !username.equals(currentUser) && DatabaseManager.checkIsTeamedUp(currentUser, username);
            String lastSeen = DatabaseManager.getLastSeenStatus(username);
            return new Object[]{s, teamed, lastSeen};
        }).thenAccept(res -> Platform.runLater(() -> {
            Student student = (Student) res[0]; boolean isTeamed = (boolean) res[1]; String lastSeen = (String) res[2];
            if (student == null) return;
            publicProfileAvatarPane.getChildren().setAll(getAvatar(student.getName(), 72, 30));
            publicProfileNameLbl.setText(student.getName());
            publicProfileSkillsLbl.setText("Skills: " + student.getSkills());
            publicProfileBioLbl.setText(student.getBio());
            publicProfileActionBox.getChildren().clear();

            if (!student.getName().equals(currentUser)) {
                publicProfileLastSeenLbl.setText(lastSeen); publicProfileLastSeenLbl.setVisible(true);
                Button msgBtn = new Button("Message"); msgBtn.getStyleClass().add("action-btn"); msgBtn.setStyle("-fx-background-color: #3498db; -fx-pref-height: 34px; -fx-padding: 0 16;");
                msgBtn.setOnAction(e -> { if (!chatContactsList.getItems().contains(student.getName())) chatContactsList.getItems().add(student.getName()); chatContactsList.getSelectionModel().select(student.getName()); navToChat(); });
                Button actionBtn = new Button(isTeamed ? "Unpair Partner" : "Invite to Team"); actionBtn.getStyleClass().add("action-btn");
                actionBtn.setStyle(isTeamed ? "-fx-background-color: #e74c3c; -fx-pref-height: 34px; -fx-padding: 0 16;" : "-fx-background-color: #d35400; -fx-pref-height: 34px; -fx-padding: 0 16;");
                actionBtn.setOnAction(e -> { if (isTeamed) handleUnpair(student.getName()); else handleSendTeamInvite(student.getName()); });
                publicProfileActionBox.getChildren().addAll(msgBtn, actionBtn);
            } else { publicProfileLastSeenLbl.setVisible(false); }

            CompletableFuture.supplyAsync(() -> DatabaseManager.getPortfolioItems(student.getName()))
                    .thenAccept(items -> Platform.runLater(() -> publicPortfolioListView.getItems().setAll(items)));
            switchView(publicProfileView, null);
        }));
    }

    // A helper to avoid duplicating the stream search above
    private static <T> java.util.stream.Stream<T> Stream(T... items) {
        return java.util.Arrays.stream(items);
    }

    @FXML void onBackFromProfile() {
        if (previousView == null) { navToHome(); return; }
        Button btn = null;
        if (previousView == projectView) btn = btnNavProj;
        else if (previousView == searchView) btn = btnNavSearch;
        else if (previousView == inboxView) btn = btnNavInbox;
        else if (previousView == chatView) btn = btnNavChat;
        else if (previousView == workspaceView) btn = btnNavWorkspace;
        else if (previousView == profileView) btn = btnNavProfile;
        else if (previousView == homeView) btn = btnNavHome;
        switchView(previousView, btn);
    }

    private void handleSendTeamInvite(String targetName) {
        TextInputDialog dialog = new TextInputDialog("Yo! Let's team up and build something awesome. 🚀");
        dialog.setTitle("Send Squad Invite"); dialog.setHeaderText("Inviting " + targetName); dialog.setContentText("Message:");
        applyDialogTheme(dialog.getDialogPane());
        dialog.showAndWait().ifPresent(input -> {
            String trimmed = input.trim();
            if (!trimmed.isEmpty() && !hasMeaningfulContent(trimmed, 2)) { showToast("Invite message must contain real words.", true); return; }
            String msg = trimmed.isEmpty() ? "Hey, I want to add you to my squad!" : trimmed;
            CompletableFuture.runAsync(() -> {
                try {
                    boolean ok = DatabaseManager.sendInvite(targetName, currentUser, msg, "TEAM", "");
                    Platform.runLater(() -> showToast(ok ? "Invite sent to " + targetName + "! 🎯" : "Invite already active.", !ok));
                } catch (Exception ignored) {}
            });
        });
    }

    private void handleJoinRequest(Project project) {
        TextInputDialog dialog = new TextInputDialog("Looks like a great project. I've got the skills you need! 🔥");
        dialog.setTitle("Apply to Project"); dialog.setHeaderText("Applying for: " + project.getTitle()); dialog.setContentText("Pitch yourself:");
        applyDialogTheme(dialog.getDialogPane());
        dialog.showAndWait().ifPresent(input -> {
            String trimmed = input.trim();
            if (!trimmed.isEmpty() && !hasMeaningfulContent(trimmed, 2)) { showToast("Your pitch must contain real words.", true); return; }
            String msg = trimmed.isEmpty() ? "I'd love to join this project!" : trimmed;
            CompletableFuture.runAsync(() -> {
                try {
                    boolean ok = DatabaseManager.sendInvite(project.getOwnerName(), currentUser, msg, "PROJECT", project.getTitle());
                    Platform.runLater(() -> showToast(ok ? "Application sent! 🤞" : "Already applied. 🛑", !ok));
                } catch (Exception ignored) {}
            });
        });
    }

    private void handleUnpair(String targetName) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION); confirm.setTitle("Confirm Unpair"); confirm.setHeaderText(null);
        confirm.setContentText("Unpair with " + targetName + "? You will no longer be a team.");
        applyDialogTheme(confirm.getDialogPane());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            CompletableFuture.runAsync(() -> {
                try {
                    DatabaseManager.unpairUsers(currentUser, targetName);
                    DatabaseManager.sendMessage(currentUser, targetName, "I have ended our team partnership.");
                    Platform.runLater(() -> { showToast("Successfully unpaired.", false); showPublicProfile(targetName); });
                } catch (Exception ignored) {}
            });
        }
    }

    // ─── Profile ──────────────────────────────────────────────────────────────

    @FXML void onUpdateProfileClick() {
        List<String> skills = new ArrayList<>();
        for (Node n : profileSkillsPane.getChildren()) if (((ToggleButton) n).isSelected()) skills.add(((ToggleButton) n).getText());
        if (skills.isEmpty()) { showToast("Select at least one skill.", true); return; }
        String bio = profileBioInput.getText().trim();
        if (!bio.isEmpty() && !hasMeaningfulContent(bio, 3)) { showToast("Bio must contain real words, not just symbols.", true); return; }
        if (!confirm("Update Profile", null, "Save these changes to your profile?")) return;
        CompletableFuture.runAsync(() -> {
            try { DatabaseManager.updateProfile(currentUser, String.join(", ", skills), bio); Platform.runLater(() -> showToast("Profile updated! ✅", false)); }
            catch (Exception ignored) {}
        });
    }

    @FXML void onUploadPortfolioClick() {
        FileChooser fc = new FileChooser(); fc.setTitle("Select Portfolio File");
        Window win = mainDashboard.getScene().getWindow(); File file = fc.showOpenDialog(win);
        if (file == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                File dest = new File(UPLOAD_DIR, System.currentTimeMillis() + "_" + file.getName());
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                DatabaseManager.addPortfolioItem(currentUser, file.getName(), dest.getAbsolutePath());
                Platform.runLater(() -> { refreshMyPortfolio(); showToast("File uploaded! 📁", false); });
            } catch (Exception e) { Platform.runLater(() -> showToast("Upload failed.", true)); }
        });
    }

    private void refreshMyPortfolio() {
        CompletableFuture.supplyAsync(() -> DatabaseManager.getPortfolioItems(currentUser))
                .thenAccept(items -> Platform.runLater(() -> myPortfolioListView.getItems().setAll(items)));
    }

    @FXML void onDeleteAccountClick() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION); confirm.setTitle("Delete Account");
        confirm.setHeaderText("⚠️ This is permanent."); confirm.setContentText("This will wipe your profile, portfolio, messages, and all data. Continue?");
        applyDialogTheme(confirm.getDialogPane());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            CompletableFuture.runAsync(() -> {
                try { DatabaseManager.deleteAccount(currentUser); Platform.runLater(() -> { showToast("Account deleted.", false); forceLogoutCleanup(); }); }
                catch (Exception e) { Platform.runLater(() -> showToast("Failed to delete account.", true)); }
            });
        }
    }

    // ─── Workspace ────────────────────────────────────────────────────────────

    @FXML void onWorkspaceTeamChanged() { refreshWorkspaceTasks(); }

    @FXML void onAddTask() {
        String teamId = workspaceTeamSelector.getValue();
        String title = workspaceNewTaskInput.getText().trim();
        if (teamId == null || teamId.isEmpty()) { showToast("Select a team first!", true); return; }
        if (title.isEmpty()) { showToast("Enter a task title.", true); return; }
        if (title.length() < 3) { showToast("Task title is too short (min 3 chars).", true); return; }
        if (!hasMeaningfulContent(title, 2)) { showToast("Task title must contain real words.", true); return; }
        String due = workspaceTaskDeadline.getValue() != null ? workspaceTaskDeadline.getValue().toString() : "None";
        workspaceNewTaskInput.clear(); workspaceTaskDeadline.setValue(null);
        CompletableFuture.runAsync(() -> {
            try { DatabaseManager.addTask(teamId, currentUser, title, due); Platform.runLater(this::refreshWorkspaceTasks); }
            catch (Exception e) { Platform.runLater(() -> showToast("Failed to add task.", true)); }
        });
    }

    private void refreshWorkspaceTasks() {
        String teamId = workspaceTeamSelector.getValue();
        if (teamId == null || teamId.isEmpty()) { pendingTaskList.getItems().clear(); progressTaskList.getItems().clear(); doneTaskList.getItems().clear(); return; }
        CompletableFuture.supplyAsync(() -> {
            List<Task> tasks = DatabaseManager.getTasks(teamId);
            double pulse = (tasks != null && !tasks.isEmpty()) ? DatabaseManager.getProjectPulse(teamId) : 0.0;
            return new Object[]{pulse, tasks};
        }).thenAccept(r -> Platform.runLater(() -> {
            double pulse = (double) r[0]; List<Task> tasks = (List<Task>) r[1]; if (tasks == null) return;
            if (workspacePulseBar != null) workspacePulseBar.setProgress(pulse / 100.0);
            if (pulseLabel != null) pulseLabel.setText(String.format("%.0f%% Team Momentum", pulse));
            pendingTaskList.getItems().clear(); progressTaskList.getItems().clear(); doneTaskList.getItems().clear();
            for (Task t : tasks) {
                String s = t.getStatus() == null ? "PENDING" : t.getStatus();
                if ("IN_PROGRESS".equalsIgnoreCase(s)) progressTaskList.getItems().add(t);
                else if ("DONE".equalsIgnoreCase(s)) doneTaskList.getItems().add(t);
                else pendingTaskList.getItems().add(t);
            }
        }));
    }

    private void changeTaskStatus(int taskId, String newStatus) {
        CompletableFuture.runAsync(() -> {
            try { DatabaseManager.updateTaskStatus(taskId, newStatus); Platform.runLater(this::refreshWorkspaceTasks); }
            catch (Exception e) { Platform.runLater(() -> showToast("Failed to update task.", true)); }
        });
    }

    private void handleClaimTask(Task task) {
        CompletableFuture.runAsync(() -> {
            if (DatabaseManager.claimTask(task.getId(), currentUser))
                Platform.runLater(() -> { showToast("Dibs called! Time to cook. 👨‍🍳", false); refreshWorkspaceTasks(); });
            else
                Platform.runLater(() -> showToast("Task already claimed.", true));
        });
    }

    @FXML void onExportTasks() {
        String teamId = workspaceTeamSelector.getValue();
        if (teamId == null || teamId.isEmpty()) { showToast("Select a team first.", true); return; }
        CompletableFuture.supplyAsync(() -> DatabaseManager.getTasks(teamId))
                .thenAccept(tasks -> Platform.runLater(() -> {
                    try {
                        File out = new File(System.getProperty("user.home") + File.separator + "Desktop", teamId + "_Tasks.txt");
                        java.io.PrintWriter w = new java.io.PrintWriter(out);
                        w.println("=== SKILLSYNC SQUAD BOARD EXPORT ===");
                        w.println("Team: " + teamId); w.println("Date: " + LocalDate.now()); w.println("---");
                        for (Task t : tasks) w.println(String.format("[%s] %s | @%s | Due: %s", t.getStatus(), t.getTitle(), t.getAssignedTo(), t.getDueDate()));
                        w.close(); showToast("Exported to Desktop! 📄", false);
                    } catch (Exception e) { showToast("Export failed.", true); }
                }));
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    @FXML private void toggleTheme() { isDarkMode = themeToggleItem.isSelected(); applyTheme(); prefs.putBoolean("darkMode", isDarkMode); }

    private void applyTheme() {
        Scene scene = mainDashboard.getScene(); if (scene == null) return;
        if (isDarkMode) {
            scene.getRoot().getStyleClass().remove("light-theme"); themeToggleItem.setSelected(true);
            settingsIcon.setIconColor(javafx.scene.paint.Color.web("#94a3b8"));
        } else {
            if (!scene.getRoot().getStyleClass().contains("light-theme")) scene.getRoot().getStyleClass().add("light-theme");
            themeToggleItem.setSelected(false); settingsIcon.setIconColor(javafx.scene.paint.Color.web("#6C6687"));
        }
    }

    // ─── Toast Notifications ─────────────────────────────────────────────────

    private void showToast(String message, boolean isError) {
        Platform.runLater(() -> {
            if (!isError && prefs.getBoolean("ping_mute_toasts", false)) return;
            Label toast = new Label(message);
            toast.setStyle("-fx-background-color: " + (isError ? "#c0392b" : "#0f9b6a") + "; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 22; -fx-font-weight: bold; -fx-font-size: 13px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            toast.setOpacity(0);
            FontIcon icon = new FontIcon(isError ? "fas-exclamation-circle" : "fas-check-circle");
            icon.setIconColor(javafx.scene.paint.Color.WHITE); icon.setIconSize(14); toast.setGraphic(icon);
            toastContainer.getChildren().add(toast);

            TranslateTransition ttIn = new TranslateTransition(Duration.millis(280), toast); ttIn.setFromY(20); ttIn.setToY(0);
            FadeTransition ftIn = new FadeTransition(Duration.millis(280), toast); ftIn.setToValue(1.0);
            TranslateTransition ttOut = new TranslateTransition(Duration.millis(280), toast); ttOut.setToY(15);
            FadeTransition ftOut = new FadeTransition(Duration.millis(280), toast); ftOut.setToValue(0);
            ftOut.setOnFinished(e -> toastContainer.getChildren().remove(toast));

            new SequentialTransition(new ParallelTransition(ttIn, ftIn), new PauseTransition(Duration.seconds(2.8)), new ParallelTransition(ttOut, ftOut)).play();
        });
    }

    // ─── Settings Dialogs ─────────────────────────────────────────────────────

    @FXML void showGhostModeSettings() {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Ghost Mode 👻"); dialog.setHeaderText("Stealth settings for your account.");
        applyDialogTheme(dialog.getDialogPane()); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        CheckBox hideOnline = styledCheckBox("Ninja Mode (Appear Offline)", prefs.getBoolean("ghost_hide_online", false));
        CheckBox hideLastSeen = styledCheckBox("Hide 'Last Seen' Timestamp", prefs.getBoolean("ghost_hide_seen", false));
        VBox content = new VBox(14, hideOnline, hideLastSeen); dialog.getDialogPane().setContent(content);
        dialog.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                prefs.putBoolean("ghost_hide_online", hideOnline.isSelected());
                prefs.putBoolean("ghost_hide_seen", hideLastSeen.isSelected());
                showToast("Ghost Mode updated! 🥷", false);
                if (hideOnline.isSelected()) CompletableFuture.runAsync(() -> DatabaseManager.forceOffline(currentUser));
                else CompletableFuture.runAsync(() -> DatabaseManager.sendHeartbeat(currentUser));
            }
        });
    }

    @FXML void showPingPreferences() {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Ping Preferences 🔔"); dialog.setHeaderText("Control your notification noise.");
        applyDialogTheme(dialog.getDialogPane()); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        CheckBox muteToasts = styledCheckBox("Mute all success toasts", prefs.getBoolean("ping_mute_toasts", false));
        CheckBox muteChat = styledCheckBox("Mute incoming message alerts", prefs.getBoolean("ping_mute_chat", false));
        VBox content = new VBox(14, muteToasts, muteChat); dialog.getDialogPane().setContent(content);
        dialog.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                prefs.putBoolean("ping_mute_toasts", muteToasts.isSelected());
                prefs.putBoolean("ping_mute_chat", muteChat.isSelected());
                showToast("Ping preferences saved! 🤫", false);
            }
        });
    }

    private CheckBox styledCheckBox(String text, boolean selected) {
        CheckBox cb = new CheckBox(text); cb.setSelected(selected);
        cb.setStyle("-fx-text-fill: -color-text-main; -fx-font-weight: bold;"); return cb;
    }

    private void applyDialogTheme(DialogPane pane) {
        if (mainDashboard.getScene() != null) pane.getStylesheets().addAll(mainDashboard.getScene().getStylesheets());
        if (!isDarkMode) { if (!pane.getStyleClass().contains("light-theme")) pane.getStyleClass().add("light-theme"); }
    }

    /**
     * Shows a themed confirmation dialog and returns true only if the user clicks OK.
     * Use this everywhere a destructive or irreversible action is about to happen.
     */
    private boolean confirm(String title, String header, String body) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(body);
        applyDialogTheme(alert.getDialogPane());
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    // ─── Particle Effects ─────────────────────────────────────────────────────

    @FXML void setEffectSakura() { prefs.put("particleEffect", "SAKURA"); startParticleEngine("SAKURA"); }
    @FXML void setEffectSnow()   { prefs.put("particleEffect", "SNOW");   startParticleEngine("SNOW"); }
    @FXML void setEffectLeaves() { prefs.put("particleEffect", "LEAVES"); startParticleEngine("LEAVES"); }
    @FXML void setEffectOff()    { prefs.put("particleEffect", "OFF");    startParticleEngine("OFF"); }

    private void startParticleEngine(String type) {
        Platform.runLater(() -> {
            StackPane root = (StackPane) splashScreen.getParent();
            if (particlePane == null) {
                particlePane = new Pane(); particlePane.setMouseTransparent(true); particlePane.setManaged(false);
                particlePane.prefWidthProperty().bind(root.widthProperty()); particlePane.prefHeightProperty().bind(root.heightProperty());
                root.getChildren().add(0, particlePane);
            }
            if (particleTimer != null) particleTimer.stop(); particlePane.getChildren().clear();
            if ("OFF".equals(type)) return;
            int count = "SNOW".equals(type) ? 60 : 40;
            for (int i = 0; i < count; i++) { Node p = createParticle(type); particlePane.getChildren().add(p); resetParticle(p, 1920, 1080, true, type); }
            particleTimer = new AnimationTimer() {
                @Override public void handle(long now) {
                    double w = particlePane.getWidth() == 0 ? 1200 : particlePane.getWidth();
                    double h = particlePane.getHeight() == 0 ? 800 : particlePane.getHeight();
                    for (Node p : particlePane.getChildren()) {
                        double dx = (double) p.getProperties().get("dx"); double dy = (double) p.getProperties().get("dy"); double dRot = (double) p.getProperties().get("dRot");
                        p.setTranslateX(p.getTranslateX() + dx); p.setTranslateY(p.getTranslateY() + dy); p.setRotate(p.getRotate() + dRot);
                        if ("SNOW".equals(type)) { dx += (random.nextDouble() - 0.5) * 0.02; dx = Math.max(-1.0, Math.min(1.0, dx)); }
                        else { dx += (random.nextDouble() - 0.5) * 0.05; dx = Math.max(-2.0, Math.min(2.0, dx)); }
                        p.getProperties().put("dx", dx);
                        if (p.getTranslateY() > h + 20) resetParticle(p, w, h, false, type);
                    }
                }
            };
            particleTimer.start();
        });
    }

    private Node createParticle(String type) {
        if ("SNOW".equals(type)) {
            javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(random.nextDouble() * 3 + 2);
            c.setFill(javafx.scene.paint.Color.web("#00E5FF", 0.8)); c.setEffect(new DropShadow(5, javafx.scene.paint.Color.web("#00E5FF"))); return c;
        } else if ("LEAVES".equals(type)) {
            Polygon leaf = new Polygon(0, 0, 8, 4, 12, 12, 4, 8);
            String[] colors = {"#e67e22", "#d35400", "#f1c40f", "#e74c3c"}; leaf.setFill(javafx.scene.paint.Color.web(colors[random.nextInt(colors.length)], 0.7)); return leaf;
        } else {
            Polygon petal = new Polygon(0, 0, 6, 10, 0, 20, -6, 10);
            petal.setFill(javafx.scene.paint.Color.web("#ffb7c5", 0.6)); petal.setEffect(new DropShadow(5, javafx.scene.paint.Color.web("#ffb7c5", 0.4))); return petal;
        }
    }

    private void resetParticle(Node p, double w, double h, boolean randomY, String type) {
        p.setTranslateX(random.nextDouble() * w); p.setTranslateY(randomY ? random.nextDouble() * h : -20); p.setRotate(random.nextDouble() * 360);
        double spd = "SNOW".equals(type) ? 0.5 : 1.0;
        p.getProperties().put("dx", (random.nextDouble() * 2 - 1) * spd);
        p.getProperties().put("dy", (random.nextDouble() * 1.5 + 1) * spd);
        p.getProperties().put("dRot", random.nextDouble() * 3 - 1.5);
        double scale = random.nextDouble() * 0.6 + 0.4; p.setScaleX(scale); p.setScaleY(scale);
    }

    // ─── Avatar Helper ────────────────────────────────────────────────────────

    private StackPane getAvatar(String name, double size, double fontSize) {
        String initial = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
        String[] gradients = {
                "linear-gradient(to bottom right, #ff9a9e, #fecfef)",
                "linear-gradient(to bottom right, #a18cd1, #fbc2eb)",
                "linear-gradient(to bottom right, #84fab0, #8fd3f4)",
                "linear-gradient(to bottom right, #fccb90, #d57eeb)",
                "linear-gradient(to bottom right, #e0c3fc, #8ec5fc)",
                "linear-gradient(to bottom right, #4facfe, #00f2fe)"
        };
        String bg = gradients[Math.abs(name == null ? 0 : name.hashCode()) % gradients.length];
        Label lbl = new Label(initial); lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + fontSize + "px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 2, 0, 1, 1);");
        StackPane base = new StackPane(lbl); base.setMinSize(size, size); base.setMaxSize(size, size);
        base.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: " + (size / 2) + ";");
        boolean online = name != null && (name.equals(currentUser) || onlineUsersCache.contains(name));
        if (online && name != null && !name.startsWith("#")) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(size * 0.16);
            dot.setStyle("-fx-fill: #10b981; -fx-effect: dropshadow(gaussian, rgba(16,185,129,0.6), 5, 0.5, 0, 0);");
            dot.setStroke(javafx.scene.paint.Color.web("#1e293b")); dot.setStrokeWidth(2.5);
            dot.setTranslateX(size * 0.08); dot.setTranslateY(size * 0.08);
            StackPane container = new StackPane(base, dot); container.setAlignment(Pos.BOTTOM_RIGHT); container.setMaxSize(size, size);
            return container;
        }
        return base;
    }

    // ─── Custom List Cells ────────────────────────────────────────────────────

    private void setupCustomListCells() {
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty); if (empty || s == null) { setText(null); setGraphic(null); setContextMenu(null); return; }
                HBox root = new HBox(14); root.getStyleClass().add("card"); root.setAlignment(Pos.CENTER_LEFT); root.prefWidthProperty().bind(lv.widthProperty().subtract(40));
                VBox info = new VBox(4);
                Label name = new Label(s.getName()); name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -color-text-main;");
                Label skills = new Label("Skills: " + s.getSkills()); skills.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-sub;");
                Label bio = new Label(s.getBio()); bio.setStyle("-fx-font-style: italic; -fx-font-size: 12px; -fx-text-fill: -color-text-sub;"); bio.setWrapText(true);
                info.getChildren().addAll(name, skills, bio); Pane spacer = new Pane(); HBox.setHgrow(spacer, Priority.ALWAYS);
                if (!s.getName().equals(currentUser)) {
                    Button view = actionBtn("View Profile", "transparent", e -> showPublicProfile(s.getName()));
                    Button msg  = actionBtn("Message", "#3498db", e -> { if (!chatContactsList.getItems().contains(s.getName())) chatContactsList.getItems().add(s.getName()); chatContactsList.getSelectionModel().select(s.getName()); navToChat(); });
                    Button inv  = actionBtn("Invite", "#d35400", e -> handleSendTeamInvite(s.getName()));
                    view.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border; -fx-text-fill: -color-text-main; -fx-pref-height: 28; -fx-font-size: 11px;");
                    msg.setStyle("-fx-background-color: #3498db; -fx-pref-height: 28; -fx-font-size: 11px;");
                    inv.setStyle("-fx-background-color: #d35400; -fx-pref-height: 28; -fx-font-size: 11px;");
                    root.getChildren().addAll(getAvatar(s.getName(), 46, 18), info, spacer, new HBox(8, view, msg, inv));
                    ContextMenu cm = new ContextMenu(); MenuItem vi = new MenuItem("View Profile"); vi.setOnAction(e -> showPublicProfile(s.getName())); cm.getItems().add(vi); setContextMenu(cm);
                } else { root.getChildren().addAll(getAvatar(s.getName(), 46, 18), info, spacer, new Label("(You)")); }
                setGraphic(root);
            }
        });

        projectListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Project p, boolean empty) {
                super.updateItem(p, empty); if (empty || p == null) { setText(null); setGraphic(null); setContextMenu(null); return; }
                VBox root = new VBox(10); root.getStyleClass().add("card"); root.prefWidthProperty().bind(lv.widthProperty().subtract(40));
                HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
                Label titleLbl = new Label(p.getTitle()); titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: -color-text-main;");
                Pane spacer = new Pane(); HBox.setHgrow(spacer, Priority.ALWAYS);
                header.getChildren().addAll(getAvatar(p.getOwnerName(), 36, 14), spacer);
                ContextMenu cm = new ContextMenu();
                if (!p.getOwnerName().equals(currentUser)) {
                    Button viewOwner = actionBtn("View Owner", "transparent", e -> showPublicProfile(p.getOwnerName()));
                    viewOwner.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border; -fx-text-fill: -color-text-main; -fx-pref-height: 28; -fx-font-size: 11px;");
                    Button apply = actionBtn("Apply", "#2980b9", e -> handleJoinRequest(p));
                    apply.setStyle("-fx-background-color: #2980b9; -fx-pref-height: 28; -fx-font-size: 11px;");
                    header.getChildren().addAll(viewOwner, apply);
                    MenuItem vi = new MenuItem("View " + p.getOwnerName() + "'s Profile"); vi.setOnAction(e -> showPublicProfile(p.getOwnerName()));
                    MenuItem ai = new MenuItem("Apply to Project"); ai.setOnAction(e -> handleJoinRequest(p)); cm.getItems().addAll(vi, ai);
                } else {
                    Label yours = new Label("Your Project"); yours.setStyle("-fx-text-fill: -color-text-sub; -fx-font-style: italic; -fx-font-size: 12px;");
                    Button del = actionBtn("Delete", "#e74c3c", e -> {
                        if (confirm("Delete Project", "⚠️ This is permanent.", "Delete \"" + p.getTitle() + "\"? This will also remove its group chat, tasks, and applications."))
                            CompletableFuture.runAsync(() -> { try { DatabaseManager.deleteProject(p.getId()); Platform.runLater(() -> { refreshProjectList(); showToast("Project removed.", false); }); } catch (Exception ex) {} });
                    });
                    del.setStyle("-fx-background-color: #e74c3c; -fx-pref-height: 28; -fx-font-size: 11px;");
                    header.getChildren().addAll(yours, del);
                }
                Label goal = new Label("🎯 " + p.getDescription()); goal.setStyle("-fx-text-fill: -color-text-main; -fx-font-size: 13px;"); goal.setWrapText(true);
                Label role = new Label("🔍 Looking for: " + p.getRequiredRole()); role.setStyle("-fx-text-fill: -color-text-main; -fx-font-size: 13px;");
                Label resp = new Label("📝 " + p.getResponsibilities()); resp.setStyle("-fx-text-fill: -color-text-main; -fx-font-size: 13px;"); resp.setWrapText(true);
                Label due = buildDueDateLabel(p.getDueDate());
                root.getChildren().addAll(header, titleLbl, goal, role, resp, due); setGraphic(root); setContextMenu(cm.getItems().isEmpty() ? null : cm);
            }
        });

        teamInvitesListView.setCellFactory(lv -> createInviteCell(lv));
        projectAppsListView.setCellFactory(lv -> createInviteCell(lv));
        pendingTaskList.setCellFactory(lv -> createTaskCell(lv));
        progressTaskList.setCellFactory(lv -> createTaskCell(lv));
        doneTaskList.setCellFactory(lv -> createTaskCell(lv));
        myPortfolioListView.setCellFactory(lv -> createPortfolioCell(true, lv));
        publicPortfolioListView.setCellFactory(lv -> createPortfolioCell(false, lv));

        chatContactsList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String contact, boolean empty) {
                super.updateItem(contact, empty); if (empty || contact == null) { setText(null); setGraphic(null); setContextMenu(null); return; }
                HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); row.setStyle("-fx-padding: 7 10;"); row.getStyleClass().add("card");
                row.prefWidthProperty().bind(lv.widthProperty().subtract(30));
                Label name = new Label(contact); name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -color-text-main;");
                row.getChildren().addAll(getAvatar(contact, 32, 14), name);
                row.setOnMouseClicked(e -> {
                    currentChatUser = contact;
                    chatHeaderLabel.setText(contact.startsWith("#") ? "Team Chat: " + contact : "Chat with " + contact);
                    CompletableFuture.runAsync(() -> DatabaseManager.markMessagesAsRead(contact, currentUser));
                    refreshChatHistory(true);
                });
                if (!contact.startsWith("#")) {
                    ContextMenu cm = new ContextMenu(); MenuItem vi = new MenuItem("View Profile"); vi.setOnAction(e -> showPublicProfile(contact)); cm.getItems().add(vi); setContextMenu(cm);
                }
                setGraphic(row);
            }
        });

        chatListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty); if (empty || msg == null) { setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;"); return; }
                boolean mine = msg.getSender().equals(currentUser);
                HBox row = new HBox(10); row.setStyle("-fx-padding: 4; -fx-background-color: transparent;");
                row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                row.prefWidthProperty().bind(lv.widthProperty().subtract(40));

                VBox bubble = new VBox(4);
                Label time = new Label(msg.getTimestamp()); time.setStyle("-fx-font-size: 9px; -fx-text-fill: " + (mine ? "#ecf0f1" : "-color-text-sub") + ";");

                if (msg.getContent().startsWith("[FILE]")) {
                    String[] parts = msg.getContent().substring(6).split("\\|");
                    String path = parts[0]; String fname = parts.length > 1 ? parts[1] : "Attachment";
                    Button openBtn = new Button("📎 " + fname); openBtn.getStyleClass().add("action-btn");
                    openBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10;"); openBtn.setOnAction(e -> openLocalFile(path));
                    bubble.getChildren().addAll(openBtn, time);
                } else {
                    Label content = new Label(msg.getContent()); content.setWrapText(true);
                    content.maxWidthProperty().bind(lv.widthProperty().multiply(0.65)); content.setMinHeight(Region.USE_PREF_SIZE);
                    content.setStyle("-fx-text-fill: " + (mine ? "white" : "-color-text-main") + "; -fx-font-size: 13px;");
                    bubble.getChildren().addAll(content, time);
                }
                bubble.setStyle(mine
                        ? "-fx-background-color: #3498db; -fx-background-radius: 14 14 0 14; -fx-padding: 8 12;"
                        : "-fx-background-color: -color-bg-base; -fx-border-color: -color-border; -fx-border-radius: 14 14 14 0; -fx-background-radius: 14 14 14 0; -fx-padding: 8 12;");

                if (mine) row.getChildren().addAll(bubble, getAvatar(msg.getSender(), 28, 12));
                else row.getChildren().addAll(getAvatar(msg.getSender(), 28, 12), bubble);
                setGraphic(row);
            }
        });
    }

    // ─── Input Validation Helpers ─────────────────────────────────────────────

    /**
     * Returns true when the string contains at least {@code minAlpha} letters
     * or digits, preventing pure-symbol / pure-whitespace / keyboard-mash input.
     */
    private boolean hasMeaningfulContent(String text, int minAlpha) {
        if (text == null) return false;
        long count = text.chars().filter(Character::isLetterOrDigit).count();
        return count >= minAlpha;
    }

    /**
     * Convenience: at least 1 alphanumeric character (for short fields like task
     * titles and usernames).
     */
    private boolean hasMeaningfulContent(String text) {
        return hasMeaningfulContent(text, 1);
    }

    /**
     * A username must start with a letter and contain only letters, digits,
     * underscores, and hyphens (no spaces, no special chars).
     */
    private boolean isValidUsername(String name) {
        return name != null && name.matches("^[A-Za-z][A-Za-z0-9_\\-]{2,}$");
    }

    private Button actionBtn(String text, String bg, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text); btn.getStyleClass().add("action-btn");
        if (!bg.equals("transparent")) btn.setStyle("-fx-background-color: " + bg + "; -fx-pref-height: 28; -fx-font-size: 11px;");
        btn.setOnAction(handler); return btn;
    }

    private Label buildDueDateLabel(String dateStr) {
        Label lbl = new Label();
        if (dateStr != null && !"Not Specified".equals(dateStr)) {
            try {
                LocalDate d = LocalDate.parse(dateStr);
                if (d.isBefore(LocalDate.now())) { lbl.setText("⚠️ EXPIRED (" + dateStr + ")"); lbl.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px;"); }
                else { lbl.setText("📅 Due: " + dateStr); lbl.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px;"); }
            } catch (Exception e) { lbl.setText("📅 " + dateStr); }
        } else { lbl.setText("📅 No Deadline"); lbl.setStyle("-fx-text-fill: -color-text-sub; -fx-font-size: 11px;"); }
        return lbl;
    }

    private ListCell<Invitation> createInviteCell(ListView<Invitation> lv) {
        return new ListCell<>() {
            @Override protected void updateItem(Invitation inv, boolean empty) {
                super.updateItem(inv, empty); if (empty || inv == null) { setText(null); setGraphic(null); setContextMenu(null); return; }
                VBox root = new VBox(9); root.getStyleClass().add("card"); root.prefWidthProperty().bind(lv.widthProperty().subtract(40));
                HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
                VBox senderBox = new VBox(2); Label senderLbl = new Label(inv.getSenderInfo()); senderLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -color-text-main;"); senderBox.getChildren().add(senderLbl);
                if ("PROJECT".equals(inv.getType())) { Label proj = new Label("For: " + inv.getRelatedTitle()); proj.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2980b9;"); senderBox.getChildren().add(proj); }
                Pane spacer = new Pane(); HBox.setHgrow(spacer, Priority.ALWAYS);
                Button viewBtn = actionBtn("View Profile", "transparent", e -> showPublicProfile(inv.getSenderInfo()));
                viewBtn.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border; -fx-text-fill: -color-text-main; -fx-pref-height: 28; -fx-font-size: 11px;");
                header.getChildren().addAll(getAvatar(inv.getSenderInfo(), 36, 14), senderBox, spacer, viewBtn);
                Label msgLbl = new Label(inv.getMessage()); msgLbl.setWrapText(true); msgLbl.setMinHeight(Region.USE_PREF_SIZE); msgLbl.setStyle("-fx-text-fill: -color-text-main; -fx-font-size: 13px;");
                HBox actions = new HBox(10);
                if ("PENDING".equals(inv.getStatus())) {
                    Button accept = actionBtn("Accept", "#27ae60", e -> {
                        String who = inv.getSenderInfo();
                        String what = "PROJECT".equals(inv.getType()) ? "Accept " + who + "'s application for \"" + inv.getRelatedTitle() + "\"?" : "Accept team invite from " + who + "?";
                        if (confirm("Accept Invite", null, what)) handleInviteAction(inv, "ACCEPTED");
                    });
                    Button decline = actionBtn("Decline", "#e74c3c", e -> {
                        if (confirm("Decline Invite", null, "Decline this invite from " + inv.getSenderInfo() + "?")) handleInviteAction(inv, "DECLINED");
                    });
                    accept.setStyle("-fx-background-color: #27ae60; -fx-pref-height: 28; -fx-font-size: 11px;");
                    decline.setStyle("-fx-background-color: #e74c3c; -fx-pref-height: 28; -fx-font-size: 11px;");
                    actions.getChildren().addAll(accept, decline);
                } else {
                    Label status = new Label("Status: " + inv.getStatus()); status.setStyle("-fx-font-style: italic; -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + ("ACCEPTED".equals(inv.getStatus()) ? "#27ae60" : "#e74c3c") + ";");
                    actions.getChildren().add(status);
                }
                ContextMenu cm = new ContextMenu(); MenuItem vi = new MenuItem("View " + inv.getSenderInfo() + "'s Profile"); vi.setOnAction(e -> showPublicProfile(inv.getSenderInfo())); cm.getItems().add(vi);
                root.getChildren().addAll(header, msgLbl, actions); setGraphic(root); setContextMenu(cm);
            }
        };
    }

    private ListCell<Task> createTaskCell(ListView<Task> lv) {
        return new ListCell<>() {
            @Override protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty); if (empty || task == null) { setText(null); setGraphic(null); setContextMenu(null); return; }
                VBox card = new VBox(10); card.getStyleClass().add("card"); card.prefWidthProperty().bind(lv.widthProperty().subtract(35));
                Label titleLbl = new Label(task.getTitle()); titleLbl.setWrapText(true); titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-main;");
                HBox footer = new HBox(6); footer.setAlignment(Pos.CENTER_LEFT);
                String status = task.getStatus() == null ? "PENDING" : task.getStatus();
                if ("PENDING".equalsIgnoreCase(status)) {
                    Button dibs = new Button("Claim ✋"); dibs.getStyleClass().add("dibs-btn");
                    dibs.setOnAction(e -> {
                        if (HelloController.this.confirm("Claim Task", null, "Claim \"" + task.getTitle() + "\"? It'll be assigned to you."))
                            handleClaimTask(task);
                    });
                    footer.getChildren().add(dibs);
                } else {
                    Label assigned = new Label("@" + task.getAssignedTo()); assigned.setStyle("-fx-text-fill: #00E5FF; -fx-font-weight: bold; -fx-font-size: 11px;"); footer.getChildren().add(assigned);
                }
                if (task.getDueDate() != null && !"None".equals(task.getDueDate())) {
                    Label due = new Label("📅 " + task.getDueDate());
                    try { due.setStyle(LocalDate.parse(task.getDueDate()).isBefore(LocalDate.now()) ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;" : "-fx-text-fill: #A39DBE; -fx-font-size: 11px;"); }
                    catch (Exception ex) { due.setStyle("-fx-text-fill: #A39DBE; -fx-font-size: 11px;"); }
                    footer.getChildren().add(due);
                }
                card.getChildren().addAll(titleLbl, footer); setGraphic(card);
                ContextMenu cm = new ContextMenu();
                if ("IN_PROGRESS".equalsIgnoreCase(status) && currentUser != null && currentUser.equalsIgnoreCase(task.getAssignedTo())) {
                    MenuItem done = new MenuItem("Mark as Done ✅");
                    done.setOnAction(e -> {
                        if (HelloController.this.confirm("Mark as Done", null, "Mark \"" + task.getTitle() + "\" as completed?"))
                            changeTaskStatus(task.getId(), "DONE");
                    });
                    cm.getItems().add(done);
                }
                MenuItem del = new MenuItem("Remove Task");
                del.setOnAction(e -> {
                    if (HelloController.this.confirm("Remove Task", null, "Permanently delete \"" + task.getTitle() + "\"?"))
                        CompletableFuture.runAsync(() -> { try { DatabaseManager.deleteTask(task.getId()); Platform.runLater(this::refreshWorkspaceTasks); } catch (Exception ex) {} });
                });
                cm.getItems().add(del); setContextMenu(cm);
            }
            private void refreshWorkspaceTasks() { HelloController.this.refreshWorkspaceTasks(); }
        };
    }

    private ListCell<PortfolioItem> createPortfolioCell(boolean isOwner, ListView<PortfolioItem> lv) {
        return new ListCell<>() {
            @Override protected void updateItem(PortfolioItem item, boolean empty) {
                super.updateItem(item, empty); if (empty || item == null) { setText(null); setGraphic(null); return; }
                HBox root = new HBox(14); root.getStyleClass().add("card"); root.setAlignment(Pos.CENTER_LEFT); root.setStyle("-fx-border-color: #8b5cf6;"); root.prefWidthProperty().bind(lv.widthProperty().subtract(40));
                Label icon = new Label("📄"); icon.setStyle("-fx-font-size: 22px;");
                Label fname = new Label(item.getFileName()); fname.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-text-main;");
                Pane spacer = new Pane(); HBox.setHgrow(spacer, Priority.ALWAYS);
                Button open = actionBtn("Open", "#94a3b8", e -> openLocalFile(item.getFilePath()));
                open.setStyle("-fx-background-color: #94a3b8; -fx-pref-height: 28; -fx-font-size: 11px;");
                root.getChildren().addAll(icon, fname, spacer, open);
                if (isOwner) {
                    Button del = actionBtn("🗑", "#e74c3c", e -> {
                        if (HelloController.this.confirm("Delete File", null, "Remove \"" + item.getFileName() + "\" from your portfolio?"))
                            CompletableFuture.runAsync(() -> {
                                try { DatabaseManager.deletePortfolioItem(item.getId()); new File(item.getFilePath()).delete(); Platform.runLater(HelloController.this::refreshMyPortfolio); }
                                catch (Exception ex) {}
                            });
                    });
                    del.setStyle("-fx-background-color: #e74c3c; -fx-pref-height: 28; -fx-font-size: 11px;");
                    root.getChildren().add(del);
                }
                setGraphic(root);
            }
        };
    }

    private void openLocalFile(String path) {
        try { File f = new File(path); if (f.exists() && Desktop.isDesktopSupported()) Desktop.getDesktop().open(f); else showToast("File not found.", true); }
        catch (Exception e) { showToast("Failed to open file.", true); }
    }
}