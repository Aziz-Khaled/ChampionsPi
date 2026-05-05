package tn.esprit.Champions.utils;

import tn.esprit.Champions.models.Utilisateur;

public class UserSession {

    private static Utilisateur loggedInUser;
    private static String jwtToken;

    // Updated to accept both the user and their token
    public static void setLoggedInUser(Utilisateur user, String token) {
        loggedInUser = user;
        jwtToken = token;
    }

    public static Utilisateur getLoggedInUser() {
        return loggedInUser;
    }

    public static String getJwtToken() {
        return jwtToken;
    }

    public static void clearSession() {
        loggedInUser = null;
        jwtToken = null;
    }
}