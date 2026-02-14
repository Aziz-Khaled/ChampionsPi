package tn.esprit.Champions.utils;

import tn.esprit.Champions.models.Utilisateur;

public class UserSession {

    private static Utilisateur instance ;

    public static void setLoggedInUser(Utilisateur user) {
        instance = user;
    }

    public static Utilisateur getLoggedInUser() {
        return instance;
    }

    public static void clearSession() {
        instance = null;
    }

}
