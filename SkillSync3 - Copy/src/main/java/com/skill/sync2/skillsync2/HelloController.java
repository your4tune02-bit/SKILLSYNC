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

import org.kordamp.ikonli.javafx.FontIcon;

public class HelloController {

    // ─── FXML Injections ─────────────────────────────────────────────────────

    @FXML
    private BorderPane mainDashboard;
    @FXML
    private StackPane toastContainer;
    @FXML
    private StackPane modalOverlay;
    @FXML
    private VBox splashScreen;
    @FXML
    private Label splashText;
    @FXML
    private Label splashTitle;
    @FXML
    private Label splashSubtitle;
    @FXML
    private ImageView splashLogoView;
    @FXML
    private FontIcon themeIcon;

    @FXML
    private MenuButton notificationBell;
    @FXML
    private javafx.scene.shape.Circle notificationBadge;
    @FXML
    private Label loggedInUserLabel;
    @FXML
    private Pane sidebarContainer;
    @FXML
    private Button hamburgerBtn;
    @FXML
    private MenuButton settingsBtn;
    @FXML
    private FontIcon settingsIcon;
    @FXML
    private CheckMenuItem themeToggleItem;

    @FXML
    private Button btnNavHome, btnNavProj, btnNavSearch, btnNavInbox, btnNavChat, btnNavWorkspace, btnNavProfile;
    @FXML
    private VBox homeView, projectView, createProjectView, searchView, inboxView, workspaceView, profileView,
            publicProfileView;
    @FXML
    private HBox chatView;

    @FXML
    private StackPane authOverlay;
    @FXML
    private VBox loginCard, signupCard, forgotPasswordCard;
    @FXML
    private TextField loginNameInput, signupNameInput;
    @FXML
    private PasswordField loginPasswordInput, signupPasswordInput;
    @FXML
    private FlowPane signupSkillsPane;
    @FXML
    private TextField forgotPasswordEmailInput, forgotPasswordOtpInput;
    @FXML
    private PasswordField forgotPasswordNewPassInput;
    @FXML
    private TextField forgotPasswordNewPassVisible;
    @FXML
    private TextField signupEmailInput;
    @FXML
    private Label forgotPasswordSubtitle, publicProfileLastSeenLbl;
    @FXML
    private VBox forgotPasswordStep1, forgotPasswordStep2;
    @FXML
    private TextField loginPasswordVisible, signupPasswordVisible;

    @FXML
    private Label homeWelcomeLbl, statProjectsLbl, statInvitesLbl, statMessagesLbl;
    @FXML
    private HBox recommendationsBox;
    @FXML
    private TextField projectTitleInput;
    @FXML
    private DatePicker projectDueDateInput;
    @FXML
    private TextArea projectGoalInput, projectResponsibilitiesInput;
    @FXML
    private FlowPane projectRolePane;
    @FXML
    private ListView<Project> projectListView;

    @FXML
    private TextField searchNameInput;
    @FXML
    private FlowPane searchSkillPane;
    @FXML
    private ListView<Student> resultsList;
    @FXML
    private ListView<Invitation> teamInvitesListView, projectAppsListView;

    @FXML
    private Label chatHeaderLabel;
    @FXML
    private ListView<String> chatContactsList;
    @FXML
    private TextField chatMessageInput;
    @FXML
    private ListView<Message> chatListView;

    @FXML
    private ComboBox<String> workspaceTeamSelector;
    @FXML
    private TextField workspaceNewTaskInput;
    @FXML
    private DatePicker workspaceTaskDeadline;
    @FXML
    private ListView<Task> pendingTaskList, progressTaskList, doneTaskList;
    @FXML
    private ProgressBar workspacePulseBar;
    @FXML
    private Label pulseLabel;
    @FXML
    private Button workspaceTeamMembersBtn;

    @FXML
    private FlowPane profileSkillsPane;
    @FXML
    private TextArea profileBioInput;
    @FXML
    private ListView<PortfolioItem> myPortfolioListView;

    @FXML
    private StackPane publicProfileAvatarPane;
    @FXML
    private Label publicProfileNameLbl, publicProfileSkillsLbl, publicProfileBioLbl;
    @FXML
    private HBox publicProfileActionBox;
    @FXML
    private ListView<PortfolioItem> publicPortfolioListView;

    // ─── State ────────────────────────────────────────────────────────────────

    private String currentUser = null;
    private String currentChatUser = "";
    private String displayedChatUser = "";
    private Node previousView = null;
    private boolean isSidebarExpanded = false;
    private boolean isDarkMode = true;
    private boolean isLoginPassVisible = false;
    private boolean isSignupPassVisible = false;
    private boolean isForgotPassVisible = false;
    // Stores the verified email during the forgot-password flow (step1 → step2)
    private String forgotPasswordVerifiedEmail = null;

    // ── NEW: tracks whether we are in the signup OTP verification flow ────────
    private String signupOtpPendingUser = null; // username awaiting email verify
    private String signupOtpPendingEmail = null; // email we sent the code to
    private String signupOtpCode = null; // in-memory code (local mode)

    // ── NEW: password-eye toggle states for Change-Password dialog ────────────
    private boolean isChangeOldPassVisible = false;
    private boolean isChangeNewPassVisible = false;
    private boolean isChangeConfPassVisible = false;

    private Timeline backgroundPoller;
    private Pane particlePane;
    private AnimationTimer particleTimer;
    private final Random random = new Random();
    private Set<String> onlineUsersCache = new java.util.HashSet<>();
    private boolean currentProjectExpired = false;

    // ─── Per-user preferences ─────────────────────────────────────────────────
    // Keys for user-specific settings are namespaced as "<username>.<key>" so each
    // app account has its own isolated settings even when multiple users share the
    // same OS account. Global keys (currentUser, darkMode, particleEffect) have no
    // prefix since they must be readable before any user is logged in.
    private final Preferences rawPrefs = Preferences.userNodeForPackage(HelloController.class);

    private boolean prefGetBoolean(String key, boolean def) {
        if (currentUser == null)
            return rawPrefs.getBoolean(key, def);
        return rawPrefs.getBoolean(currentUser + "." + key, def);
    }

    private void prefPutBoolean(String key, boolean value) {
        if (currentUser == null) {
            rawPrefs.putBoolean(key, value);
            return;
        }
        rawPrefs.putBoolean(currentUser + "." + key, value);
    }

    private String prefGet(String key, String def) {
        return rawPrefs.get(key, def);
    }

    private void prefPut(String key, String value) {
        rawPrefs.put(key, value);
    }

    private void prefRemove(String key) {
        rawPrefs.remove(key);
    }

    private final String UPLOAD_DIR = System.getProperty("user.home") + File.separator + ".skillsync" + File.separator
            + "uploads";
    private final String[] SKILL_LIST = { "Java", "Python", "UI/UX", "SQL", "Web Dev", "C++", "C#", "Mobile Dev",
            "DevOps", "AI/ML" };

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

        // ── Enter-key wiring for auth forms ──────────────────────────────────────
        // Login card
        loginNameInput.setOnAction(e -> loginPasswordInput.requestFocus());
        loginPasswordInput.setOnAction(e -> onLoginSubmit());
        loginPasswordVisible.setOnAction(e -> onLoginSubmit());

        // Signup card — Tab-like Enter flow
        signupNameInput.setOnAction(e -> signupPasswordInput.requestFocus());
        signupPasswordInput.setOnAction(e -> signupEmailInput.requestFocus());
        signupPasswordVisible.setOnAction(e -> signupEmailInput.requestFocus());
        signupEmailInput.setOnAction(e -> onSignupSubmit());

        // Forgot-password card
        forgotPasswordEmailInput.setOnAction(e -> onSendResetCodeSubmit());
        forgotPasswordOtpInput.setOnAction(e -> forgotPasswordNewPassInput.requestFocus());
        forgotPasswordNewPassInput.setOnAction(e -> onVerifyResetCodeSubmit());
        forgotPasswordNewPassVisible.setOnAction(e -> onVerifyResetCodeSubmit());

        notificationBell.setOnShowing(e -> {
            notificationBadge.setVisible(false);
            if (currentUser != null)
                CompletableFuture.runAsync(() -> DatabaseManager.markInvitesAsRead(currentUser));
        });
        settingsBtn.setOnShowing(e -> {
            RotateTransition rt = new RotateTransition(Duration.millis(400), settingsIcon);
            rt.setByAngle(90);
            rt.play();
        });

        setupViewEnterKeys();

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
        if (action != null)
            btn.setOnAction(action);
        return btn;
    }

    private void setupListPlaceholders() {
        projectListView.setPlaceholder(
                placeholder("It's crickets in here... 🦗 Be a trailblazer and start a Squad!", "#94a3b8"));
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
        backgroundPoller = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (currentUser == null)
                return;
            CompletableFuture.supplyAsync(() -> DatabaseManager.getStudentProfile(currentUser))
                    .thenAccept(profile -> Platform.runLater(() -> {
                        if (profile == null) {
                            showToast("Session Terminated: Account no longer exists.", true);
                            forceLogoutCleanup();
                        } else if (!"NETWORK_ERROR".equals(profile.getName())) {
                            if (!prefGetBoolean("ghost_hide_online", false))
                                CompletableFuture.runAsync(() -> DatabaseManager.sendHeartbeat(currentUser));
                            pollNotifications();
                            refreshBell();
                            refreshInbox();
                            if (chatView.isVisible())
                                refreshChatHistory(false);
                            if (workspaceView.isVisible())
                                refreshWorkspaceTasks();
                            if (homeView.isVisible())
                                refreshRecommendations();
                            if (searchView.isVisible())
                                executeSearch();
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
                if (empty || date == null) {
                    setDisable(true);
                    return;
                }
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
            if (newText.matches("[\\d/\\-]{0,10}"))
                return change;
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
        // Initial hidden states for staggered entrance
        splashLogoView.setScaleX(0.4);
        splashLogoView.setScaleY(0.4);
        splashLogoView.setOpacity(0);
        splashTitle.setOpacity(0);
        splashTitle.setTranslateY(18);
        splashSubtitle.setOpacity(0);
        splashSubtitle.setTranslateY(12);
        splashText.setOpacity(0);

        // 1. Logo fade + elastic bounce
        FadeTransition logoFade = new FadeTransition(Duration.millis(800), splashLogoView);
        logoFade.setToValue(1.0);
        ScaleTransition logoBounce = new ScaleTransition(Duration.millis(800), splashLogoView);
        logoBounce.setToX(1.0);
        logoBounce.setToY(1.0);
        logoBounce.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.4, 1.0));

        // 2. Title slide up
        FadeTransition titleFade = new FadeTransition(Duration.millis(600), splashTitle);
        titleFade.setToValue(1.0);
        TranslateTransition titleSlide = new TranslateTransition(Duration.millis(600), splashTitle);
        titleSlide.setToY(0);
        titleSlide.setInterpolator(Interpolator.EASE_OUT);

        // 3. Subtitle slide up
        FadeTransition subFade = new FadeTransition(Duration.millis(500), splashSubtitle);
        subFade.setToValue(1.0);
        TranslateTransition subSlide = new TranslateTransition(Duration.millis(500), splashSubtitle);
        subSlide.setToY(0);
        subSlide.setInterpolator(Interpolator.EASE_OUT);

        // 4. Loading text reveal
        FadeTransition loadFade = new FadeTransition(Duration.millis(400), splashText);
        loadFade.setToValue(1.0);

        // Logo container glow pulse
        javafx.scene.Parent logoParent = splashLogoView.getParent();
        StackPane logoContainer = (logoParent instanceof StackPane) ? (StackPane) logoParent : new StackPane();
        javafx.scene.effect.Effect existingEffect = logoContainer.getEffect();
        DropShadow glow;
        if (existingEffect instanceof DropShadow) {
            glow = (DropShadow) existingEffect;
        } else {
            glow = new DropShadow(20, javafx.scene.paint.Color.web("#8b5cf6"));
            logoContainer.setEffect(glow);
        }
        Timeline pulseGlow = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 20),
                        new KeyValue(glow.spreadProperty(), 0.1)),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(glow.radiusProperty(), 55),
                        new KeyValue(glow.spreadProperty(), 0.4)));
        pulseGlow.setAutoReverse(true);
        pulseGlow.setCycleCount(Animation.INDEFINITE);

        // Staggered entrance sequence
        SequentialTransition entrance = new SequentialTransition(
                new ParallelTransition(logoFade, logoBounce),
                new PauseTransition(Duration.millis(150)),
                new ParallelTransition(titleFade, titleSlide),
                new PauseTransition(Duration.millis(100)),
                new ParallelTransition(subFade, subSlide),
                new PauseTransition(Duration.millis(200)),
                loadFade);
        entrance.play();
        pulseGlow.play();

        setupSidebarClipping();
        setupKeyboardShortcuts();
        startParticleEngine(prefGet("particleEffect", "OFF"));

        PauseTransition pause = new PauseTransition(Duration.seconds(3.5));

        pause.setOnFinished(e -> {
            pulseGlow.stop();

            isDarkMode = prefGetBoolean("darkMode", true);
            applyTheme();

            String savedUser = prefGet("currentUser", null);

            if (savedUser != null) {
                CompletableFuture.supplyAsync(() -> DatabaseManager.getStudentProfile(savedUser))
                        .thenAccept(profile -> Platform.runLater(() -> {
                            if (profile == null) {
                                prefRemove("currentUser");
                                authOverlay.setVisible(true);
                                showToast("Session expired. Please log in again.", true);
                            } else if ("NETWORK_ERROR".equals(profile.getName())) {
                                authOverlay.setVisible(true);
                                showToast("Connection failed. Check your database.", true);
                            } else {
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
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> splashScreen.setVisible(false));
        ft.play();
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    @FXML
    void onLoginSubmit() {
        String identifier = loginNameInput.getText().trim();
        String pass = isLoginPassVisible ? loginPasswordVisible.getText() : loginPasswordInput.getText();

        loginNameInput.getStyleClass().remove("input-error");
        loginPasswordInput.getStyleClass().remove("input-error");

        if (identifier.isEmpty()) {
            loginNameInput.getStyleClass().add("input-error");
            showToast("Please enter your username or email.", true);
            return;
        }
        if (pass.isEmpty()) {
            loginPasswordInput.getStyleClass().add("input-error");
            showToast("Please enter your password.", true);
            return;
        }
        // Basic sanity: if it looks like an email it must have a dot after @
        boolean looksLikeEmail = identifier.contains("@");
        if (looksLikeEmail && !identifier.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            loginNameInput.getStyleClass().add("input-error");
            showToast("That doesn't look like a valid email address.", true);
            return;
        }

        loginCard.setDisable(true);
        CompletableFuture.supplyAsync(() -> DatabaseManager.verifyLogin(identifier, pass))
                .thenAccept(resolvedName -> Platform.runLater(() -> {
                    loginCard.setDisable(false);
                    if (resolvedName != null) {
                        loginSuccess(resolvedName);
                        showToast("Welcome back, " + resolvedName + "! \uD83C\uDF89", false);
                    } else {
                        loginNameInput.getStyleClass().add("input-error");
                        loginPasswordInput.getStyleClass().add("input-error");
                        showToast(looksLikeEmail
                                ? "No account found for that email, or password is wrong."
                                : "Invalid username or password.", true);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        loginCard.setDisable(false);
                        showToast("Login failed. Check your database connection.", true);
                    });
                    return null;
                });
    }

    @FXML
    void onSignupSubmit() {
        String username = signupNameInput.getText().trim();
        String pass = isSignupPassVisible ? signupPasswordVisible.getText() : signupPasswordInput.getText();
        String email = signupEmailInput.getText().trim();

        signupNameInput.getStyleClass().remove("input-error");
        signupPasswordInput.getStyleClass().remove("input-error");
        signupEmailInput.getStyleClass().remove("input-error");

        List<String> skills = new ArrayList<>();
        for (Node n : signupSkillsPane.getChildren())
            if (((ToggleButton) n).isSelected())
                skills.add(((ToggleButton) n).getText());

        if (username.isEmpty()) {
            signupNameInput.getStyleClass().add("input-error");
            showToast("Username is required.", true);
            return;
        }
        if (!isValidUsername(username)) {
            signupNameInput.getStyleClass().add("input-error");
            showToast("Username: start with a letter, letters/digits/_ /- only, min 3 chars.", true);
            return;
        }
        if (!pass.matches("^(?=.*[0-9])(?=.*[A-Z]).{8,}$")) {
            signupPasswordInput.getStyleClass().add("input-error");
            showToast("Password: 8+ chars, 1 uppercase, 1 number.", true);
            return;
        }
        if (email.isEmpty()) {
            signupEmailInput.getStyleClass().add("input-error");
            showToast("Email is required for account recovery.", true);
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            signupEmailInput.getStyleClass().add("input-error");
            showToast("Please enter a valid email address.", true);
            return;
        }
        if (skills.isEmpty()) {
            showToast("Select at least one skill.", true);
            return;
        }

        signupCard.setDisable(true);
        final String finalEmail = email.toLowerCase();
        final String finalPass = pass;
        final String finalSkills = String.join(", ", skills);

        CompletableFuture.runAsync(() -> {
            if (DatabaseManager.emailExists(finalEmail)) {
                Platform.runLater(() -> {
                    signupCard.setDisable(false);
                    signupEmailInput.getStyleClass().add("input-error");
                    showToast("An account with that email already exists.", true);
                });
                return;
            }
            try {
                DatabaseManager.addStudent(username, finalPass, finalEmail, finalSkills);
                // Generate signup OTP and show verification dialog
                String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
                DatabaseManager.storeOtp(finalEmail, code, "SIGNUP");
                Platform.runLater(() -> {
                    signupCard.setDisable(false);
                    signupOtpPendingUser = username;
                    signupOtpPendingEmail = finalEmail;
                    signupOtpCode = code;
                    // Toast shows OTP (local/offline mode — no SMTP)
                    showToast("Verification code for " + username + ": " + code, false);
                    showSignupOtpDialog();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    signupCard.setDisable(false);
                    String msg = e.getMessage() != null && e.getMessage().contains("email")
                            ? "An account with that email already exists."
                            : "Username already taken!";
                    if (msg.contains("email"))
                        signupEmailInput.getStyleClass().add("input-error");
                    else
                        signupNameInput.getStyleClass().add("input-error");
                    showToast(msg, true);
                });
            }
        });
    }

    /**
     * Shows the signup email-verification OTP dialog.
     * The user must enter the 6-digit code displayed in the toast.
     * On success the account is marked verified and the user is logged in.
     * On skip/cancel the account remains unverified but usable.
     */
    private void showSignupOtpDialog() {
        VBox card = buildModalCard(440);
        card.getChildren().add(modalTitle("Verify Your Email 📧"));

        Label info = new Label("A verification code has been generated for your account. "
                + "Copy it below and enter it in the field. Code expires in 15 minutes.");
        info.setWrapText(true);
        info.setStyle("-fx-text-fill: " + (isDarkMode ? "#A39DBE" : "#6C6687")
                + "; -fx-font-size: 13px; -fx-padding: 0 0 8 0;");
        card.getChildren().add(info);

        // ── Visible OTP display box ───────────────────────────────────────────
        Label otpDisplayLabel = new Label(signupOtpCode != null ? signupOtpCode : "------");
        otpDisplayLabel.setStyle(
                "-fx-font-size: 32px; -fx-font-weight: bold; -fx-letter-spacing: 8px;"
                        + "-fx-text-fill: #00E5FF; -fx-padding: 10 0;"
                        + "-fx-font-family: monospace;");
        otpDisplayLabel.setMaxWidth(Double.MAX_VALUE);
        otpDisplayLabel.setAlignment(Pos.CENTER);

        VBox otpBox = new VBox(6);
        Label otpHint = new Label("Your verification code:");
        otpHint.setStyle("-fx-text-fill: " + (isDarkMode ? "#A39DBE" : "#6C6687") + "; -fx-font-size: 11px;");
        otpBox.setAlignment(Pos.CENTER);
        otpBox.setStyle("-fx-background-color: " + (isDarkMode ? "rgba(0,229,255,0.07)" : "rgba(0,180,200,0.07)")
                + "; -fx-background-radius: 10; -fx-border-color: rgba(0,229,255,0.3);"
                + "-fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 12 20;");
        otpBox.getChildren().addAll(otpHint, otpDisplayLabel);
        card.getChildren().add(otpBox);

        TextField codeField = new TextField();
        codeField.setPromptText("Enter 6-digit code");
        codeField.setStyle(dialogInputStyle());
        codeField.setMaxWidth(Double.MAX_VALUE);
        // Accept only digits, max 6 chars
        codeField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d{0,6}"))
                return change;
            return null;
        }));
        card.getChildren().addAll(modalSubLabel("Enter Code:"), codeField);

        HBox buttons = modalButtonRow();
        Button verifyBtn = modalPrimaryButton("Verify ✓");
        Button skipBtn = modalSecondaryButton("Skip for now");

        Runnable doVerify = () -> {
            String entered = codeField.getText().trim();
            if (entered.length() != 6) {
                showToast("Enter the full 6-digit code.", true);
                return;
            }
            boolean ok = DatabaseManager.verifyOtp(signupOtpPendingEmail, entered, "SIGNUP");
            if (!ok && !entered.equals(signupOtpCode)) { // fallback: in-memory code
                showToast("Incorrect code. Check the toast and try again.", true);
                return;
            }
            DatabaseManager.markEmailVerified(signupOtpPendingUser);
            signupOtpCode = null;
            signupOtpPendingEmail = null;
            String verifiedUser = signupOtpPendingUser;
            signupOtpPendingUser = null;
            dismissModal(card);
            // Clear signup fields and switch to login
            signupNameInput.clear();
            signupPasswordInput.clear();
            signupEmailInput.clear();
            switchToLogin();
            loginNameInput.setText(verifiedUser);
            showToast("Email verified! Account ready. Please log in. 🚀", false);
        };

        verifyBtn.setOnAction(e -> doVerify.run());
        // Enter key submits
        codeField.setOnAction(e -> doVerify.run());

        skipBtn.setOnAction(e -> {
            signupOtpCode = null;
            signupOtpPendingEmail = null;
            signupOtpPendingUser = null;
            dismissModal(card);
            switchToLogin();
            showToast("Account created — verify your email later via Settings. 🔑", false);
        });

        modalOverlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                skipBtn.fire();
        });

        buttons.getChildren().addAll(skipBtn, verifyBtn);
        card.getChildren().add(buttons);
        showModal(card);
    }

    private void loginSuccess(String username) {
        currentUser = username;
        prefPut("currentUser", username);
        if (!prefGetBoolean("ghost_hide_online", false))
            CompletableFuture.runAsync(() -> DatabaseManager.sendHeartbeat(currentUser));
        loggedInUserLabel.setText("User: " + currentUser);
        authOverlay.setVisible(false);
        mainDashboard.setVisible(true);
        homeWelcomeLbl.setText("Welcome Back! 🚀");

        loginNameInput.clear();
        loginPasswordInput.clear();
        signupNameInput.clear();
        signupPasswordInput.clear();
        signupEmailInput.clear();

        refreshProjectList();
        refreshInbox();
        executeSearch();
        refreshRecommendations();
        refreshBell();
        backgroundPoller.stop(); // stop any previous session's poller first
        backgroundPoller.play();
        Platform.runLater(this::setupKeyboardShortcuts);
        navToHome();
    }

    @FXML
    void onLogoutClick() {
        if (confirm("Log Out", null, "Heading out? We'll keep your seat warm! 🍕")) {
            CompletableFuture.runAsync(() -> DatabaseManager.forceOffline(currentUser));
            forceLogoutCleanup();
        }
    }

    private void forceLogoutCleanup() {
        currentUser = null;
        currentChatUser = "";
        displayedChatUser = "";
        prefRemove("currentUser");
        backgroundPoller.stop();

        resultsList.getItems().clear();
        projectListView.getItems().clear();
        teamInvitesListView.getItems().clear();
        projectAppsListView.getItems().clear();
        chatListView.getItems().clear();
        chatContactsList.getItems().clear();
        pendingTaskList.getItems().clear();
        progressTaskList.getItems().clear();
        doneTaskList.getItems().clear();
        workspaceTeamSelector.getItems().clear();
        workspaceTeamSelector.setValue(null);

        chatHeaderLabel.setText("Select a contact to start chatting");
        btnNavInbox.setText("Pings & Invites");
        btnNavChat.setText("Squad Chat");
        if (isSidebarExpanded)
            toggleSidebar();
        mainDashboard.setVisible(false);
        authOverlay.setVisible(true);
        switchToLogin();
    }

    // Holds the 6-digit code generated during the forgot-password flow (in-memory
    // only)
    private String forgotPasswordOtpCode = null;

    @FXML
    void onSendResetCodeSubmit() {
        String email = forgotPasswordEmailInput.getText().trim();
        forgotPasswordEmailInput.getStyleClass().remove("input-error");

        if (email.isEmpty()) {
            forgotPasswordEmailInput.getStyleClass().add("input-error");
            showToast("Please enter your email address.", true);
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            forgotPasswordEmailInput.getStyleClass().add("input-error");
            showToast("That doesn't look like a valid email address.", true);
            return;
        }

        forgotPasswordCard.setDisable(true);
        CompletableFuture.supplyAsync(() -> DatabaseManager.getUsernameByEmail(email))
                .thenAccept(username -> Platform.runLater(() -> {
                    forgotPasswordCard.setDisable(false);
                    if (username == null) {
                        forgotPasswordEmailInput.getStyleClass().add("input-error");
                        showToast("No account found with that email address.", true);
                        return;
                    }
                    // Generate a 6-digit code and store it in memory
                    forgotPasswordOtpCode = String.format("%06d", new Random().nextInt(1_000_000));
                    forgotPasswordVerifiedEmail = email.toLowerCase();

                    // In local mode there is no email server — show the code directly as a
                    // toast so the user can proceed. In a networked version this would be emailed.
                    showToast("Reset code for " + username + ": " + forgotPasswordOtpCode, false);

                    forgotPasswordSubtitle.setText("Code sent! Enter it below to set a new password.");
                    forgotPasswordStep1.setVisible(false);
                    forgotPasswordStep1.setManaged(false);
                    forgotPasswordStep2.setVisible(true);
                    forgotPasswordStep2.setManaged(true);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        forgotPasswordCard.setDisable(false);
                        showToast("Could not look up that email. Check your connection.", true);
                    });
                    return null;
                });
    }

    @FXML
    void onVerifyResetCodeSubmit() {
        String enteredCode = forgotPasswordOtpInput.getText().trim();
        String newPass = isForgotPassVisible
                ? forgotPasswordNewPassVisible.getText()
                : forgotPasswordNewPassInput.getText();

        forgotPasswordOtpInput.getStyleClass().remove("input-error");
        forgotPasswordNewPassInput.getStyleClass().remove("input-error");

        if (enteredCode.isEmpty()) {
            forgotPasswordOtpInput.getStyleClass().add("input-error");
            showToast("Enter the 6-digit code.", true);
            return;
        }
        if (forgotPasswordOtpCode == null || !forgotPasswordOtpCode.equals(enteredCode)) {
            forgotPasswordOtpInput.getStyleClass().add("input-error");
            showToast("Incorrect code. Check the toast and try again.", true);
            return;
        }
        if (newPass.isEmpty()) {
            forgotPasswordNewPassInput.getStyleClass().add("input-error");
            showToast("Enter your new password.", true);
            return;
        }
        if (!newPass.matches("^(?=.*[0-9])(?=.*[A-Z]).{8,}$")) {
            forgotPasswordNewPassInput.getStyleClass().add("input-error");
            showToast("Password: 8+ chars, 1 uppercase, 1 number.", true);
            return;
        }

        final String emailToReset = forgotPasswordVerifiedEmail;
        forgotPasswordCard.setDisable(true);
        CompletableFuture.supplyAsync(() -> DatabaseManager.changePasswordByEmail(emailToReset, newPass))
                .thenAccept(ok -> Platform.runLater(() -> {
                    forgotPasswordCard.setDisable(false);
                    if (ok) {
                        // Invalidate the code immediately after use
                        forgotPasswordOtpCode = null;
                        forgotPasswordVerifiedEmail = null;
                        showToast("Password updated! You can now log in. 🔒", false);
                        switchToLogin();
                    } else {
                        showToast("Failed to update password. Try again.", true);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        forgotPasswordCard.setDisable(false);
                        showToast("Something went wrong. Check your connection.", true);
                    });
                    return null;
                });
    }

    @FXML
    void toggleForgotPassword() {
        isForgotPassVisible = !isForgotPassVisible;
        if (isForgotPassVisible) {
            forgotPasswordNewPassVisible.setText(forgotPasswordNewPassInput.getText());
            forgotPasswordNewPassVisible.setVisible(true);
            forgotPasswordNewPassVisible.setManaged(true);
            forgotPasswordNewPassInput.setVisible(false);
            forgotPasswordNewPassInput.setManaged(false);
        } else {
            forgotPasswordNewPassInput.setText(forgotPasswordNewPassVisible.getText());
            forgotPasswordNewPassInput.setVisible(true);
            forgotPasswordNewPassInput.setManaged(true);
            forgotPasswordNewPassVisible.setVisible(false);
            forgotPasswordNewPassVisible.setManaged(false);
        }
    }

    @FXML
    void switchToForgotPassword() {
        loginCard.setVisible(false);
        signupCard.setVisible(false);
        forgotPasswordCard.setVisible(true);
    }

    @FXML
    void switchToSignup() {
        loginCard.setVisible(false);
        forgotPasswordCard.setVisible(false);
        signupCard.setVisible(true);
    }

    @FXML
    void switchToLogin() {
        signupCard.setVisible(false);
        forgotPasswordCard.setVisible(false);
        loginCard.setVisible(true);
        forgotPasswordStep2.setVisible(false);
        forgotPasswordStep2.setManaged(false);
        forgotPasswordStep1.setVisible(true);
        forgotPasswordStep1.setManaged(true);
        forgotPasswordSubtitle.setText("Enter your email to receive a 6-digit reset code.");
        forgotPasswordEmailInput.clear();
        forgotPasswordOtpInput.clear();
        forgotPasswordNewPassInput.clear();
        forgotPasswordNewPassVisible.clear();
        forgotPasswordNewPassInput.setVisible(true);
        forgotPasswordNewPassInput.setManaged(true);
        forgotPasswordNewPassVisible.setVisible(false);
        forgotPasswordNewPassVisible.setManaged(false);
        isForgotPassVisible = false;
        forgotPasswordOtpCode = null;
        forgotPasswordVerifiedEmail = null;
    }

    @FXML
    void toggleLoginPassword() {
        isLoginPassVisible = !isLoginPassVisible;
        if (isLoginPassVisible) {
            loginPasswordVisible.setText(loginPasswordInput.getText());
            loginPasswordVisible.setVisible(true);
            loginPasswordVisible.setManaged(true);
            loginPasswordInput.setVisible(false);
            loginPasswordInput.setManaged(false);
        } else {
            loginPasswordInput.setText(loginPasswordVisible.getText());
            loginPasswordInput.setVisible(true);
            loginPasswordInput.setManaged(true);
            loginPasswordVisible.setVisible(false);
            loginPasswordVisible.setManaged(false);
        }
    }

    @FXML
    void toggleSignupPassword() {
        isSignupPassVisible = !isSignupPassVisible;
        if (isSignupPassVisible) {
            signupPasswordVisible.setText(signupPasswordInput.getText());
            signupPasswordVisible.setVisible(true);
            signupPasswordVisible.setManaged(true);
            signupPasswordInput.setVisible(false);
            signupPasswordInput.setManaged(false);
        } else {
            signupPasswordInput.setText(signupPasswordVisible.getText());
            signupPasswordInput.setVisible(true);
            signupPasswordInput.setManaged(true);
            signupPasswordVisible.setVisible(false);
            signupPasswordVisible.setManaged(false);
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML
    protected void navToHome() {
        switchView(homeView, btnNavHome);
        refreshHomeDashboardStats();
    }

    @FXML
    protected void navToProjects() {
        switchView(projectView, btnNavProj);
        refreshProjectList();
    }

    @FXML
    protected void navToSearch() {
        switchView(searchView, btnNavSearch);
    }

    @FXML
    protected void navToInbox() {
        switchView(inboxView, btnNavInbox);
        refreshInbox();
    }

    @FXML
    protected void navToChat() {
        switchView(chatView, btnNavChat);
        refreshChatContacts();
    }

    @FXML
    protected void navToWorkspace() {
        switchView(workspaceView, btnNavWorkspace);
        CompletableFuture.supplyAsync(() -> DatabaseManager.getUserTeams(currentUser))
                .thenAccept(teams -> Platform.runLater(() -> {
                    if (teams == null || teams.isEmpty()) {
                        showToast("You're not on any squad yet. Join or create a project!", false);
                        navToHome();
                        return;
                    }
                    String current = workspaceTeamSelector.getValue();
                    workspaceTeamSelector.getItems().setAll(teams);
                    if (current != null && teams.contains(current))
                        workspaceTeamSelector.setValue(current);
                    else
                        workspaceTeamSelector.setValue(teams.get(0));
                    refreshWorkspaceTasks();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showToast("Failed to load your squads.", true));
                    return null;
                });
    }

    @FXML
    protected void navToProfile() {
        switchView(profileView, btnNavProfile);
        refreshMyPortfolio();
        CompletableFuture.supplyAsync(() -> DatabaseManager.getStudentProfile(currentUser))
                .thenAccept(student -> Platform.runLater(() -> {
                    if (student == null || "NETWORK_ERROR".equals(student.getName())) {
                        showToast("Could not load profile data.", true);
                        return;
                    }
                    profileBioInput.setText(student.getBio() != null ? student.getBio() : "");
                    String skills = student.getSkills() != null ? student.getSkills() : "";
                    for (Node n : profileSkillsPane.getChildren())
                        ((ToggleButton) n).setSelected(skills.contains(((ToggleButton) n).getText()));
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showToast("Failed to load profile.", true));
                    return null;
                });

        ContextMenu bioCm = new ContextMenu();
        MenuItem copyBio = new MenuItem("📋 Copy Bio");
        copyBio.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(profileBioInput.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            showToast("Bio copied.", false);
        });
        MenuItem clearBio = new MenuItem("🗑 Clear Bio");
        clearBio.setOnAction(e -> profileBioInput.clear());
        bioCm.getItems().addAll(copyBio, clearBio);
        profileBioInput.setContextMenu(bioCm);
    }

    private void switchView(Node target, Button activeBtn) {
        for (Node v : new Node[] { homeView, projectView, createProjectView, searchView, inboxView, chatView,
                workspaceView, profileView, publicProfileView })
            v.setVisible(false);
        for (Button b : new Button[] { btnNavHome, btnNavProj, btnNavSearch, btnNavInbox, btnNavChat, btnNavWorkspace,
                btnNavProfile })
            b.getStyleClass().remove("nav-btn-active");

        target.setVisible(true);
        if (activeBtn != null)
            activeBtn.getStyleClass().add("nav-btn-active");

        FadeTransition fade = new FadeTransition(Duration.millis(250), target);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(250), target);
        slide.setFromY(15);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void setupSidebarClipping() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarContainer.prefWidthProperty());
        clip.heightProperty().bind(sidebarContainer.heightProperty());
        sidebarContainer.setClip(clip);
        sidebarContainer.minWidthProperty().bind(sidebarContainer.prefWidthProperty());
        sidebarContainer.maxWidthProperty().bind(sidebarContainer.prefWidthProperty());
        sidebarContainer.setPrefWidth(0);
        isSidebarExpanded = false;
    }

    @FXML
    private void toggleSidebar() {
        isSidebarExpanded = !isSidebarExpanded;
        new Timeline(new KeyFrame(Duration.millis(280), new KeyValue(sidebarContainer.prefWidthProperty(),
                isSidebarExpanded ? 220 : 0, Interpolator.EASE_BOTH))).play();
        RotateTransition rt = new RotateTransition(Duration.millis(280), hamburgerBtn);
        rt.setToAngle(isSidebarExpanded ? 90 : 0);
        rt.play();
    }

    private void setupKeyboardShortcuts() {
        Scene scene = mainDashboard.getScene();
        if (scene == null)
            return;
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN), this::navToHome);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN),
                this::navToProjects);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), this::navToSearch);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN), this::navToInbox);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
            if (publicProfileView.isVisible() || createProjectView.isVisible())
                onBackFromProfile();
        });

        // ── Chat: Ctrl+Enter sends message ────────────────────────────────────
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                () -> {
                    if (chatView.isVisible())
                        onSendChatMessage();
                });

        // ── Workspace: Ctrl+T adds task ───────────────────────────────────────
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN),
                () -> {
                    if (workspaceView.isVisible())
                        workspaceNewTaskInput.requestFocus();
                });
    }

    /**
     * Call once from initialize() after FXML injection. Wires Enter keys for main
     * views.
     */
    private void setupViewEnterKeys() {
        // Chat: Enter sends message
        if (chatMessageInput != null) {
            chatMessageInput.setOnAction(e -> onSendChatMessage());
        }
        // Workspace: Enter adds task
        if (workspaceNewTaskInput != null) {
            workspaceNewTaskInput.setOnAction(e -> onAddTask());
        }
        // Search: Enter runs search
        if (searchNameInput != null) {
            searchNameInput.setOnAction(e -> executeSearch());
        }
    }

    // ─── Notifications & Home ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void pollNotifications() {
        if (currentUser == null)
            return;
        CompletableFuture.supplyAsync(() -> new Object[] {
                DatabaseManager.getUnreadInviteCount(currentUser),
                DatabaseManager.getUnreadMessageCount(currentUser),
                DatabaseManager.getOnlineUsers()
        }).thenAccept(r -> Platform.runLater(() -> {
            int inv = (int) r[0];
            int msg = (int) r[1];
            onlineUsersCache = (Set<String>) r[2];
            btnNavInbox.setText(inv > 0 ? "Pings & Invites (" + inv + ")" : "Pings & Invites");
            btnNavChat.setText(msg > 0 ? "Squad Chat (" + msg + ")" : "Squad Chat");
            if (homeView.isVisible()) {
                statInvitesLbl.setText(String.valueOf(inv));
                statMessagesLbl.setText(String.valueOf(msg));
            }
        })).exceptionally(ex -> null); // silent — background poll, non-critical
    }

    private void refreshHomeDashboardStats() {
        CompletableFuture.supplyAsync(() -> {
            List<Project> all = DatabaseManager.getAllProjects();
            if (all == null)
                return 0;
            return (int) all.stream().filter(p -> {
                String d = p.getDueDate();
                if (d == null || "Not Specified".equals(d))
                    return true;
                try {
                    return LocalDate.parse(d).isAfter(LocalDate.now().minusDays(1));
                } catch (Exception e) {
                    return true;
                }
            }).count();
        }).thenAccept(n -> Platform.runLater(() -> statProjectsLbl.setText(String.valueOf(n))));
        pollNotifications();
    }

    private void refreshBell() {
        CompletableFuture.supplyAsync(() -> DatabaseManager.getRecentActivity(currentUser))
                .thenAccept(activities -> Platform.runLater(() -> {
                    notificationBell.getItems().clear();
                    if (activities.isEmpty()) {
                        Label lbl = new Label("No recent activity.");
                        lbl.setStyle("-fx-text-fill: #A39DBE; -fx-padding: 5 10;");
                        CustomMenuItem item = new CustomMenuItem(lbl);
                        item.setHideOnClick(true);
                        notificationBell.getItems().add(item);
                        notificationBadge.setVisible(false);
                    } else {
                        if (!notificationBell.isShowing())
                            notificationBadge.setVisible(true);
                        for (String act : activities) {
                            Label lbl = new Label(act);
                            lbl.setStyle("-fx-text-fill: -color-text-main; -fx-font-weight: bold; -fx-padding: 5 10;");
                            CustomMenuItem item = new CustomMenuItem(lbl);
                            item.setHideOnClick(true);
                            notificationBell.getItems().add(item);
                        }
                    }
                }));
    }

    private void refreshRecommendations() {
        if (currentUser == null)
            return;
        CompletableFuture.supplyAsync(() -> DatabaseManager.getRecommendations(currentUser))
                .thenAccept(recs -> Platform.runLater(() -> {
                    if (recs == null)
                        return;
                    recommendationsBox.getChildren().clear();
                    for (Student s : recs) {
                        VBox card = new VBox(6);
                        card.getStyleClass().add("card");
                        card.setAlignment(Pos.CENTER);
                        card.setStyle("-fx-padding: 14 22;");
                        card.setPrefWidth(210);
                        card.setMaxWidth(210);
                        Label name = new Label(s.getName());
                        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-text-main;");
                        String firstSkill = (s.getSkills() != null && !s.getSkills().isEmpty())
                                ? s.getSkills().split(",")[0].trim()
                                : "No skills listed";
                        Label skill = new Label(firstSkill);
                        skill.setStyle("-fx-text-fill: #8b5cf6; -fx-font-weight: bold; -fx-font-size: 12px;");
                        Button view = new Button("View Profile");
                        view.getStyleClass().add("action-btn");
                        view.setStyle("-fx-padding: 5 14; -fx-font-size: 11px;");
                        view.setOnAction(e -> showPublicProfile(s.getName()));
                        card.getChildren().addAll(getAvatar(s.getName(), 42, 17), name, skill, view);
                        recommendationsBox.getChildren().add(card);
                    }
                }))
                .exceptionally(ex -> null); // silent — not critical UI
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    @FXML
    private void executeSearch() {
        String query = searchNameInput.getText().trim();
        List<String> skills = new ArrayList<>();
        for (Node n : searchSkillPane.getChildren())
            if (((ToggleButton) n).isSelected())
                skills.add(((ToggleButton) n).getText());
        CompletableFuture.supplyAsync(() -> DatabaseManager.searchStudents(query, skills))
                .thenAccept(r -> Platform.runLater(() -> {
                    if (r != null)
                        resultsList.getItems().setAll(r);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showToast("Search failed. Check your connection.", true));
                    return null;
                });
    }

    @FXML
    private void onClearSearchFilters() {
        searchNameInput.clear();
        for (Node n : searchSkillPane.getChildren())
            ((ToggleButton) n).setSelected(false);
        executeSearch();
    }

    @FXML
    private void onSurpriseMeClick() {
        String skill = "";
        for (Node n : searchSkillPane.getChildren()) {
            if (((ToggleButton) n).isSelected()) {
                skill = ((ToggleButton) n).getText();
                break;
            }
        }
        if (skill.isEmpty()) {
            showToast("Select a skill filter first!", true);
            return;
        }
        final String targetSkill = skill;
        CompletableFuture.supplyAsync(() -> DatabaseManager.getRandomMatch(currentUser, targetSkill))
                .thenAccept(match -> Platform.runLater(() -> {
                    if (match == null) {
                        showToast("No users found with '" + targetSkill + "' skill.", true);
                        return;
                    }
                    final String msg = "\u26A1 SkillSync Smart Match! Looking for a " + targetSkill
                            + " expert. Let's team up!";
                    CompletableFuture.runAsync(() -> {
                        try {
                            boolean ok = DatabaseManager.sendInvite(match, currentUser, msg, "TEAM", "");
                            Platform.runLater(
                                    () -> showToast(ok ? "Match found! Invite sent to " + match + " \uD83C\uDFAF"
                                            : "Match found, but invite already exists.", !ok));
                        } catch (Exception e) {
                            Platform.runLater(() -> showToast("Failed to send match invite.", true));
                        }
                    });
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showToast("Surprise Me failed. Try again!", true));
                    return null;
                });
    }

    // ─── Projects ─────────────────────────────────────────────────────────────

    private void refreshProjectList() {
        CompletableFuture.supplyAsync(DatabaseManager::getAllProjects)
                .thenAccept(p -> Platform.runLater(() -> {
                    if (p != null)
                        projectListView.getItems().setAll(p);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showToast("Failed to load projects.", true));
                    return null;
                });
    }

    @FXML
    protected void showCreateProjectForm() {
        switchView(createProjectView, btnNavProj);
    }

    @FXML
    protected void onPostProjectClick() {
        String title = projectTitleInput.getText().trim();
        String goal = projectGoalInput.getText().trim();
        String resp = projectResponsibilitiesInput.getText().trim();
        LocalDate selectedDate = projectDueDateInput.getValue();
        String dueDate = selectedDate != null ? selectedDate.toString() : "Not Specified";
        List<String> roles = new ArrayList<>();
        for (Node n : projectRolePane.getChildren())
            if (((ToggleButton) n).isSelected())
                roles.add(((ToggleButton) n).getText());

        if (title.length() < 5) {
            showToast("Project title is too short (min 5 chars).", true);
            return;
        }
        if (!hasMeaningfulContent(title, 3)) {
            showToast("Project title must contain real words, not just symbols or spaces.", true);
            return;
        }
        if (roles.isEmpty()) {
            showToast("Select at least one required skill.", true);
            return;
        }
        if (roles.size() > 4) {
            showToast("Maximum 4 skills allowed.", true);
            return;
        }
        if (goal.length() < 15) {
            showToast("Goal description is too short (min 15 chars).", true);
            return;
        }
        if (!hasMeaningfulContent(goal, 8)) {
            showToast("Goal must contain meaningful text, not just symbols.", true);
            return;
        }
        if (resp.length() < 10) {
            showToast("Responsibilities too short (min 10 chars).", true);
            return;
        }
        if (!hasMeaningfulContent(resp, 5)) {
            showToast("Responsibilities must contain meaningful text.", true);
            return;
        }
        if (selectedDate != null && selectedDate.isBefore(LocalDate.now())) {
            showToast("Due date cannot be in the past! Please choose today or a future date.", true);
            projectDueDateInput.requestFocus();
            createProjectView.setDisable(false); // important: re-enable form
            return;
        }

        createProjectView.setDisable(true);
        if (!confirm("Publish Project", "Ready to go live? 🚀",
                "Post \"" + title + "\" so others can find and apply?")) {
            createProjectView.setDisable(false);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.addProject(currentUser, title, goal, String.join(", ", roles), resp, dueDate);
                Platform.runLater(() -> {
                    createProjectView.setDisable(false);
                    projectTitleInput.clear();
                    projectGoalInput.clear();
                    projectResponsibilitiesInput.clear();
                    projectDueDateInput.setValue(null);
                    projectRolePane.getChildren().forEach(n -> ((ToggleButton) n).setSelected(false));
                    refreshProjectList();
                    showToast("Project published! 🚀", false);
                    navToProjects();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    createProjectView.setDisable(false);
                    showToast("Failed to post project.", true);
                });
            }
        });
    }

    // ─── Inbox ────────────────────────────────────────────────────────────────

    @FXML
    protected void refreshInbox() {
        if (currentUser == null)
            return;
        CompletableFuture.supplyAsync(() -> {
            DatabaseManager.markInvitesAsRead(currentUser);
            return DatabaseManager.getInvitations(currentUser);
        })
                .thenAccept(invites -> Platform.runLater(() -> {
                    if (invites == null)
                        return;
                    teamInvitesListView.getItems().clear();
                    projectAppsListView.getItems().clear();
                    for (Invitation inv : invites)
                        (("PROJECT".equals(inv.getType())) ? projectAppsListView : teamInvitesListView).getItems()
                                .add(inv);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showToast("Failed to load inbox.", true));
                    return null;
                });
    }

    private void handleInviteAction(Invitation invite, String newStatus) {
        final String statusToStore = "DECLINED".equals(newStatus) ? "REJECTED" : newStatus;
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.updateInviteStatus(invite.getId(), statusToStore);
                if ("ACCEPTED".equals(statusToStore)) {
                    String msg = "PROJECT".equals(invite.getType())
                            ? "I accepted your application for: " + invite.getRelatedTitle() + "!"
                            : "I accepted your team invitation! Let's collaborate. \uD83E\uDD1D";
                    DatabaseManager.sendMessage(currentUser, invite.getSenderInfo(), msg);
                }
                Platform.runLater(() -> {
                    refreshInbox();
                    boolean accepted = "ACCEPTED".equals(statusToStore);
                    showToast(accepted
                            ? "✅ Accepted " + invite.getSenderInfo() + "'s request!"
                            : "❌ Declined " + invite.getSenderInfo() + "'s request.", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showToast("Failed to update invite. Check your connection.", true));
            }
        });
    }

    // ─── Chat ─────────────────────────────────────────────────────────────────

    @FXML
    void onSendChatMessage() {
        String selected = chatContactsList.getSelectionModel().getSelectedItem();
        if (selected != null)
            currentChatUser = selected;
        String text = chatMessageInput.getText().trim();
        if (currentChatUser.isEmpty() || text.isEmpty())
            return;

        // Block messaging in a locked project's group chat
        if (currentChatUser.startsWith("#")) {
            String teamTitle = currentChatUser.substring(1);
            if (DatabaseManager.isProjectLocked(teamTitle)) {
                Project lockedProj = DatabaseManager.getProjectByTitle(teamTitle);
                String reason = (lockedProj != null && lockedProj.isDone()) ? "completed" : "expired";
                showToast("🔒 This project is " + reason + " — chat is read-only.", true);
                chatMessageInput.clear();
                return;
            }
        }

        if (!hasMeaningfulContent(text)) {
            showToast("Message must contain real text.", true);
            return;
        }
        chatMessageInput.clear();
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.sendMessage(currentUser, currentChatUser, text);
                Platform.runLater(() -> refreshChatHistory(true));
            } catch (Exception e) {
                Platform.runLater(() -> showToast("Message failed to send. Try again.", true));
            }
        });
    }

    @FXML
    void onSendChatAttachment() {
        String selected = chatContactsList.getSelectionModel().getSelectedItem();
        if (selected != null)
            currentChatUser = selected;
        if (currentChatUser.isEmpty()) {
            showToast("Select a contact first.", true);
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Select File to Send");
        Scene chatScene = chatMessageInput.getScene();
        if (chatScene == null) {
            showToast("Cannot open file dialog. Try again.", true);
            return;
        }
        File file = fc.showOpenDialog(chatScene.getWindow());
        if (file == null)
            return;

        // ── Validate before uploading ─────────────────────────────────────
        final long MAX_BYTES = 25 * 1024 * 1024; // 25 MB cap
        if (file.length() == 0) {
            showToast("Cannot send an empty file.", true);
            return;
        }
        if (file.length() > MAX_BYTES) {
            showToast("File too large (max 25 MB). Please compress it first.", true);
            return;
        }
        String fname = file.getName().toLowerCase();
        // Block dangerous executable types
        String[] blocked = { ".exe", ".bat", ".cmd", ".sh", ".msi", ".ps1", ".vbs", ".jar" };
        for (String ext : blocked) {
            if (fname.endsWith(ext)) {
                showToast("Cannot send executable files for safety reasons.", true);
                return;
            }
        }

        showToast("Uploading attachment...", false);
        CompletableFuture.runAsync(() -> {
            try {
                File dest = new File(UPLOAD_DIR, System.currentTimeMillis() + "_" + file.getName());
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                DatabaseManager.sendMessage(currentUser, currentChatUser,
                        "[FILE]" + dest.getAbsolutePath() + "|" + file.getName());
                Platform.runLater(() -> {
                    refreshChatHistory(true);
                    showToast("Attachment sent! 📎", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showToast("Failed to send attachment.", true));
            }
        });
    }

    private void refreshChatContacts() {
        if (currentUser == null)
            return;
        CompletableFuture.supplyAsync(() -> {
            List<String> contacts = DatabaseManager.getChatContacts(currentUser);
            List<String> teams = DatabaseManager.getUserTeams(currentUser);
            for (String t : teams) {
                String tag = "#" + t;
                if (!contacts.contains(tag))
                    contacts.add(0, tag);
            }
            return contacts;
        }).thenAccept(contacts -> Platform.runLater(() -> {
            if (contacts == null)
                return;
            String selected = chatContactsList.getSelectionModel().getSelectedItem();
            chatContactsList.getItems().setAll(contacts);
            if (selected != null && contacts.contains(selected))
                chatContactsList.getSelectionModel().select(selected);
        }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showToast("Failed to load contacts.", true));
                    return null;
                });
    }

    private void refreshChatHistory(boolean scrollToBottom) {
        if (currentChatUser.isEmpty())
            return;
        final String target = currentChatUser;
        CompletableFuture.supplyAsync(() -> DatabaseManager.getChatHistory(currentUser, target))
                .thenAccept(history -> Platform.runLater(() -> {
                    if (!target.equals(currentChatUser) || history == null)
                        return;
                    boolean newContact = !target.equals(displayedChatUser);
                    int uiCount = chatListView.getItems().size();
                    int dbCount = history.size();
                    if (newContact) {
                        chatListView.getItems().setAll(history);
                        displayedChatUser = target;
                        if (!chatListView.getItems().isEmpty())
                            chatListView.scrollTo(chatListView.getItems().size() - 1);
                    } else if (dbCount > uiCount) {
                        chatListView.getItems().addAll(history.subList(uiCount, dbCount));
                        if (scrollToBottom || dbCount - uiCount < 3)
                            chatListView.scrollTo(chatListView.getItems().size() - 1);
                    } else if (dbCount < uiCount) {
                        chatListView.getItems().setAll(history);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showToast("Failed to load chat history. Try again.", true));
                    return null;
                });
    }

    // ─── Public Profile ───────────────────────────────────────────────────────

    private void showPublicProfile(String username) {
        previousView = java.util.Arrays
                .stream(new Node[] { projectView, searchView, inboxView, chatView, workspaceView, profileView,
                        homeView })
                .filter(Node::isVisible).findFirst().orElse(homeView);

        CompletableFuture.supplyAsync(() -> {
            Student s = DatabaseManager.getStudentProfile(username);
            boolean teamed = s != null && currentUser != null && !username.equals(currentUser)
                    && DatabaseManager.checkIsTeamedUp(currentUser, username);
            String lastSeen = DatabaseManager.getLastSeenStatus(username);
            return new Object[] { s, teamed, lastSeen };
        }).thenAccept(res -> Platform.runLater(() -> {
            Student student = (Student) res[0];
            boolean isTeamed = (boolean) res[1];
            String lastSeen = (String) res[2];
            if (student == null)
                return;
            publicProfileAvatarPane.getChildren().setAll(getAvatar(student.getName(), 72, 30));
            publicProfileNameLbl.setText(student.getName());
            publicProfileSkillsLbl.setText("Skills: " + student.getSkills());
            publicProfileBioLbl.setText(student.getBio());
            publicProfileActionBox.getChildren().clear();

            if (!student.getName().equals(currentUser)) {
                publicProfileLastSeenLbl.setText(lastSeen);
                publicProfileLastSeenLbl.setVisible(true);
                Button msgBtn = new Button("Message");
                msgBtn.getStyleClass().add("action-btn");
                msgBtn.setStyle("-fx-background-color: #3498db; -fx-pref-height: 34px; -fx-padding: 0 16;");
                msgBtn.setOnAction(e -> {
                    if (!chatContactsList.getItems().contains(student.getName()))
                        chatContactsList.getItems().add(student.getName());
                    chatContactsList.getSelectionModel().select(student.getName());
                    navToChat();
                });
                Button actionBtn = new Button(isTeamed ? "Unpair Partner" : "Invite to Team");
                actionBtn.getStyleClass().add("action-btn");
                actionBtn.setStyle(isTeamed ? "-fx-background-color: #e74c3c; -fx-pref-height: 34px; -fx-padding: 0 16;"
                        : "-fx-background-color: #d35400; -fx-pref-height: 34px; -fx-padding: 0 16;");
                actionBtn.setOnAction(e -> {
                    if (isTeamed)
                        handleUnpair(student.getName());
                    else
                        handleSendTeamInvite(student.getName());
                });
                publicProfileActionBox.getChildren().addAll(msgBtn, actionBtn);
            } else {
                publicProfileLastSeenLbl.setVisible(false);
            }

            CompletableFuture.supplyAsync(() -> DatabaseManager.getPortfolioItems(student.getName()))
                    .thenAccept(items -> Platform.runLater(() -> publicPortfolioListView.getItems().setAll(items)));
            switchView(publicProfileView, null);
        }));
    }

    @FXML
    void onBackFromProfile() {
        if (previousView == null) {
            navToHome();
            return;
        }
        Button btn = null;
        if (previousView == projectView)
            btn = btnNavProj;
        else if (previousView == searchView)
            btn = btnNavSearch;
        else if (previousView == inboxView)
            btn = btnNavInbox;
        else if (previousView == chatView)
            btn = btnNavChat;
        else if (previousView == workspaceView)
            btn = btnNavWorkspace;
        else if (previousView == profileView)
            btn = btnNavProfile;
        else if (previousView == homeView)
            btn = btnNavHome;
        switchView(previousView, btn);
    }

    private void handleSendTeamInvite(String targetName) {
        String input = modalTextInput("Send Squad Invite", "Inviting " + targetName + " — add a message:",
                "Yo! Let's team up and build something awesome. \uD83D\uDE80");
        if (input == null)
            return;
        String trimmed = input.trim();
        if (!trimmed.isEmpty() && !hasMeaningfulContent(trimmed, 2)) {
            showToast("Invite message must contain real words.", true);
            return;
        }
        String msg = trimmed.isEmpty() ? "Hey, I want to add you to my squad!" : trimmed;
        CompletableFuture.runAsync(() -> {
            try {
                boolean ok = DatabaseManager.sendInvite(targetName, currentUser, msg, "TEAM", "");
                Platform.runLater(() -> showToast(
                        ok ? "Invite sent to " + targetName + "! 🎯" : "Invite already active.", !ok));
            } catch (Exception e) {
                Platform.runLater(() -> showToast("Failed to send invite. Check your connection.", true));
            }
        });
    }

    private void handleJoinRequest(Project project) {
        String input = modalTextInput("Apply to Project", "Applying for: " + project.getTitle() + "\nPitch yourself:",
                "Looks like a great project. I've got the skills you need! \uD83D\uDD25");
        if (input == null)
            return;
        String trimmed = input.trim();
        if (!trimmed.isEmpty() && !hasMeaningfulContent(trimmed, 2)) {
            showToast("Your pitch must contain real words.", true);
            return;
        }
        String msg = trimmed.isEmpty() ? "I'd love to join this project!" : trimmed;
        CompletableFuture.runAsync(() -> {
            try {
                boolean ok = DatabaseManager.sendInvite(project.getOwnerName(), currentUser, msg, "PROJECT",
                        project.getTitle());
                Platform.runLater(() -> showToast(ok ? "Application sent! 🤞" : "Already applied. 🛑", !ok));
            } catch (Exception e) {
                Platform.runLater(() -> showToast("Failed to send application. Check your connection.", true));
            }
        });
    }

    private void handleUnpair(String targetName) {
        if (confirm("Confirm Unpair", null, "Unpair with " + targetName + "? You will no longer be a team.")) {
            CompletableFuture.runAsync(() -> {
                try {
                    DatabaseManager.unpairUsers(currentUser, targetName);
                    DatabaseManager.sendMessage(currentUser, targetName, "I have ended our team partnership.");
                    Platform.runLater(() -> {
                        showToast("Successfully unpaired.", false);
                        showPublicProfile(targetName);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showToast("Failed to unpair. Check your connection.", true));
                }
            });
        }
    }

    // ─── Profile ──────────────────────────────────────────────────────────────

    @FXML
    void onUpdateProfileClick() {
        List<String> skills = new ArrayList<>();
        for (Node n : profileSkillsPane.getChildren())
            if (((ToggleButton) n).isSelected())
                skills.add(((ToggleButton) n).getText());
        if (skills.isEmpty()) {
            showToast("Select at least one skill.", true);
            return;
        }
        String bio = profileBioInput.getText().trim();
        if (!bio.isEmpty() && !hasMeaningfulContent(bio, 3)) {
            showToast("Bio must contain real words, not just symbols.", true);
            return;
        }
        if (!confirm("Update Profile", null, "Save these changes to your profile?"))
            return;
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.updateProfile(currentUser, String.join(", ", skills), bio);
                Platform.runLater(() -> showToast("Profile updated! ✅", false));
            } catch (Exception e) {
                Platform.runLater(() -> showToast("Failed to save profile. Try again.", true));
            }
        });
    }

    @FXML
    void onUploadPortfolioClick() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Portfolio File");
        Scene win = mainDashboard.getScene();
        if (win == null) {
            showToast("Cannot open file dialog. Try again.", true);
            return;
        }
        File file = fc.showOpenDialog(win.getWindow());
        if (file == null)
            return;

        // ── Validate before uploading ─────────────────────────────────────────
        final long MAX_BYTES = 25 * 1024 * 1024; // 25 MB cap
        if (file.length() == 0) {
            showToast("Cannot upload an empty file.", true);
            return;
        }
        if (file.length() > MAX_BYTES) {
            showToast("File too large (max 25 MB). Please compress it first.", true);
            return;
        }
        String fname = file.getName().toLowerCase();
        String[] blocked = { ".exe", ".bat", ".cmd", ".sh", ".msi", ".ps1", ".vbs", ".jar" };
        for (String ext : blocked) {
            if (fname.endsWith(ext)) {
                showToast("Cannot upload executable files for safety reasons.", true);
                return;
            }
        }

        CompletableFuture.runAsync(() -> {
            try {
                File dest = new File(UPLOAD_DIR, System.currentTimeMillis() + "_" + file.getName());
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                DatabaseManager.addPortfolioItem(currentUser, file.getName(), dest.getAbsolutePath());
                Platform.runLater(() -> {
                    refreshMyPortfolio();
                    showToast("File uploaded! 📁", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showToast("Upload failed. Check your storage and try again.", true));
            }
        });
    }

    private void refreshMyPortfolio() {
        if (currentUser == null)
            return;
        CompletableFuture.supplyAsync(() -> DatabaseManager.getPortfolioItems(currentUser))
                .thenAccept(items -> Platform.runLater(() -> {
                    if (items != null)
                        myPortfolioListView.getItems().setAll(items);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showToast("Failed to load portfolio.", true));
                    return null;
                });
    }

    @FXML
    void onDeleteAccountClick() {
        if (confirm("Delete Account", "⚠️ This is permanent.",
                "This will wipe your profile, portfolio, messages, and all data. Continue?")) {
            CompletableFuture.runAsync(() -> {
                try {
                    DatabaseManager.deleteAccount(currentUser);
                    Platform.runLater(() -> {
                        showToast("Account deleted.", false);
                        forceLogoutCleanup();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showToast("Failed to delete account.", true));
                }
            });
        }
    }

    // ─── Workspace ────────────────────────────────────────────────────────────

    @FXML
    void onWorkspaceTeamChanged() {
        refreshWorkspaceTasks();
    }

    @FXML
    void onAddTask() {
        String teamId = workspaceTeamSelector.getValue();
        String title = workspaceNewTaskInput.getText().trim();
        if (teamId == null || teamId.isEmpty()) {
            showToast("Select a team first!", true);
            return;
        }
        if (title.isEmpty()) {
            showToast("Enter a task title.", true);
            return;
        }
        if (title.length() < 3) {
            showToast("Task title is too short (min 3 chars).", true);
            return;
        }
        if (!hasMeaningfulContent(title, 2)) {
            showToast("Task title must contain real words.", true);
            return;
        }
        String due = workspaceTaskDeadline.getValue() != null ? workspaceTaskDeadline.getValue().toString() : "None";

        // Block task creation on expired projects (flag set by refreshWorkspaceTasks)
        if (currentProjectExpired) {
            showToast("This project has expired. No new tasks can be added.", true);
            return;
        }

        workspaceNewTaskInput.clear();
        workspaceTaskDeadline.setValue(null);
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.addTask(teamId, currentUser, title, due);
                Platform.runLater(this::refreshWorkspaceTasks);
            } catch (Exception e) {
                Platform.runLater(() -> showToast("Failed to add task.", true));
            }
        });
    }

    private void refreshWorkspaceTasks() {
        String teamId = workspaceTeamSelector.getValue();
        if (teamId == null || teamId.isEmpty()) {
            currentProjectExpired = false;
            pendingTaskList.getItems().clear();
            progressTaskList.getItems().clear();
            doneTaskList.getItems().clear();
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            List<Task> tasks = DatabaseManager.getTasks(teamId);
            double pulse = (tasks != null && !tasks.isEmpty()) ? DatabaseManager.getProjectPulse(teamId) : 0.0;
            // Determine whether this project is locked (DONE or EXPIRED via status field)
            boolean expired = false;
            Project project = DatabaseManager.getProjectByTitle(teamId);
            if (project != null) {
                expired = project.isDone() || project.isExpired();
            }
            return new Object[] { tasks, pulse, expired };
        }).thenAccept(result -> Platform.runLater(() -> {
            @SuppressWarnings("unchecked")
            List<Task> tasks = (List<Task>) result[0];
            double pulse = (double) result[1];
            currentProjectExpired = (boolean) result[2];
            if (tasks == null)
                return;
            if (workspacePulseBar != null)
                workspacePulseBar.setProgress(pulse / 100.0);
            if (pulseLabel != null)
                pulseLabel.setText(String.format("%.0f%% Team Momentum", pulse));
            pendingTaskList.getItems().clear();
            progressTaskList.getItems().clear();
            doneTaskList.getItems().clear();
            for (Task t : tasks) {
                String s = t.getStatus() == null ? "PENDING" : t.getStatus();
                if ("IN_PROGRESS".equalsIgnoreCase(s))
                    progressTaskList.getItems().add(t);
                else if ("DONE".equalsIgnoreCase(s))
                    doneTaskList.getItems().add(t);
                else
                    pendingTaskList.getItems().add(t);
            }
            // If the project is locked, show a banner and lock the add-task input
            if (workspaceNewTaskInput != null) {
                workspaceNewTaskInput.setDisable(currentProjectExpired);
                String lockPrompt = "What needs to get done?";
                if (currentProjectExpired) {
                    Project proj = DatabaseManager.getProjectByTitle(teamId);
                    lockPrompt = (proj != null && proj.isDone())
                            ? "✅ Project completed — task board is read-only"
                            : "⏰ Project expired — no new tasks allowed";
                }
                workspaceNewTaskInput.setPromptText(lockPrompt);
            }
            if (workspaceTaskDeadline != null)
                workspaceTaskDeadline.setDisable(currentProjectExpired);
        })).exceptionally(ex -> {
            Platform.runLater(() -> showToast("Failed to load tasks.", true));
            return null;
        });
    }

    @FXML
    void showTeamMembersDialog() {
        String teamId = workspaceTeamSelector.getValue();
        if (teamId == null || teamId.isEmpty()) {
            showToast("Select a squad first!", true);
            return;
        }

        final String bg = isDarkMode ? "#0F0C1B" : "#FAFAFA";
        final String textMain = isDarkMode ? "#F0EEF8" : "#110F18";
        final String textSub = isDarkMode ? "#A39DBE" : "#6C6687";

        VBox card = buildModalCard(500);
        card.getChildren().add(modalTitle("👥 Squad Members — " + teamId));

        VBox content = new VBox(8);
        content.setStyle("-fx-padding: 2 0 4 0; -fx-background-color: " + bg + ";");

        Label loading = new Label("Loading members...");
        loading.setStyle("-fx-text-fill: " + textSub + "; -fx-font-style: italic; -fx-font-size: 13px;");
        content.getChildren().add(loading);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(340);
        scroll.setStyle("-fx-background: " + bg + "; -fx-background-color: " + bg
                + "; -fx-border-color: transparent;");
        content.heightProperty().addListener((obs, oldH, newH) -> {
            double h = newH.doubleValue() + 16;
            scroll.setPrefHeight(Math.min(h, 340));
        });
        card.getChildren().add(scroll);

        HBox buttons = modalButtonRow();
        Button closeBtn = modalPrimaryButton("Close");
        closeBtn.setOnAction(e -> dismissModal(card));
        buttons.getChildren().add(closeBtn);
        card.getChildren().add(buttons);

        modalOverlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                dismissModal(card);
        });

        // Show immediately, then populate async
        StackPane wrapper = new StackPane(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setStyle("-fx-padding: 40 0;");
        StackPane.setAlignment(card, Pos.CENTER);
        modalOverlay.getChildren().setAll(wrapper);
        modalOverlay.setVisible(true);
        modalOverlay.requestFocus();
        card.setOpacity(0);
        card.setTranslateY(18);
        FadeTransition ftIn = new FadeTransition(Duration.millis(220), card);
        ftIn.setToValue(1.0);
        TranslateTransition ttIn = new TranslateTransition(Duration.millis(220), card);
        ttIn.setToY(0);
        ttIn.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ftIn, ttIn).play();

        CompletableFuture.supplyAsync(() -> {
            List<String> members = DatabaseManager.getTeamMembers(teamId);
            Project project = DatabaseManager.getProjectByTitle(teamId);
            String owner = (project != null) ? project.getOwnerName() : null;
            return new Object[] { members, owner };
        })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        content.getChildren().clear();
                        Label err = new Label("⚠️ Failed to load members. Try again.");
                        err.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
                        content.getChildren().add(err);
                    });
                    return new Object[] { java.util.Collections.emptyList(), null };
                })
                .thenAccept(result -> Platform.runLater(() -> {
                    @SuppressWarnings("unchecked")
                    List<String> members = (List<String>) result[0];
                    String projectOwner = (String) result[1];
                    boolean canKick = currentUser != null && currentUser.equals(projectOwner);

                    content.getChildren().clear();
                    if (members.isEmpty()) {
                        Label empty = new Label("No members in this squad yet.");
                        empty.setStyle("-fx-text-fill: " + textSub + "; -fx-font-style: italic; -fx-font-size: 13px;");
                        content.getChildren().add(empty);
                    } else {
                        Label countLbl = new Label(members.size() + " member" + (members.size() > 1 ? "s" : ""));
                        countLbl.setStyle(
                                "-fx-text-fill: #8A2BE2; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0 0 2 2;");
                        content.getChildren().add(countLbl);
                        for (String member : members) {
                            HBox row = new HBox(12);
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.setStyle(
                                    "-fx-background-color: rgba(138,43,226,0.08); -fx-background-radius: 12; -fx-padding: 8 14; -fx-cursor: hand;");
                            row.setOnMouseEntered(e -> row.setStyle(
                                    "-fx-background-color: rgba(138,43,226,0.18); -fx-background-radius: 12; -fx-padding: 8 14; -fx-cursor: hand;"));
                            row.setOnMouseExited(e -> row.setStyle(
                                    "-fx-background-color: rgba(138,43,226,0.08); -fx-background-radius: 12; -fx-padding: 8 14; -fx-cursor: hand;"));
                            Label name = new Label(member);
                            name.setStyle(
                                    "-fx-text-fill: " + textMain + "; -fx-font-weight: bold; -fx-font-size: 14px;");
                            // Owner crown badge
                            if (member.equals(projectOwner)) {
                                Label ownerBadge = new Label("👑 Owner");
                                ownerBadge.setStyle(
                                        "-fx-background-color: rgba(255,193,7,0.18); -fx-text-fill: #f59e0b;"
                                                + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 7;"
                                                + "-fx-background-radius: 4;");
                                row.getChildren().addAll(getAvatar(member, 36, 14), name, ownerBadge);
                            } else {
                                row.getChildren().addAll(getAvatar(member, 36, 14), name);
                            }
                            Pane spacer = new Pane();
                            HBox.setHgrow(spacer, Priority.ALWAYS);
                            boolean isOnline = onlineUsersCache.contains(member) || member.equals(currentUser);
                            Label statusLbl = new Label(isOnline ? "🟢 Online" : "⚫ Offline");
                            statusLbl.setStyle(
                                    "-fx-font-size: 11px; -fx-text-fill: " + (isOnline ? "#10b981" : textSub) + ";");
                            Button viewBtn = new Button("View");
                            viewBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #8A2BE2;"
                                    + " -fx-text-fill: #8A2BE2; -fx-font-size: 11px; -fx-padding: 3 10;"
                                    + " -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
                            viewBtn.setOnMouseEntered(e -> viewBtn
                                    .setStyle("-fx-background-color: rgba(138,43,226,0.18); -fx-border-color: #8A2BE2;"
                                            + " -fx-text-fill: #8A2BE2; -fx-font-size: 11px; -fx-padding: 3 10;"
                                            + " -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));
                            viewBtn.setOnMouseExited(e -> viewBtn
                                    .setStyle("-fx-background-color: transparent; -fx-border-color: #8A2BE2;"
                                            + " -fx-text-fill: #8A2BE2; -fx-font-size: 11px; -fx-padding: 3 10;"
                                            + " -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));
                            viewBtn.setOnAction(e -> {
                                dismissModal(card);
                                Platform.runLater(() -> showPublicProfile(member));
                            });
                            row.getChildren().addAll(spacer, statusLbl, viewBtn);

                            // ── Kick button: only for owners, only for non-owner members ──
                            if (canKick && !member.equals(currentUser)) {
                                Button kickBtn = new Button("Kick 🦵");
                                kickBtn.setStyle(
                                        "-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5;"
                                                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 10;"
                                                + "-fx-background-radius: 6; -fx-cursor: hand;");
                                kickBtn.setOnMouseEntered(e -> kickBtn.setStyle(
                                        "-fx-background-color: #ef4444; -fx-text-fill: white;"
                                                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 10;"
                                                + "-fx-background-radius: 6; -fx-cursor: hand;"));
                                kickBtn.setOnMouseExited(e -> kickBtn.setStyle(
                                        "-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5;"
                                                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 10;"
                                                + "-fx-background-radius: 6; -fx-cursor: hand;"));
                                kickBtn.setOnAction(e -> {
                                    // confirm() replaces the modal overlay; reopen after regardless of choice
                                    boolean confirmed = HelloController.this.confirm("Kick Member",
                                            "⚠️ This cannot be undone.",
                                            "Remove " + member + " from \"" + teamId + "\"? They will be notified.");
                                    if (confirmed) {
                                        CompletableFuture.runAsync(() -> {
                                            try {
                                                DatabaseManager.kickTeamMember(teamId, member);
                                                DatabaseManager.sendMessage(currentUser, member,
                                                        "You have been removed from the squad: " + teamId + ".");
                                                Platform.runLater(() -> {
                                                    showToast("🦵 " + member + " was removed from the squad.", false);
                                                    showTeamMembersDialog(); // reopen with updated list
                                                });
                                            } catch (Exception ex) {
                                                Platform.runLater(() -> {
                                                    showToast("Failed to kick member. Try again.", true);
                                                    showTeamMembersDialog(); // reopen even on failure
                                                });
                                            }
                                        });
                                    } else {
                                        // Cancelled — confirm() cleared the overlay, reopen this dialog
                                        Platform.runLater(HelloController.this::showTeamMembersDialog);
                                    }
                                });
                                row.getChildren().add(kickBtn);
                            }

                            content.getChildren().add(row);
                        }
                    }
                }));
    }

    private void changeTaskStatus(int taskId, String newStatus) {
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager.updateTaskStatus(taskId, newStatus);
                Platform.runLater(() -> {
                    refreshWorkspaceTasks();
                    showToast("DONE".equals(newStatus) ? "Task marked as done! ✅" : "Task status updated.", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showToast("Failed to update task status. Try again.", true));
            }
        });
    }

    private void handleClaimTask(Task task) {
        if (currentProjectExpired) {
            showToast("This project has expired. Tasks can no longer be claimed.", true);
            return;
        }
        CompletableFuture.runAsync(() -> {
            if (DatabaseManager.claimTask(task.getId(), currentUser))
                Platform.runLater(() -> {
                    showToast("Dibs called! Time to cook. 👨‍🍳", false);
                    refreshWorkspaceTasks();
                });
            else
                Platform.runLater(() -> showToast("Task already claimed.", true));
        }).exceptionally(ex -> {
            Platform.runLater(() -> showToast("Failed to claim task. Try again.", true));
            return null;
        });
    }

    @FXML
    void onExportTasks() {
        String teamId = workspaceTeamSelector.getValue();
        if (teamId == null || teamId.isEmpty()) {
            showToast("Select a team first.", true);
            return;
        }
        CompletableFuture.supplyAsync(() -> DatabaseManager.getTasks(teamId))
                .thenAccept(tasks -> Platform.runLater(() -> {
                    try {
                        File desktop = new File(System.getProperty("user.home") + File.separator + "Desktop");
                        // Fall back to home directory on systems without a Desktop folder
                        File exportDir = (desktop.exists() && desktop.isDirectory()) ? desktop
                                : new File(System.getProperty("user.home"));
                        File out = new File(exportDir, teamId + "_Tasks.txt");
                        try (java.io.PrintWriter w = new java.io.PrintWriter(out)) {
                            w.println("=== SKILLSYNC SQUAD BOARD EXPORT ===");
                            w.println("Team: " + teamId);
                            w.println("Date: " + LocalDate.now());
                            w.println("---");
                            for (Task t : tasks)
                                w.println(String.format("[%s] %s | @%s | Due: %s", t.getStatus(), t.getTitle(),
                                        t.getAssignedTo(), t.getDueDate()));
                        }
                        showToast("Exported to Desktop! 📄", false);
                    } catch (Exception e) {
                        showToast("Export failed.", true);
                    }
                }));
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    @FXML
    private void toggleTheme() {
        isDarkMode = themeToggleItem.isSelected();
        applyTheme();
        prefPutBoolean("darkMode", isDarkMode);
    }

    private void applyTheme() {
        Scene scene = mainDashboard.getScene();
        if (scene == null)
            return;
        if (isDarkMode) {
            scene.getRoot().getStyleClass().remove("light-theme");
            themeToggleItem.setSelected(true);
            settingsIcon.setIconColor(javafx.scene.paint.Color.web("#94a3b8"));
        } else {
            if (!scene.getRoot().getStyleClass().contains("light-theme"))
                scene.getRoot().getStyleClass().add("light-theme");
            themeToggleItem.setSelected(false);
            settingsIcon.setIconColor(javafx.scene.paint.Color.web("#6C6687"));
        }
    }

    // ─── Toast Notifications ─────────────────────────────────────────────────

    private void showToast(String message, boolean isError) {
        Platform.runLater(() -> {
            if (!isError && prefGetBoolean("ping_mute_toasts", false))
                return;
            Label toast = new Label(message);
            toast.setStyle("-fx-background-color: " + (isError ? "#c0392b" : "#0f9b6a")
                    + "; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 22; -fx-font-weight: bold; -fx-font-size: 13px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            toast.setOpacity(0);
            FontIcon icon = new FontIcon(isError ? "fas-exclamation-circle" : "fas-check-circle");
            icon.setIconColor(javafx.scene.paint.Color.WHITE);
            icon.setIconSize(14);
            toast.setGraphic(icon);
            toastContainer.getChildren().add(toast);

            TranslateTransition ttIn = new TranslateTransition(Duration.millis(280), toast);
            ttIn.setFromY(20);
            ttIn.setToY(0);
            FadeTransition ftIn = new FadeTransition(Duration.millis(280), toast);
            ftIn.setToValue(1.0);
            TranslateTransition ttOut = new TranslateTransition(Duration.millis(280), toast);
            ttOut.setToY(15);
            FadeTransition ftOut = new FadeTransition(Duration.millis(280), toast);
            ftOut.setToValue(0);
            ftOut.setOnFinished(e -> toastContainer.getChildren().remove(toast));

            new SequentialTransition(new ParallelTransition(ttIn, ftIn), new PauseTransition(Duration.seconds(2.8)),
                    new ParallelTransition(ttOut, ftOut)).play();
        });
    }

    // ─── Settings Dialogs ─────────────────────────────────────────────────────

    @FXML
    void showGhostModeSettings() {
        VBox card = buildModalCard(400);
        card.getChildren().add(modalTitle("Ghost Mode 👻"));

        // Section header
        Label sectionIcon = new Label("🥷");
        sectionIcon.setStyle("-fx-font-size: 18px;");
        Label sectionLbl = new Label("Privacy Controls");
        sectionLbl.setStyle("-fx-text-fill: #8A2BE2; -fx-font-weight: 900; -fx-font-size: 14px;");
        HBox sectionRow = new HBox(8, sectionIcon, sectionLbl);
        sectionRow.setAlignment(Pos.CENTER_LEFT);
        sectionRow.setStyle("-fx-background-color: rgba(138,43,226,0.10);"
                + "-fx-background-radius: 10; -fx-padding: 10 14;"
                + "-fx-border-color: rgba(138,43,226,0.28); -fx-border-radius: 10; -fx-border-width: 1;");
        card.getChildren().add(sectionRow);

        CheckBox hideOnline = styledCheckBox("Ninja Mode (Appear Offline)",
                prefGetBoolean("ghost_hide_online", false));
        CheckBox hideLastSeen = styledCheckBox("Hide 'Last Seen' Timestamp",
                prefGetBoolean("ghost_hide_seen", false));

        String hintStyle = "-fx-text-fill: " + (isDarkMode ? "#A39DBE" : "#6C6687")
                + "; -fx-font-size: 11.5px; -fx-font-style: italic; -fx-padding: 0 0 0 4;";
        Label hintOnline = new Label("Ninja Mode hides your online status from other users.");
        hintOnline.setStyle(hintStyle);
        hintOnline.setWrapText(true);
        Label hintSeen = new Label("Hide Last Seen removes your activity timestamp from your public profile.");
        hintSeen.setStyle(hintStyle);
        hintSeen.setWrapText(true);

        card.getChildren().addAll(new VBox(3, hideOnline, hintOnline), new VBox(3, hideLastSeen, hintSeen));

        HBox buttons = modalButtonRow();
        Button okBtn = modalPrimaryButton("Save");
        Button cancelBtn = modalSecondaryButton("Cancel");
        okBtn.setOnAction(e -> {
            prefPutBoolean("ghost_hide_online", hideOnline.isSelected());
            prefPutBoolean("ghost_hide_seen", hideLastSeen.isSelected());
            dismissModal(card);
            showToast("Ghost Mode updated! 🥷", false);
            if (hideOnline.isSelected())
                CompletableFuture.runAsync(() -> DatabaseManager.forceOffline(currentUser));
            else
                CompletableFuture.runAsync(() -> DatabaseManager.sendHeartbeat(currentUser));
        });
        cancelBtn.setOnAction(e -> dismissModal(card));
        modalOverlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                dismissModal(card);
        });
        buttons.getChildren().addAll(cancelBtn, okBtn);
        card.getChildren().add(buttons);
        showModal(card);
    }

    @FXML
    void showPingPreferences() {
        VBox card = buildModalCard(400);
        card.getChildren().add(modalTitle("Ping Preferences 🔔"));

        Label sectionIcon = new Label("🔕");
        sectionIcon.setStyle("-fx-font-size: 18px;");
        Label sectionLbl = new Label("Notification Controls");
        sectionLbl.setStyle("-fx-text-fill: #00E5FF; -fx-font-weight: 900; -fx-font-size: 14px;");
        HBox sectionRow = new HBox(8, sectionIcon, sectionLbl);
        sectionRow.setAlignment(Pos.CENTER_LEFT);
        sectionRow.setStyle("-fx-background-color: rgba(0,229,255,0.08);"
                + "-fx-background-radius: 10; -fx-padding: 10 14;"
                + "-fx-border-color: rgba(0,229,255,0.25); -fx-border-radius: 10; -fx-border-width: 1;");
        card.getChildren().add(sectionRow);

        CheckBox muteToasts = styledCheckBox("Mute all success toasts", prefGetBoolean("ping_mute_toasts", false));
        CheckBox muteChat = styledCheckBox("Mute incoming message alerts", prefGetBoolean("ping_mute_chat", false));
        Label hint = new Label("Error notifications are always shown, regardless of these settings.");
        hint.setStyle("-fx-text-fill: " + (isDarkMode ? "#A39DBE" : "#6C6687")
                + "; -fx-font-size: 11.5px; -fx-font-style: italic; -fx-padding: 4 0 0 4;");
        hint.setWrapText(true);
        card.getChildren().addAll(muteToasts, muteChat, hint);

        HBox buttons = modalButtonRow();
        Button okBtn = modalPrimaryButton("Save");
        Button cancelBtn = modalSecondaryButton("Cancel");
        okBtn.setOnAction(e -> {
            prefPutBoolean("ping_mute_toasts", muteToasts.isSelected());
            prefPutBoolean("ping_mute_chat", muteChat.isSelected());
            dismissModal(card);
            showToast("Ping preferences saved! 🤫", false);
        });
        cancelBtn.setOnAction(e -> dismissModal(card));
        modalOverlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                dismissModal(card);
        });
        buttons.getChildren().addAll(cancelBtn, okBtn);
        card.getChildren().add(buttons);
        showModal(card);
    }

    @FXML
    void showChangePasswordDialog() {
        // ── Scrollable inner content ──────────────────────────────────────────
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 4 2 4 2;");

        Label title = modalTitle("Change Password 🔒");
        content.getChildren().add(title);

        String inputStyle = dialogInputStyle();

        // ── Old password row ─────────────────────────────────────────────────
        PasswordField oldPassField = new PasswordField();
        oldPassField.setPromptText("Current password");
        oldPassField.setStyle(inputStyle);
        oldPassField.setMaxWidth(Double.MAX_VALUE);

        TextField oldPassVisible = new TextField();
        oldPassVisible.setPromptText("Current password");
        oldPassVisible.setStyle(inputStyle);
        oldPassVisible.setMaxWidth(Double.MAX_VALUE);
        oldPassVisible.setVisible(false);
        oldPassVisible.setManaged(false);

        Button eyeOld = eyeToggleBtn();
        boolean[] oldVisible = { false };
        eyeOld.setOnAction(e -> {
            oldVisible[0] = !oldVisible[0];
            if (oldVisible[0]) {
                oldPassVisible.setText(oldPassField.getText());
                oldPassVisible.setVisible(true);
                oldPassVisible.setManaged(true);
                oldPassField.setVisible(false);
                oldPassField.setManaged(false);
                eyeOld.setText("🙈");
            } else {
                oldPassField.setText(oldPassVisible.getText());
                oldPassField.setVisible(true);
                oldPassField.setManaged(true);
                oldPassVisible.setVisible(false);
                oldPassVisible.setManaged(false);
                eyeOld.setText("👁");
            }
        });
        StackPane oldRow = passwordRow(oldPassField, oldPassVisible, eyeOld);

        // ── New password row ──────────────────────────────────────────────────
        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Min 8 chars, 1 uppercase, 1 digit");
        newPassField.setStyle(inputStyle);
        newPassField.setMaxWidth(Double.MAX_VALUE);

        TextField newPassVisible = new TextField();
        newPassVisible.setPromptText("Min 8 chars, 1 uppercase, 1 digit");
        newPassVisible.setStyle(inputStyle);
        newPassVisible.setMaxWidth(Double.MAX_VALUE);
        newPassVisible.setVisible(false);
        newPassVisible.setManaged(false);

        Button eyeNew = eyeToggleBtn();
        boolean[] newVisible = { false };
        eyeNew.setOnAction(e -> {
            newVisible[0] = !newVisible[0];
            if (newVisible[0]) {
                newPassVisible.setText(newPassField.getText());
                newPassVisible.setVisible(true);
                newPassVisible.setManaged(true);
                newPassField.setVisible(false);
                newPassField.setManaged(false);
                eyeNew.setText("🙈");
            } else {
                newPassField.setText(newPassVisible.getText());
                newPassField.setVisible(true);
                newPassField.setManaged(true);
                newPassVisible.setVisible(false);
                newPassVisible.setManaged(false);
                eyeNew.setText("👁");
            }
        });
        StackPane newRow = passwordRow(newPassField, newPassVisible, eyeNew);

        // ── Confirm password row ──────────────────────────────────────────────
        PasswordField confPassField = new PasswordField();
        confPassField.setPromptText("Re-enter new password");
        confPassField.setStyle(inputStyle);
        confPassField.setMaxWidth(Double.MAX_VALUE);

        TextField confPassVisible = new TextField();
        confPassVisible.setPromptText("Re-enter new password");
        confPassVisible.setStyle(inputStyle);
        confPassVisible.setMaxWidth(Double.MAX_VALUE);
        confPassVisible.setVisible(false);
        confPassVisible.setManaged(false);

        Button eyeConf = eyeToggleBtn();
        boolean[] confVisible = { false };
        eyeConf.setOnAction(e -> {
            confVisible[0] = !confVisible[0];
            if (confVisible[0]) {
                confPassVisible.setText(confPassField.getText());
                confPassVisible.setVisible(true);
                confPassVisible.setManaged(true);
                confPassField.setVisible(false);
                confPassField.setManaged(false);
                eyeConf.setText("🙈");
            } else {
                confPassField.setText(confPassVisible.getText());
                confPassField.setVisible(true);
                confPassField.setManaged(true);
                confPassVisible.setVisible(false);
                confPassVisible.setManaged(false);
                eyeConf.setText("👁");
            }
        });
        StackPane confRow = passwordRow(confPassField, confPassVisible, eyeConf);

        content.getChildren().addAll(
                modalSubLabel("Current Password:"), oldRow,
                new Separator(),
                modalSubLabel("New Password:"), newRow,
                modalSubLabel("Confirm New Password:"), confRow);

        // ── OTP section (hidden until password validated) ─────────────────────
        VBox otpSection = new VBox(8);
        otpSection.setVisible(false);
        otpSection.setManaged(false);

        Separator otpSep = new Separator();
        Label otpSectionTitle = new Label("Step 2 — Verify It's You 🔐");
        otpSectionTitle.setStyle("-fx-text-fill: #00E5FF; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label otpCodeDisplay = new Label("------");
        otpCodeDisplay.setStyle(
                "-fx-font-size: 30px; -fx-font-weight: bold; -fx-letter-spacing: 8px;"
                        + "-fx-text-fill: #00E5FF; -fx-font-family: monospace;");
        otpCodeDisplay.setMaxWidth(Double.MAX_VALUE);
        otpCodeDisplay.setAlignment(Pos.CENTER);

        VBox otpBox = new VBox(4);
        Label otpHint = new Label("Your confirmation code:");
        otpHint.setStyle("-fx-text-fill: " + (isDarkMode ? "#A39DBE" : "#6C6687") + "; -fx-font-size: 11px;");
        otpBox.setAlignment(Pos.CENTER);
        otpBox.setStyle("-fx-background-color: " + (isDarkMode ? "rgba(0,229,255,0.07)" : "rgba(0,180,200,0.07)")
                + "; -fx-background-radius: 10; -fx-border-color: rgba(0,229,255,0.3);"
                + "-fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 10 20;");
        otpBox.getChildren().addAll(otpHint, otpCodeDisplay);

        TextField otpField = new TextField();
        otpField.setPromptText("Enter 6-digit code");
        otpField.setStyle(inputStyle);
        otpField.setMaxWidth(Double.MAX_VALUE);
        otpField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d{0,6}"))
                return change;
            return null;
        }));

        otpSection.getChildren().addAll(otpSep, otpSectionTitle, otpBox,
                modalSubLabel("Enter Code:"), otpField);
        content.getChildren().add(otpSection);

        // ── Scroll pane wrapper ───────────────────────────────────────────────
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(480);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + "-fx-border-color: transparent;");
        content.setStyle("-fx-padding: 4 14 4 2;");

        VBox card = new VBox(12);
        card.setStyle(buildModalCard(460).getStyle());
        card.setPrefWidth(460);
        card.setMaxWidth(460);
        card.setStyle("-fx-background-color: " + (isDarkMode ? "#1A1730" : "#F4F2FF")
                + "; -fx-background-radius: 18; -fx-border-color: rgba(138,43,226,0.45);"
                + "-fx-border-radius: 18; -fx-border-width: 1.5; -fx-padding: 24 28 20 28;");
        card.getChildren().add(scroll);

        // ── Buttons ───────────────────────────────────────────────────────────
        HBox buttons = modalButtonRow();
        Button okBtn = modalPrimaryButton("Next →");
        Button cancelBtn = modalSecondaryButton("Cancel");
        buttons.getChildren().addAll(cancelBtn, okBtn);
        card.getChildren().add(buttons);

        // Tracks the generated OTP code
        final String[] generatedCode = { null };

        // Use arrays so lambdas can reference each other
        Runnable[] doConfirmRef = { null };
        Runnable[] doUpdateRef = { null };

        doConfirmRef[0] = () -> {
            String entered = otpField.getText().trim();
            if (entered.length() != 6) {
                showToast("Enter the full 6-digit code.", true);
                return;
            }
            String email = DatabaseManager.getEmailByUsername(currentUser);
            boolean valid = email != null && DatabaseManager.verifyOtp(email, entered, "RESET");
            if (!valid && !entered.equals(generatedCode[0])) {
                showToast("Incorrect code. Try again.", true);
                return;
            }
            String newP = newVisible[0] ? newPassVisible.getText() : newPassField.getText();
            dismissModal(card);
            if (email != null) {
                CompletableFuture.supplyAsync(() -> DatabaseManager.changePasswordByEmail(email, newP))
                        .thenAccept(ok -> Platform.runLater(() -> showToast(
                                ok ? "Password changed successfully! 🔒" : "Failed to update. Try again.", !ok)));
            } else {
                boolean ok = DatabaseManager.changePassword(currentUser, "", newP);
                showToast(ok ? "Password changed! 🔒" : "Failed to update. Try again.", !ok);
            }
        };

        doUpdateRef[0] = () -> {
            String oldP = oldVisible[0] ? oldPassVisible.getText() : oldPassField.getText();
            String newP = newVisible[0] ? newPassVisible.getText() : newPassField.getText();
            String confP = confVisible[0] ? confPassVisible.getText() : confPassField.getText();

            if (oldP.isEmpty() || newP.isEmpty() || confP.isEmpty()) {
                showToast("Please fill in all fields.", true);
                return;
            }
            if (!newP.equals(confP)) {
                showToast("New passwords don't match!", true);
                return;
            }
            if (!newP.matches("^(?=.*[0-9])(?=.*[A-Z]).{8,}$")) {
                showToast("Password: 8+ chars, 1 uppercase, 1 number.", true);
                return;
            }
            if (oldP.equals(newP)) {
                showToast("New password must differ from current password.", true);
                return;
            }
            if (DatabaseManager.verifyLogin(currentUser, oldP) == null) {
                showToast("Current password is incorrect.", true);
                return;
            }

            // Password validated — generate OTP and reveal OTP section
            String email = DatabaseManager.getEmailByUsername(currentUser);
            String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
            generatedCode[0] = code;
            if (email != null && !email.isBlank())
                DatabaseManager.storeOtp(email, code, "RESET");

            otpCodeDisplay.setText(code);
            otpSection.setVisible(true);
            otpSection.setManaged(true);
            okBtn.setText("Confirm ✓");
            scroll.setVvalue(1.0);
            okBtn.setOnAction(ev -> doConfirmRef[0].run());
            otpField.requestFocus();
        };

        okBtn.setOnAction(e -> doUpdateRef[0].run());
        otpField.setOnAction(e -> doConfirmRef[0].run());

        // Enter-key flow through password fields
        oldPassField.setOnAction(e -> newPassField.requestFocus());
        oldPassVisible.setOnAction(e -> newPassField.requestFocus());
        newPassField.setOnAction(e -> confPassField.requestFocus());
        newPassVisible.setOnAction(e -> confPassField.requestFocus());
        confPassField.setOnAction(e -> doUpdateRef[0].run());
        confPassVisible.setOnAction(e -> doUpdateRef[0].run());

        cancelBtn.setOnAction(e -> dismissModal(card));
        modalOverlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                dismissModal(card);
        });

        showModal(card);
    }

    /**
     * Wraps a PasswordField + TextField (visible clone) + eye Button in a StackPane
     * row so they share the same space.
     */
    private StackPane passwordRow(PasswordField pf, TextField tf, Button eye) {
        StackPane stack = new StackPane(pf, tf);
        StackPane.setAlignment(eye, javafx.geometry.Pos.CENTER_RIGHT);
        eye.setTranslateX(-8);
        stack.getChildren().add(eye);
        stack.setMaxWidth(Double.MAX_VALUE);
        return stack;
    }

    /** Creates a styled eye-toggle button for password fields. */
    private Button eyeToggleBtn() {
        Button btn = new Button("👁");
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"
                + "-fx-font-size: 14px; -fx-padding: 0 4;");
        btn.setFocusTraversable(false);
        return btn;
    }

    @FXML
    void showAboutDialog() {
        VBox card = buildModalCard(360);
        card.getChildren().add(modalTitle("About SkillSync"));
        card.setAlignment(Pos.CENTER);

        ImageView logo = new ImageView(
                new Image(getClass().getResourceAsStream("/com/skill/sync2/skillsync2/logo.png")));
        logo.setFitWidth(80);
        logo.setPreserveRatio(true);
        StackPane logoWrap = new StackPane(logo);
        logoWrap.setStyle("-fx-background-color: rgba(138,43,226,0.06); -fx-background-radius: 200;"
                + "-fx-border-color: rgba(0,229,255,0.3); -fx-border-radius: 200; -fx-border-width: 2; -fx-padding: 14;");
        logoWrap.setMaxSize(110, 110);

        Label version = new Label("Version 1.0.5");
        version.setStyle("-fx-font-size: 13px; -fx-text-fill: #00E5FF; -fx-font-weight: bold;");

        Label desc = new Label(
                "A collaborative platform for students to find\nteammates, manage projects, and build together.");
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isDarkMode ? "#F0EEF8" : "#110F18")
                + "; -fx-text-alignment: center;");
        desc.setWrapText(true);
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label tech = new Label("Built with JavaFX 17 · SQLite · Ikonli");
        tech.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A2BE2; -fx-font-weight: bold;");
        Label copyright = new Label("© 2026 SkillSync Team");
        copyright.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isDarkMode ? "#A39DBE" : "#6C6687") + ";");

        card.getChildren().addAll(logoWrap, version, new Separator(), desc, tech, copyright);

        HBox buttons = modalButtonRow();
        Button closeBtn = modalPrimaryButton("Got it!");
        closeBtn.setOnAction(e -> dismissModal(card));
        modalOverlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                dismissModal(card);
        });
        buttons.getChildren().add(closeBtn);
        card.getChildren().add(buttons);
        showModal(card);
    }

    // ─── Dialog Theme Infrastructure (legacy helpers still used internally) ──────

    /**
     * Fully themed checkbox that matches the Neon Glassmorphism palette.
     * The box and mark are styled inline because modena.css checkbox defaults
     * always override CSS-class rules applied before the scene is shown.
     */
    private CheckBox styledCheckBox(String text, boolean selected) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        // Text + cursor — box/mark styling is handled in applyDialogThemeOnShown()
        cb.setStyle(
                "-fx-text-fill: " + (isDarkMode ? "#F0EEF8" : "#110F18") + ";" +
                        "-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-cursor: hand;" +
                        "-fx-padding: 4 0;");
        return cb;
    }

    /**
     * Returns the inline style string for text/password inputs inside overlay
     * modals.
     */
    private String dialogInputStyle() {
        return isDarkMode
                ? "-fx-background-color: rgba(8,6,14,0.85); -fx-text-fill: #F0EEF8;" +
                        "-fx-prompt-text-fill: #6C6687; -fx-border-color: rgba(138,43,226,0.35);" +
                        "-fx-border-radius: 9px; -fx-background-radius: 9px; -fx-padding: 10 14;" +
                        "-fx-font-size: 13px;"
                : "-fx-background-color: #FFFFFF; -fx-text-fill: #110F18;" +
                        "-fx-prompt-text-fill: #8884A8; -fx-border-color: #D9D5EE;" +
                        "-fx-border-radius: 9px; -fx-background-radius: 9px; -fx-padding: 10 14;" +
                        "-fx-font-size: 13px;";
    }

    // ─── In-App Modal Overlay Infrastructure ─────────────────────────────────

    /**
     * Shows a branded in-app confirm overlay and returns true if user pressed OK.
     * Uses a JavaFX nested event loop so it behaves synchronously.
     */
    private boolean confirm(String title, String header, String body) {
        final boolean[] result = { false };
        VBox card = buildModalCard(480);
        card.getChildren().add(modalTitle(title));
        if (header != null && !header.isBlank()) {
            Label headerLbl = new Label(header);
            headerLbl.setWrapText(true);
            headerLbl.setStyle("-fx-text-fill: " + (isDarkMode ? "#F0EEF8" : "#110F18")
                    + "; -fx-font-weight: 700; -fx-font-size: 14px; -fx-padding: 0 0 4 0;");
            card.getChildren().add(headerLbl);
        }
        Label bodyLbl = new Label(body);
        bodyLbl.setWrapText(true);
        bodyLbl.setStyle("-fx-text-fill: " + (isDarkMode ? "#A39DBE" : "#6C6687")
                + "; -fx-font-size: 13px; -fx-padding: 4 0 14 0;");
        card.getChildren().add(bodyLbl);
        HBox buttons = modalButtonRow();
        Button okBtn = modalPrimaryButton("OK");
        Button cancelBtn = modalSecondaryButton("Cancel");
        okBtn.setOnAction(e -> {
            result[0] = true;
            dismissModal(card);
        });
        cancelBtn.setOnAction(e -> dismissModal(card));
        modalOverlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                dismissModal(card);
        });
        buttons.getChildren().addAll(cancelBtn, okBtn);
        card.getChildren().add(buttons);
        showModal(card);
        javafx.application.Platform.enterNestedEventLoop(card);
        return result[0];
    }

    /**
     * Shows an in-app text-input overlay. Returns entered text, or null if
     * cancelled.
     */
    private String modalTextInput(String title, String header, String defaultValue) {
        final String[] result = { null };
        VBox card = buildModalCard(460);
        card.getChildren().add(modalTitle(title));
        if (header != null && !header.isBlank()) {
            Label hdr = new Label(header);
            hdr.setWrapText(true);
            hdr.setStyle("-fx-text-fill: " + (isDarkMode ? "#A39DBE" : "#6C6687")
                    + "; -fx-font-size: 13px; -fx-padding: 0 0 6 0;");
            card.getChildren().add(hdr);
        }
        TextField input = new TextField(defaultValue != null ? defaultValue : "");
        input.setStyle(dialogInputStyle());
        input.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(input);
        HBox buttons = modalButtonRow();
        Button okBtn = modalPrimaryButton("Send");
        Button cancelBtn = modalSecondaryButton("Cancel");
        okBtn.setOnAction(e -> {
            result[0] = input.getText();
            dismissModal(card);
        });
        cancelBtn.setOnAction(e -> dismissModal(card));
        input.setOnAction(e -> okBtn.fire());
        modalOverlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE)
                dismissModal(card);
        });
        buttons.getChildren().addAll(cancelBtn, okBtn);
        card.getChildren().add(buttons);
        showModal(card);
        javafx.application.Platform.enterNestedEventLoop(card);
        return result[0];
    }

    // ── Modal building helpers ────────────────────────────────────────────────

    private VBox buildModalCard(double width) {
        VBox card = new VBox(12);
        card.setMaxWidth(width);
        card.setPrefWidth(width);
        card.setMaxHeight(Region.USE_PREF_SIZE); // shrink-wrap to content height
        card.setMinHeight(Region.USE_PREF_SIZE);
        card.setStyle(isDarkMode
                ? "-fx-background-color: #0F0C1B; -fx-background-radius: 18;"
                        + "-fx-border-color: rgba(138,43,226,0.55); -fx-border-radius: 18; -fx-border-width: 1.5;"
                        + "-fx-padding: 26 28 22 28;"
                        + "-fx-effect: dropshadow(gaussian,rgba(138,43,226,0.35),50,0,0,12);"
                : "-fx-background-color: #FAFAFA; -fx-background-radius: 18;"
                        + "-fx-border-color: #D9D5EE; -fx-border-radius: 18; -fx-border-width: 1.5;"
                        + "-fx-padding: 26 28 22 28;"
                        + "-fx-effect: dropshadow(gaussian,rgba(100,60,200,0.18),40,0,0,8);");
        return card;
    }

    private Label modalTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + (isDarkMode ? "#F0EEF8" : "#110F18")
                + "; -fx-font-weight: 900; -fx-font-size: 17px;"
                + "-fx-border-color: " + (isDarkMode ? "rgba(138,43,226,0.3)" : "#E4DFF2") + ";"
                + "-fx-border-width: 0 0 1.5 0; -fx-padding: 0 0 10 0;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private Label modalSubLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + (isDarkMode ? "#F0EEF8" : "#110F18")
                + "; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 0 2 0;");
        return lbl;
    }

    private HBox modalButtonRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setStyle("-fx-padding: 12 0 0 0;"
                + "-fx-border-color: " + (isDarkMode ? "rgba(138,43,226,0.18)" : "#E4DFF2") + ";"
                + "-fx-border-width: 1.5 0 0 0;");
        return row;
    }

    private Button modalPrimaryButton(String text) {
        Button btn = new Button(text);
        String base = "-fx-background-color: linear-gradient(to right,#8A2BE2,#FF2A85);"
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;"
                + "-fx-background-radius: 9; -fx-padding: 9 24; -fx-cursor: hand; -fx-border-color: transparent;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: linear-gradient(to right,#9B3DFF,#FF4D9A);"
                        + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;"
                        + "-fx-background-radius: 9; -fx-padding: 9 24; -fx-cursor: hand; -fx-border-color: transparent;"
                        + "-fx-effect: dropshadow(gaussian,rgba(255,42,133,0.55),18,0,0,0);"));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button modalSecondaryButton(String text) {
        Button btn = new Button(text);
        String accentSoft = isDarkMode ? "rgba(138,43,226,0.18)" : "rgba(138,43,226,0.09)";
        String borderSoft = isDarkMode ? "rgba(138,43,226,0.28)" : "#D9D5EE";
        String textMain = isDarkMode ? "#F0EEF8" : "#110F18";
        String base = "-fx-background-color: " + accentSoft + "; -fx-text-fill: " + textMain
                + "; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 9;"
                + "-fx-border-radius: 9; -fx-border-color: " + borderSoft
                + "; -fx-border-width: 1.5; -fx-padding: 9 24; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: rgba(138,43,226,0.32); -fx-text-fill: " + textMain
                        + "; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 9;"
                        + "-fx-border-radius: 9; -fx-border-color: #8A2BE2; -fx-border-width: 1.5;"
                        + "-fx-padding: 9 24; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void showModal(VBox card) {
        // Wrap in a StackPane so the overlay's alignment doesn't stretch the card
        StackPane wrapper = new StackPane(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setStyle("-fx-padding: 40 0;"); // breathing room top/bottom
        StackPane.setAlignment(card, Pos.CENTER);
        modalOverlay.getChildren().setAll(wrapper);
        modalOverlay.setVisible(true);
        modalOverlay.requestFocus();
        card.setOpacity(0);
        card.setTranslateY(18);
        FadeTransition ft = new FadeTransition(Duration.millis(220), card);
        ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), card);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    private void dismissModal(VBox card) {
        FadeTransition ft = new FadeTransition(Duration.millis(180), card);
        ft.setToValue(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(180), card);
        tt.setToY(12);
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setOnFinished(e -> {
            modalOverlay.setVisible(false);
            modalOverlay.getChildren().clear(); // clears the wrapper
            javafx.application.Platform.exitNestedEventLoop(card, null);
        });
        pt.play();
    }

    // ─── Particle Effects ─────────────────────────────────────────────────────

    @FXML
    void setEffectSakura() {
        prefPut("particleEffect", "SAKURA");
        startParticleEngine("SAKURA");
    }

    @FXML
    void setEffectSnow() {
        prefPut("particleEffect", "SNOW");
        startParticleEngine("SNOW");
    }

    @FXML
    void setEffectLeaves() {
        prefPut("particleEffect", "LEAVES");
        startParticleEngine("LEAVES");
    }

    @FXML
    void setEffectOff() {
        prefPut("particleEffect", "OFF");
        startParticleEngine("OFF");
    }

    private void startParticleEngine(String type) {
        Platform.runLater(() -> {
            javafx.scene.Parent splashParent = splashScreen.getParent();
            if (!(splashParent instanceof StackPane))
                return;
            StackPane root = (StackPane) splashParent;
            if (particlePane == null) {
                particlePane = new Pane();
                particlePane.setMouseTransparent(true);
                particlePane.setManaged(false);
                particlePane.prefWidthProperty().bind(root.widthProperty());
                particlePane.prefHeightProperty().bind(root.heightProperty());
                root.getChildren().add(0, particlePane);
            }
            if (particleTimer != null)
                particleTimer.stop();
            particlePane.getChildren().clear();
            if ("OFF".equals(type))
                return;
            int count = "SNOW".equals(type) ? 60 : 40;
            for (int i = 0; i < count; i++) {
                Node p = createParticle(type);
                particlePane.getChildren().add(p);
                resetParticle(p, 1920, 1080, true, type);
            }
            particleTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    double w = particlePane.getWidth() == 0 ? 1200 : particlePane.getWidth();
                    double h = particlePane.getHeight() == 0 ? 800 : particlePane.getHeight();
                    for (Node p : particlePane.getChildren()) {
                        double dx = (double) p.getProperties().get("dx");
                        double dy = (double) p.getProperties().get("dy");
                        double dRot = (double) p.getProperties().get("dRot");
                        p.setTranslateX(p.getTranslateX() + dx);
                        p.setTranslateY(p.getTranslateY() + dy);
                        p.setRotate(p.getRotate() + dRot);
                        if ("SNOW".equals(type)) {
                            dx += (random.nextDouble() - 0.5) * 0.02;
                            dx = Math.max(-1.0, Math.min(1.0, dx));
                        } else {
                            dx += (random.nextDouble() - 0.5) * 0.05;
                            dx = Math.max(-2.0, Math.min(2.0, dx));
                        }
                        p.getProperties().put("dx", dx);
                        if (p.getTranslateY() > h + 20)
                            resetParticle(p, w, h, false, type);
                    }
                }
            };
            particleTimer.start();
        });
    }

    private Node createParticle(String type) {
        if ("SNOW".equals(type)) {
            javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(random.nextDouble() * 3 + 2);
            c.setFill(javafx.scene.paint.Color.web("#00E5FF", 0.8));
            c.setEffect(new DropShadow(5, javafx.scene.paint.Color.web("#00E5FF")));
            return c;
        } else if ("LEAVES".equals(type)) {
            Polygon leaf = new Polygon(0, 0, 8, 4, 12, 12, 4, 8);
            String[] colors = { "#e67e22", "#d35400", "#f1c40f", "#e74c3c" };
            leaf.setFill(javafx.scene.paint.Color.web(colors[random.nextInt(colors.length)], 0.7));
            return leaf;
        } else {
            Polygon petal = new Polygon(0, 0, 6, 10, 0, 20, -6, 10);
            petal.setFill(javafx.scene.paint.Color.web("#ffb7c5", 0.6));
            petal.setEffect(new DropShadow(5, javafx.scene.paint.Color.web("#ffb7c5", 0.4)));
            return petal;
        }
    }

    private void resetParticle(Node p, double w, double h, boolean randomY, String type) {
        p.setTranslateX(random.nextDouble() * w);
        p.setTranslateY(randomY ? random.nextDouble() * h : -20);
        p.setRotate(random.nextDouble() * 360);
        double spd = "SNOW".equals(type) ? 0.5 : 1.0;
        p.getProperties().put("dx", (random.nextDouble() * 2 - 1) * spd);
        p.getProperties().put("dy", (random.nextDouble() * 1.5 + 1) * spd);
        p.getProperties().put("dRot", random.nextDouble() * 3 - 1.5);
        double scale = random.nextDouble() * 0.6 + 0.4;
        p.setScaleX(scale);
        p.setScaleY(scale);
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
        Label lbl = new Label(initial);
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + fontSize
                + "px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 2, 0, 1, 1);");
        StackPane base = new StackPane(lbl);
        base.setMinSize(size, size);
        base.setMaxSize(size, size);
        base.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: " + (size / 2) + ";");
        boolean online = name != null && (name.equals(currentUser) || onlineUsersCache.contains(name));
        if (online && name != null && !name.startsWith("#")) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(size * 0.16);
            dot.setStyle("-fx-fill: #10b981; -fx-effect: dropshadow(gaussian, rgba(16,185,129,0.6), 5, 0.5, 0, 0);");
            dot.setStroke(javafx.scene.paint.Color.web("#1e293b"));
            dot.setStrokeWidth(2.5);
            dot.setTranslateX(size * 0.08);
            dot.setTranslateY(size * 0.08);
            StackPane container = new StackPane(base, dot);
            container.setAlignment(Pos.BOTTOM_RIGHT);
            container.setMaxSize(size, size);
            return container;
        }
        return base;
    }

    // ─── Custom List Cells ────────────────────────────────────────────────────

    private void setupCustomListCells() {
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }
                HBox root = new HBox(14);
                root.getStyleClass().add("card");
                root.setAlignment(Pos.CENTER_LEFT);
                root.prefWidthProperty().bind(lv.widthProperty().subtract(40));
                VBox info = new VBox(4);
                Label name = new Label(s.getName());
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -color-text-main;");
                Label skills = new Label("Skills: " + s.getSkills());
                skills.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-sub;");
                Label bio = new Label(s.getBio());
                bio.setStyle("-fx-font-style: italic; -fx-font-size: 12px; -fx-text-fill: -color-text-sub;");
                bio.setWrapText(true);
                info.getChildren().addAll(name, skills, bio);
                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                if (!s.getName().equals(currentUser)) {
                    Button view = actionBtn("View Profile", "transparent", e -> showPublicProfile(s.getName()));
                    Button msg = actionBtn("Message", "#3498db", e -> {
                        if (!chatContactsList.getItems().contains(s.getName()))
                            chatContactsList.getItems().add(s.getName());
                        chatContactsList.getSelectionModel().select(s.getName());
                        navToChat();
                    });
                    Button inv = actionBtn("Invite", "#d35400", e -> handleSendTeamInvite(s.getName()));
                    view.setStyle(
                            "-fx-background-color: transparent; -fx-border-color: -color-border; -fx-text-fill: -color-text-main; -fx-pref-height: 28; -fx-font-size: 11px;");
                    msg.setStyle("-fx-background-color: #3498db; -fx-pref-height: 28; -fx-font-size: 11px;");
                    inv.setStyle("-fx-background-color: #d35400; -fx-pref-height: 28; -fx-font-size: 11px;");
                    root.getChildren().addAll(getAvatar(s.getName(), 46, 18), info, spacer,
                            new HBox(8, view, msg, inv));
                    ContextMenu cm = new ContextMenu();
                    MenuItem vi = new MenuItem("View Profile");
                    vi.setOnAction(e -> showPublicProfile(s.getName()));
                    MenuItem inviteItem = new MenuItem("📨 Invite to Team");
                    inviteItem.setOnAction(e -> handleSendTeamInvite(s.getName()));
                    cm.getItems().addAll(vi, inviteItem);
                    setContextMenu(cm);
                } else {
                    root.getChildren().addAll(getAvatar(s.getName(), 46, 18), info, spacer, new Label("(You)"));
                }
                setGraphic(root);
            }
        });

        projectListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Project p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }

                // ── Determine status using the DB-backed status field ─────────
                boolean isExpired = p.isExpired();
                boolean isDone = p.isDone();
                boolean isLocked = isExpired || isDone;
                boolean isOwner = p.getOwnerName().equals(currentUser);

                VBox root = new VBox(10);
                root.getStyleClass().add("card");
                root.prefWidthProperty().bind(lv.widthProperty().subtract(40));

                if (isLocked)
                    root.setStyle("-fx-opacity: 0.72;");

                HBox header = new HBox(12);
                header.setAlignment(Pos.CENTER_LEFT);
                Label titleLbl = new Label(p.getTitle());
                titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: -color-text-main;");
                HBox.setHgrow(titleLbl, Priority.ALWAYS);
                header.getChildren().addAll(getAvatar(p.getOwnerName(), 36, 14), titleLbl);

                if (isDone) {
                    Label doneBadge = new Label("✅ COMPLETED");
                    doneBadge.setStyle("-fx-background-color: rgba(16,185,129,0.2); -fx-text-fill: #10b981;"
                            + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 7;"
                            + "-fx-background-radius: 4;");
                    header.getChildren().add(doneBadge);
                } else if (isExpired) {
                    Label expiredBadge = new Label("⏰ EXPIRED");
                    expiredBadge.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white;"
                            + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 7;"
                            + "-fx-background-radius: 4;");
                    header.getChildren().add(expiredBadge);
                }

                ContextMenu cm = new ContextMenu();
                Pane btnSpacer = new Pane();
                HBox.setHgrow(btnSpacer, Priority.ALWAYS);
                header.getChildren().add(btnSpacer);

                if (!isOwner) {
                    Button viewOwner = actionBtn("View Owner", "transparent", e -> showPublicProfile(p.getOwnerName()));
                    viewOwner.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border;"
                            + "-fx-text-fill: -color-text-main; -fx-pref-height: 28; -fx-font-size: 11px;");

                    if (isLocked) {
                        String lockMsg = isDone ? "🔒 Completed" : "🔒 Closed";
                        Label closedLbl = new Label(lockMsg);
                        closedLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;"
                                + "-fx-font-style: italic; -fx-padding: 0 4;");
                        header.getChildren().addAll(viewOwner, closedLbl);
                    } else {
                        Button apply = actionBtn("Apply", "#2980b9", e -> handleJoinRequest(p));
                        apply.setStyle("-fx-background-color: #2980b9; -fx-pref-height: 28; -fx-font-size: 11px;");
                        header.getChildren().addAll(viewOwner, apply);
                        MenuItem ai = new MenuItem("Apply to Project");
                        ai.setOnAction(e -> handleJoinRequest(p));
                        cm.getItems().add(ai);
                    }
                    MenuItem vi = new MenuItem("View " + p.getOwnerName() + "'s Profile");
                    vi.setOnAction(e -> showPublicProfile(p.getOwnerName()));
                    cm.getItems().add(0, vi);
                } else {
                    String ownerTag = isDone ? "Your Project (Done)"
                            : isExpired ? "Your Project (Expired)" : "Your Project";
                    String ownerColor = isDone ? "#10b981" : isExpired ? "#ef4444" : "-color-text-sub";
                    Label yours = new Label(ownerTag);
                    yours.setStyle("-fx-text-fill: " + ownerColor + "; -fx-font-style: italic; -fx-font-size: 12px;");

                    if (!isLocked) {
                        Button markDone = new Button("✅ Mark Done");
                        markDone.setStyle(
                                "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10b981;"
                                        + "-fx-border-color: #10b981; -fx-border-width: 1;"
                                        + "-fx-background-radius: 6; -fx-border-radius: 6;"
                                        + "-fx-pref-height: 28; -fx-font-size: 11px; -fx-cursor: hand;");
                        markDone.setOnAction(e -> {
                            if (confirm("Mark Project as Done?",
                                    "Squad members will lose access to chat and the task board.",
                                    "Mark \"" + p.getTitle() + "\" as completed?"))
                                CompletableFuture.runAsync(() -> {
                                    try {
                                        DatabaseManager.markProjectDone(p.getId());
                                        Platform.runLater(() -> {
                                            refreshProjectList();
                                            showToast("✅ Project marked as done!", false);
                                        });
                                    } catch (Exception ex) {
                                        Platform.runLater(
                                                () -> showToast("Failed to update project. Try again.", true));
                                    }
                                });
                        });
                        header.getChildren().add(markDone);
                    }

                    Button del = actionBtn("Delete", "#e74c3c", e -> {
                        if (confirm("Delete Project", "⚠️ This is permanent.", "Delete \"" + p.getTitle()
                                + "\"? This will also remove its group chat, tasks, and applications."))
                            CompletableFuture.runAsync(() -> {
                                try {
                                    DatabaseManager.deleteProject(p.getId());
                                    Platform.runLater(() -> {
                                        refreshProjectList();
                                        showToast("Project removed.", false);
                                    });
                                } catch (Exception ex) {
                                    Platform.runLater(() -> showToast("Failed to delete project. Try again.", true));
                                }
                            });
                    });
                    del.setStyle("-fx-background-color: #e74c3c; -fx-pref-height: 28; -fx-font-size: 11px;");
                    header.getChildren().addAll(yours, del);

                    if (!isLocked) {
                        MenuItem markDoneCtx = new MenuItem("✅ Mark as Done");
                        markDoneCtx.setOnAction(e -> {
                            if (confirm("Mark Project as Done?",
                                    "Squad members will lose access to chat and the task board.",
                                    "Mark \"" + p.getTitle() + "\" as completed?"))
                                CompletableFuture.runAsync(() -> {
                                    try {
                                        DatabaseManager.markProjectDone(p.getId());
                                        Platform.runLater(() -> {
                                            refreshProjectList();
                                            showToast("✅ Project marked as done!", false);
                                        });
                                    } catch (Exception ex) {
                                        Platform.runLater(() -> showToast("Failed to update project.", true));
                                    }
                                });
                        });
                        cm.getItems().add(markDoneCtx);
                    }
                    MenuItem deleteCtx = new MenuItem("🗑 Delete Project");
                    deleteCtx.setOnAction(e -> {
                        if (confirm("Delete Project", "⚠️ This is permanent.",
                                "Delete \"" + p.getTitle()
                                        + "\"? This will also remove its group chat, tasks, and applications."))
                            CompletableFuture.runAsync(() -> {
                                try {
                                    DatabaseManager.deleteProject(p.getId());
                                    Platform.runLater(() -> {
                                        refreshProjectList();
                                        showToast("Project removed.", false);
                                    });
                                } catch (Exception ex) {
                                    Platform.runLater(() -> showToast("Failed to delete project.", true));
                                }
                            });
                    });
                    MenuItem copyTitle = new MenuItem("📋 Copy Project Title");
                    copyTitle.setOnAction(e -> {
                        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                        cc.putString(p.getTitle());
                        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                        showToast("Copied: " + p.getTitle(), false);
                    });
                    cm.getItems().addAll(deleteCtx, copyTitle);
                }

                Label goal = new Label("🎯 " + p.getDescription());
                goal.setStyle("-fx-text-fill: -color-text-main; -fx-font-size: 13px;");
                goal.setWrapText(true);
                Label role = new Label("🔍 Looking for: " + p.getRequiredRole());
                role.setStyle("-fx-text-fill: -color-text-main; -fx-font-size: 13px;");
                Label resp = new Label("📝 " + p.getResponsibilities());
                resp.setStyle("-fx-text-fill: -color-text-main; -fx-font-size: 13px;");
                resp.setWrapText(true);
                Label due = buildDueDateLabel(p.getDueDate());
                root.getChildren().addAll(header, goal, role, resp, due);
                setGraphic(root);
                setContextMenu(cm.getItems().isEmpty() ? null : cm);
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
            @Override
            protected void updateItem(String contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 7 10;");
                row.getStyleClass().add("card");
                row.prefWidthProperty().bind(lv.widthProperty().subtract(30));
                Label name = new Label(contact);
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -color-text-main;");
                row.getChildren().addAll(getAvatar(contact, 32, 14), name);
                row.setOnMouseClicked(e -> {
                    currentChatUser = contact;
                    chatHeaderLabel.setText(contact.startsWith("#") ? "Team Chat: " + contact : "Chat with " + contact);
                    CompletableFuture.runAsync(() -> DatabaseManager.markMessagesAsRead(contact, currentUser));
                    refreshChatHistory(true);
                });
                if (!contact.startsWith("#")) {
                    ContextMenu cm = new ContextMenu();
                    MenuItem vi = new MenuItem("View Profile");
                    vi.setOnAction(e -> showPublicProfile(contact));
                    MenuItem openChat = new MenuItem("Open Chat");
                    openChat.setOnAction(e -> {
                        currentChatUser = contact;
                        chatHeaderLabel.setText("Chat with " + contact);
                        CompletableFuture.runAsync(() -> DatabaseManager.markMessagesAsRead(contact, currentUser));
                        refreshChatHistory(true);
                    });
                    MenuItem copyName = new MenuItem("Copy Username");
                    copyName.setOnAction(e -> {
                        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                        cc.putString(contact);
                        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                        showToast("Copied: " + contact, false);
                    });
                    cm.getItems().addAll(vi, openChat, copyName);
                    setContextMenu(cm);
                } else {
                    // Group chat: copy group name
                    ContextMenu cm = new ContextMenu();
                    MenuItem copyGroup = new MenuItem("Copy Group Name");
                    copyGroup.setOnAction(e -> {
                        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                        cc.putString(contact);
                        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                        showToast("Copied: " + contact, false);
                    });
                    cm.getItems().add(copyGroup);
                    setContextMenu(cm);
                }
                setGraphic(row);
            }
        });

        chatListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                boolean mine = msg.getSender().equals(currentUser);
                HBox row = new HBox(10);
                row.setStyle("-fx-padding: 4; -fx-background-color: transparent;");
                row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                row.prefWidthProperty().bind(lv.widthProperty().subtract(40));

                VBox bubble = new VBox(4);
                Label time = new Label(msg.getTimestamp());
                time.setStyle("-fx-font-size: 9px; -fx-text-fill: " + (mine ? "#ecf0f1" : "-color-text-sub") + ";");

                if (msg.getContent().startsWith("[FILE]")) {
                    String[] parts = msg.getContent().substring(6).split("\\|");
                    String path = parts[0];
                    String fname = parts.length > 1 ? parts[1] : "Attachment";
                    Button openBtn = new Button("📎 " + fname);
                    openBtn.getStyleClass().add("action-btn");
                    openBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10;");
                    openBtn.setOnAction(e -> openLocalFile(path));
                    bubble.getChildren().addAll(openBtn, time);
                } else {
                    Label content = new Label(msg.getContent());
                    content.setWrapText(true);
                    content.maxWidthProperty().bind(lv.widthProperty().multiply(0.65));
                    content.setMinHeight(Region.USE_PREF_SIZE);
                    content.setStyle(
                            "-fx-text-fill: " + (mine ? "white" : "-color-text-main") + "; -fx-font-size: 13px;");
                    bubble.getChildren().addAll(content, time);
                }
                bubble.setStyle(mine
                        ? "-fx-background-color: #3498db; -fx-background-radius: 14 14 0 14; -fx-padding: 8 12;"
                        : "-fx-background-color: -color-bg-base; -fx-border-color: -color-border; -fx-border-radius: 14 14 14 0; -fx-background-radius: 14 14 14 0; -fx-padding: 8 12;");

                if (mine)
                    row.getChildren().addAll(bubble, getAvatar(msg.getSender(), 28, 12));
                else
                    row.getChildren().addAll(getAvatar(msg.getSender(), 28, 12), bubble);

                // ── Message context menu ─────────────────────────────────────
                ContextMenu msgCm = new ContextMenu();
                MenuItem copyMsg = new MenuItem("📋 Copy Message");
                copyMsg.setOnAction(e -> {
                    javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                    cc.putString(msg.getContent());
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                    showToast("Message copied.", false);
                });
                msgCm.getItems().add(copyMsg);
                if (!msg.getSender().equals(currentUser)) {
                    MenuItem viewSender = new MenuItem("👤 View " + msg.getSender() + "'s Profile");
                    viewSender.setOnAction(e -> showPublicProfile(msg.getSender()));
                    msgCm.getItems().add(viewSender);
                }
                setContextMenu(msgCm);

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
        if (text == null)
            return false;
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
        Button btn = new Button(text);
        btn.getStyleClass().add("action-btn");
        if (!bg.equals("transparent"))
            btn.setStyle("-fx-background-color: " + bg + "; -fx-pref-height: 28; -fx-font-size: 11px;");
        btn.setOnAction(handler);
        return btn;
    }

    private Label buildDueDateLabel(String dateStr) {
        Label lbl = new Label();
        if (dateStr != null && !"Not Specified".equals(dateStr)) {
            try {
                LocalDate d = LocalDate.parse(dateStr);
                if (d.isBefore(LocalDate.now())) {
                    lbl.setText("⚠️ EXPIRED (" + dateStr + ")");
                    lbl.setStyle(
                            "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else {
                    lbl.setText("📅 Due: " + dateStr);
                    lbl.setStyle(
                            "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px;");
                }
            } catch (Exception e) {
                lbl.setText("📅 " + dateStr);
            }
        } else {
            lbl.setText("📅 No Deadline");
            lbl.setStyle("-fx-text-fill: -color-text-sub; -fx-font-size: 11px;");
        }
        return lbl;
    }

    private ListCell<Invitation> createInviteCell(ListView<Invitation> lv) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Invitation inv, boolean empty) {
                super.updateItem(inv, empty);
                if (empty || inv == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }
                VBox root = new VBox(9);
                root.getStyleClass().add("card");
                root.prefWidthProperty().bind(lv.widthProperty().subtract(40));

                HBox header = new HBox(12);
                header.setAlignment(Pos.CENTER_LEFT);
                VBox senderBox = new VBox(2);

                // Sender name + type badge on same row
                HBox senderRow = new HBox(8);
                senderRow.setAlignment(Pos.CENTER_LEFT);
                Label senderLbl = new Label(inv.getSenderInfo());
                senderLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -color-text-main;");
                Label typeBadge = new Label("PROJECT".equals(inv.getType()) ? "📋 Project" : "👥 Team");
                typeBadge.setStyle("-fx-background-color: "
                        + ("PROJECT".equals(inv.getType()) ? "rgba(41,128,185,0.2)" : "rgba(39,174,96,0.2)")
                        + "; -fx-text-fill: " + ("PROJECT".equals(inv.getType()) ? "#2980b9" : "#27ae60")
                        + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 7; -fx-background-radius: 4;");
                senderRow.getChildren().addAll(senderLbl, typeBadge);
                senderBox.getChildren().add(senderRow);

                if ("PROJECT".equals(inv.getType())) {
                    Label proj = new Label("For: " + inv.getRelatedTitle());
                    proj.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2980b9;");
                    senderBox.getChildren().add(proj);
                }

                // Timestamp if available
                if (inv.getTimestamp() != null && !inv.getTimestamp().isBlank()) {
                    Label time = new Label("🕐 " + inv.getTimestamp());
                    time.setStyle("-fx-text-fill: -color-text-sub; -fx-font-size: 10px;");
                    senderBox.getChildren().add(time);
                }

                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Button viewBtn = actionBtn("View Profile", "transparent", e -> showPublicProfile(inv.getSenderInfo()));
                viewBtn.setStyle("-fx-background-color: transparent; -fx-border-color: -color-border;"
                        + "-fx-text-fill: -color-text-main; -fx-pref-height: 28; -fx-font-size: 11px;");
                header.getChildren().addAll(getAvatar(inv.getSenderInfo(), 36, 14), senderBox, spacer, viewBtn);

                Label msgLbl = new Label(inv.getMessage());
                msgLbl.setWrapText(true);
                msgLbl.setMinHeight(Region.USE_PREF_SIZE);
                msgLbl.setStyle("-fx-text-fill: -color-text-main; -fx-font-size: 13px;");

                HBox actions = new HBox(10);
                if ("PENDING".equals(inv.getStatus())) {
                    Button accept = actionBtn("Accept", "#27ae60", e -> {
                        String who = inv.getSenderInfo();
                        String what = "PROJECT".equals(inv.getType())
                                ? "Accept " + who + "'s application for \"" + inv.getRelatedTitle() + "\"?"
                                : "Accept team invite from " + who + "?";
                        if (confirm("Accept Invite", null, what))
                            handleInviteAction(inv, "ACCEPTED");
                    });
                    Button decline = actionBtn("Decline", "#e74c3c", e -> {
                        if (confirm("Decline Invite", null, "Decline this invite from " + inv.getSenderInfo() + "?"))
                            handleInviteAction(inv, "DECLINED");
                    });
                    accept.setStyle("-fx-background-color: #27ae60; -fx-pref-height: 28; -fx-font-size: 11px;");
                    decline.setStyle("-fx-background-color: #e74c3c; -fx-pref-height: 28; -fx-font-size: 11px;");
                    actions.getChildren().addAll(accept, decline);
                } else {
                    boolean accepted = "ACCEPTED".equals(inv.getStatus());
                    Label status = new Label((accepted ? "✅ " : "❌ ") + "Status: " + inv.getStatus());
                    status.setStyle(
                            "-fx-font-style: italic; -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: "
                                    + (accepted ? "#27ae60" : "#e74c3c") + ";");
                    actions.getChildren().add(status);
                }

                ContextMenu cm = new ContextMenu();
                MenuItem vi = new MenuItem("View " + inv.getSenderInfo() + "'s Profile");
                vi.setOnAction(e -> showPublicProfile(inv.getSenderInfo()));
                cm.getItems().add(vi);
                root.getChildren().addAll(header, msgLbl, actions);
                setGraphic(root);
                setContextMenu(cm);
            }
        };
    }

    private ListCell<Task> createTaskCell(ListView<Task> lv) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }

                String status = task.getStatus() == null ? "PENDING" : task.getStatus();
                boolean isDone = "DONE".equalsIgnoreCase(status);

                // ── Check if task deadline has passed ────────────────────────
                boolean isOverdue = false;
                if (task.getDueDate() != null && !"None".equals(task.getDueDate())) {
                    try {
                        isOverdue = LocalDate.parse(task.getDueDate()).isBefore(LocalDate.now());
                    } catch (Exception ignored) {
                    }
                }

                VBox card = new VBox(10);
                card.getStyleClass().add("card");
                card.prefWidthProperty().bind(lv.widthProperty().subtract(35));

                // Red left-border accent for overdue non-done tasks
                if (isOverdue && !isDone)
                    card.setStyle("-fx-border-color: #ef4444; -fx-border-width: 0 0 0 3;");

                // Title row with optional overdue tag
                HBox titleRow = new HBox(8);
                titleRow.setAlignment(Pos.CENTER_LEFT);
                Label titleLbl = new Label(task.getTitle());
                titleLbl.setWrapText(true);
                titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: "
                        + (isDone ? "-color-text-sub" : "-color-text-main") + ";"
                        + (isDone ? "-fx-strikethrough: true;" : ""));
                titleRow.getChildren().add(titleLbl);

                if (isOverdue && !isDone) {
                    Label overdueBadge = new Label("⚠ OVERDUE");
                    overdueBadge.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5;"
                            + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6;"
                            + "-fx-background-radius: 4;");
                    titleRow.getChildren().add(overdueBadge);
                }

                HBox footer = new HBox(6);
                footer.setAlignment(Pos.CENTER_LEFT);

                if ("PENDING".equalsIgnoreCase(status)) {
                    if (isOverdue) {
                        // Overdue pending tasks cannot be claimed — show locked state
                        Label lockedLbl = new Label("🔒 Expired — cannot claim");
                        lockedLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");
                        footer.getChildren().add(lockedLbl);
                    } else {
                        Button dibs = new Button("Claim ✋");
                        dibs.getStyleClass().add("dibs-btn");
                        dibs.setOnAction(e -> {
                            if (HelloController.this.confirm("Claim Task", null,
                                    "Claim \"" + task.getTitle() + "\"? It'll be assigned to you."))
                                handleClaimTask(task);
                        });
                        footer.getChildren().add(dibs);
                    }
                } else {
                    Label assigned = new Label("@" + task.getAssignedTo());
                    assigned.setStyle("-fx-text-fill: " + (isDone ? "#10b981" : "#00E5FF")
                            + "; -fx-font-weight: bold; -fx-font-size: 11px;");
                    footer.getChildren().add(assigned);
                    if (isDone) {
                        Label doneBadge = new Label("✅ Done");
                        doneBadge.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: bold;");
                        footer.getChildren().add(doneBadge);
                    }
                }

                // Due date chip
                if (task.getDueDate() != null && !"None".equals(task.getDueDate())) {
                    String dueTxt = (isOverdue && !isDone) ? "⚠️ " : "📅 ";
                    Label due = new Label(dueTxt + task.getDueDate());
                    due.setStyle("-fx-text-fill: " + (isOverdue && !isDone ? "#ef4444" : "#A39DBE")
                            + "; -fx-font-size: 11px;");
                    footer.getChildren().add(due);
                }

                card.getChildren().addAll(titleRow, footer);
                setGraphic(card);

                // ── Context menu ─────────────────────────────────────────────
                ContextMenu cm = new ContextMenu();
                if ("IN_PROGRESS".equalsIgnoreCase(status) && currentUser != null
                        && currentUser.equalsIgnoreCase(task.getAssignedTo())) {
                    MenuItem done = new MenuItem("Mark as Done ✅");
                    done.setOnAction(e -> {
                        if (HelloController.this.confirm("Mark as Done", null,
                                "Mark \"" + task.getTitle() + "\" as completed?"))
                            changeTaskStatus(task.getId(), "DONE");
                    });
                    cm.getItems().add(done);
                }
                MenuItem del = new MenuItem("Remove Task");
                del.setOnAction(e -> {
                    if (HelloController.this.confirm("Remove Task", null,
                            "Permanently delete \"" + task.getTitle() + "\"?"))
                        CompletableFuture.runAsync(() -> {
                            try {
                                DatabaseManager.deleteTask(task.getId());
                                Platform.runLater(this::refreshWorkspaceTasks);
                            } catch (Exception ex) {
                                Platform.runLater(() -> HelloController.this
                                        .showToast("Failed to remove task. Try again.", true));
                            }
                        });
                });
                cm.getItems().add(del);
                setContextMenu(cm);
            }

            private void refreshWorkspaceTasks() {
                HelloController.this.refreshWorkspaceTasks();
            }
        };
    }

    private ListCell<PortfolioItem> createPortfolioCell(boolean isOwner, ListView<PortfolioItem> lv) {
        return new ListCell<>() {
            @Override
            protected void updateItem(PortfolioItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                HBox root = new HBox(14);
                root.getStyleClass().add("card");
                root.setAlignment(Pos.CENTER_LEFT);
                root.setStyle("-fx-border-color: #8b5cf6;");
                root.prefWidthProperty().bind(lv.widthProperty().subtract(40));

                // Warn if file no longer exists on disk
                boolean fileExists = new File(item.getFilePath()).exists();
                Label icon = new Label(fileExists ? "📄" : "❌");
                icon.setStyle("-fx-font-size: 22px;");

                Label fname = new Label(item.getFileName());
                fname.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: "
                        + (fileExists ? "-color-text-main" : "#94a3b8") + ";"
                        + (fileExists ? "" : "-fx-strikethrough: true;"));

                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button open = actionBtn(fileExists ? "Open" : "Missing", fileExists ? "#94a3b8" : "#475569",
                        e -> {
                            if (!fileExists) {
                                HelloController.this.showToast("File no longer exists on disk.", true);
                                return;
                            }
                            openLocalFile(item.getFilePath());
                        });
                open.setStyle("-fx-background-color: " + (fileExists ? "#94a3b8" : "#475569")
                        + "; -fx-pref-height: 28; -fx-font-size: 11px;");
                open.setDisable(!fileExists);

                root.getChildren().addAll(icon, fname, spacer, open);

                if (isOwner) {
                    Button del = actionBtn("🗑", "#e74c3c", e -> {
                        if (HelloController.this.confirm("Delete File", null,
                                "Remove \"" + item.getFileName() + "\" from your portfolio?"))
                            CompletableFuture.runAsync(() -> {
                                try {
                                    DatabaseManager.deletePortfolioItem(item.getId());
                                    new File(item.getFilePath()).delete();
                                    Platform.runLater(HelloController.this::refreshMyPortfolio);
                                } catch (Exception ex) {
                                    Platform.runLater(() -> HelloController.this
                                            .showToast("Failed to delete portfolio item. Try again.", true));
                                }
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
        try {
            File f = new File(path);
            if (f.exists() && Desktop.isDesktopSupported())
                Desktop.getDesktop().open(f);
            else
                showToast("File not found.", true);
        } catch (Exception e) {
            showToast("Failed to open file.", true);
        }
    }
}