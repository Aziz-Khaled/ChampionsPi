package tn.esprit.Champions.test;

import tn.esprit.Champions.models.*;
import tn.esprit.Champions.services.CertificatService;
import tn.esprit.Champions.services.FormationService;
import tn.esprit.Champions.services.ParticipationService;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Test {

    public static void main(String[] args) {

        try {
            // Initialisation connexion
            DbConnection.getInstance();

            // =========================
            // TEST FORMATION
            // =========================
            FormationService fs = new FormationService();

            formations f = new formations(
                    "Python pour debutants",
                    0,
                    "Backend ",
                    "Informatique",
                    LocalDate.of(2026, 4, 5),
                    LocalDate.of(2026, 4, 17),
                    500,
                    25,
                    "OUVERTE" // Si ton modèle formations utilise String, garde ça.
                    // Sinon, utilise l'Enum correspondant.
            );

            fs.insertOne(f);
            List<formations> listFormations = fs.SelectAll();
            formations insertedFormation = listFormations.get(listFormations.size() - 1);
            f.setIdFormation(insertedFormation.getIdFormation());

            // =========================
            // TEST PARTICIPATION (CORRIGÉ)
            // =========================
            ParticipationService ps = new ParticipationService();

            System.out.println("===== INSERT PARTICIPATION =====");

            // CORRECTION : "PAYEE" devient StatutParticipation.PAYEE
            participations p = new participations(
                    0,
                    f.getIdFormation(),
                    1,
                    LocalDateTime.now(),
                    StatutParticipation.PAYEE, // <--- ICI : Utilisation de l'ENUM
                    false,
                    0f
            );

            ps.insertOne(p);

            List<participations> listParticipations = ps.SelectAll();
            participations lastParticipation = listParticipations.get(listParticipations.size() - 1);
            p.setIdParticipation(lastParticipation.getIdParticipation());

            // =========================
            // TEST CERTIFICAT (CORRIGÉ)
            // =========================
            CertificatService cs = new CertificatService();

            System.out.println("===== INSERT CERTIFICAT =====");

            // CORRECTION : "EXCELLENT" devient MentionCertificat.EXCELLENT
            certificats c = new certificats(
                    0,
                    (long) p.getIdParticipation(),
                    LocalDate.now(),
                    "CODE123456",
                    MentionCertificat.EXCELLENT,   // <--- ICI : Utilisation de l'ENUM
                    "https://urlfichier.com/certificat.pdf"
            );

            cs.insertOne(c);
            System.out.println("Certificat inséré avec succès !");

            // =========================
            // NETTOYAGE (Optionnel)
            // =========================
            // ps.deleteOne(p);
            // fs.deleteOne(f);

        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Erreur de type (Enum) : " + e.getMessage());
        }
    }
}