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

            // =========================
            // Initialisation connexion
            // =========================
            DbConnection.getInstance();

            // =========================
            // TEST FORMATION
            // =========================
            FormationService fs = new FormationService();

            System.out.println("===== AVANT INSERT FORMATION =====");
            fs.SelectAll().forEach(System.out::println);

            formations f = new formations(
                    "Python pour debutants",
                    0,
                    "Backend ",
                    "Informatique",
                    LocalDate.of(2026, 4, 5),
                    LocalDate.of(2026, 4, 17),
                    500,
                    25,
                    "OUVERTE"
            );

            fs.insertOne(f);

            // Récupérer l’ID généré
            List<formations> listFormations = fs.SelectAll();
            formations insertedFormation = listFormations.get(listFormations.size() - 1);
            f.setIdFormation(insertedFormation.getIdFormation());

            System.out.println("===== APRES INSERT FORMATION =====");
            fs.SelectAll().forEach(System.out::println);

            // UPDATE FORMATION
            f.setTitre("Trading");
            f.setPrix(800);
            f.setDescription("Backend Java Avancé");
            fs.updateOne(f);

            System.out.println("===== APRES UPDATE FORMATION =====");
            fs.SelectAll().forEach(System.out::println);

            // =========================
            // TEST PARTICIPATION
            // =========================
            ParticipationService ps = new ParticipationService();

            System.out.println("===== AVANT INSERT PARTICIPATION =====");
            ps.SelectAll().forEach(System.out::println);

            participations p = new participations(
                    0,                  // idParticipation temporaire
                    f.getIdFormation(), // idFormation inséré
                    1,                  // idUtilisateur existant
                    LocalDateTime.now(),
                    "PAYEE",            // statut
                    false,
                    0f
            );

            ps.insertOne(p);

            // Récupérer l’ID généré
            List<participations> listParticipations = ps.SelectAll();
            participations lastParticipation = listParticipations.get(listParticipations.size() - 1);
            p.setIdParticipation(lastParticipation.getIdParticipation());

            System.out.println("===== APRES INSERT PARTICIPATION =====");
            ps.SelectAll().forEach(System.out::println);

            // UPDATE PARTICIPATION
            p.setPresence(true);
            p.setNote(18f);
            ps.updateOne(p);

            System.out.println("===== APRES UPDATE PARTICIPATION =====");
            ps.SelectAll().forEach(System.out::println);

            // =========================
            // TEST CERTIFICAT
            // =========================
            CertificatService cs = new CertificatService();

            System.out.println("===== AVANT INSERT CERTIFICAT =====");
            cs.SelectAll().forEach(System.out::println);

            certificats c = new certificats(
                    0,
                    (long) p.getIdParticipation(), // FK valide
                    LocalDate.now(),
                    "CODE123456",
                    "EXCELLENT",                   // MentionCertificat (doit exister dans ton enum)
                    "https://urlfichier.com/certificat.pdf"
            );

            cs.insertOne(c);

            System.out.println("===== APRES INSERT CERTIFICAT =====");
            cs.SelectAll().forEach(System.out::println);

            // UPDATE CERTIFICAT
            c.setUrlFichier("https://urlfichier.com/certificat_modifie.pdf");
            cs.updateOne(c);

            System.out.println("===== APRES UPDATE CERTIFICAT =====");
            cs.SelectAll().forEach(System.out::println);

            // DELETE CERTIFICAT
            /*cs.deleteOne(c);

            System.out.println("===== APRES DELETE CERTIFICAT =====");
            cs.SelectAll().forEach(System.out::println);*/

            // =========================
            // DELETE PARTICIPATION
            // =========================
            ps.deleteOne(p);

            System.out.println("===== APRES DELETE PARTICIPATION =====");
            ps.SelectAll().forEach(System.out::println);

            // =========================
            // DELETE FORMATION
            // =========================
            fs.deleteOne(f);

            System.out.println("===== APRES DELETE FORMATION =====");
            fs.SelectAll().forEach(System.out::println);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
