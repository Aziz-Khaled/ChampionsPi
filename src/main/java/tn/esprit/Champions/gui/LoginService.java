package tn.esprit.Champions.gui;

import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.services.UtilisateurService;
import tn.esprit.Champions.utils.Auth0Config;
import tn.esprit.Champions.utils.UserSession;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;

public class LoginService {

    @FXML private Button btn_GoogleLogin;
    @FXML private Button btn_Login;
    @FXML private TextField txt_Email;
    @FXML private TextField txt_Password;

    private String codeVerifier;
    private HttpServer authServer;
    private UtilisateurService userService = new UtilisateurService();

    @FXML
    private void initialize() {
        btn_Login.setOnAction(e -> login());
        btn_GoogleLogin.setOnAction(e -> handleAuth0Login());
    }

    // --- AUTH0 / GOOGLE LOGIN LOGIC ---

    private void handleAuth0Login() {
        try {
            generatePKCE();
            String challenge = getCodeChallenge();

            // ADDED: &prompt=select_account to the end of the URL
            String authUrl = String.format(
                    "https://%s/authorize?response_type=code&client_id=%s&redirect_uri=%s" +
                            "&scope=openid%%20profile%%20email&connection=google-oauth2&state=random_state" +
                            "&code_challenge=%s&code_challenge_method=S256&prompt=select_account",
                    Auth0Config.DOMAIN,
                    Auth0Config.CLIENT_ID,
                    Auth0Config.REDIRECT_URI,
                    challenge
            );

            startLocalCallbackServer();

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new URI(authUrl));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Auth0 Error", "Could not initiate Google Login.");
        }
    }

    private void startLocalCallbackServer() throws Exception {
        if (authServer != null) return; // Already running

        authServer = HttpServer.create(new InetSocketAddress(4242), 0);
        authServer.createContext("/callback", exchange -> {
            try {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("code=")) {
                    String code = query.split("code=")[1].split("&")[0];

                    // Exchange code for user info
                    exchangeCodeForToken(code);

                    String response = "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                            "<h1>✅ Authenticated</h1><p>Returning to Champions Platform...</p></body></html>";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                }
            } finally {
                stopServer();
            }
        });

        authServer.setExecutor(null);
        authServer.start();
    }

    private void stopServer() {
        if (authServer != null) {
            authServer.stop(1);
            authServer = null;
        }
    }

    private void exchangeCodeForToken(String code) {
        try {
            com.auth0.client.auth.AuthAPI auth = com.auth0.client.auth.AuthAPI.newBuilder(
                    Auth0Config.DOMAIN, Auth0Config.CLIENT_ID, "").build();

            com.auth0.net.TokenRequest tokenRequest = auth.exchangeCode(code, Auth0Config.REDIRECT_URI);
            tokenRequest.addParameter("code_verifier", codeVerifier);

            com.auth0.json.auth.TokenHolder holder = tokenRequest.execute().getBody();

            com.auth0.net.Request<com.auth0.json.auth.UserInfo> infoRequest = auth.userInfo(holder.getAccessToken());
            com.auth0.json.auth.UserInfo info = infoRequest.execute().getBody();

            String googleEmail = (String) info.getValues().get("email");
            String googleName = (String) info.getValues().get("name");

            // Sync with DB
            Utilisateur authenticatedUser = userService.handleLocalUserSync(googleEmail, googleName);

            Platform.runLater(() -> {
                UserSession.setLoggedInUser(authenticatedUser);

                // --- KYC GATEKEEPER ---
                boolean isKycMissing = authenticatedUser.getPiece_identite() == null ||
                        authenticatedUser.getPiece_identite().equals("NOT_PROVIDED") ||
                        authenticatedUser.getTelephone() == null ||
                        authenticatedUser.getTelephone().equals("00000000");

                if (isKycMissing && authenticatedUser.getRole() != tn.esprit.Champions.models.Role.ADMIN) {
                    showAlert(Alert.AlertType.INFORMATION, "Verification Required", "Please complete your KYC to access the platform.");
                    openPage("/KYC_Setup.fxml", "Identity Verification");
                }
                else if (authenticatedUser.getStatut() == tn.esprit.Champions.models.Status.PENDING) {
                    showAlert(Alert.AlertType.WARNING, "Pending Approval", "Your account is currently under review by our team.");
                }
                else {
                    navigateToRoleDashboard(authenticatedUser);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Sync Error", "Failed to process Google login."));
        }
    }

    // --- STANDARD LOGIN LOGIC ---

    private void login() {
        String email = txt_Email.getText();
        String password = txt_Password.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields", "Please fill in all fields.");
            return;
        }

        try {
            Utilisateur user = userService.getUserByEmail(email);
            if (user != null && BCrypt.checkpw(password, user.getMot_de_passe())) {
                if (user.getStatut() == tn.esprit.Champions.models.Status.DESACTIVE) {
                    showAlert(Alert.AlertType.ERROR, "Account Disabled", "Your account has been suspended.");
                    return;
                }

                UserSession.setLoggedInUser(user);
                navigateToRoleDashboard(user);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid email or password.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // --- UTILS ---

    private void navigateToRoleDashboard(Utilisateur user) {
        if (user.getRole() == tn.esprit.Champions.models.Role.ADMIN) {
            openPage("/Admin.fxml", "Admin Panel");
        } else {
            openPage("/ClientPanel.fxml", "Client Panel");
        }
    }

    private void generatePKCE() {
        SecureRandom sr = new SecureRandom();
        byte[] code = new byte[32];
        sr.nextBytes(code);
        this.codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(code);
    }

    private String getCodeChallenge() throws Exception {
        byte[] bytes = codeVerifier.getBytes("US-ASCII");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(bytes, 0, bytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openPage(String fxmlPath, String title) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) btn_Login.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load page: " + title);
        }
    }
    @FXML
    private void openSignUpPage() {

        openPage("/SignUpPage.fxml", "Create Account");
    }

}