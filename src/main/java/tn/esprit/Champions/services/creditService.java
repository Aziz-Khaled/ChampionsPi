package tn.esprit.Champions.services;

import tn.esprit.Champions.models.CreditStatus;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.models.Utilisateur;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class creditService implements CRUD<credit> {

    private Connection cnx;

    public creditService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(credit c) throws SQLException {
        // On simplifie la requête pour une nouvelle demande (certains champs seront NULL par défaut)
        String req = "INSERT INTO `credit` (`project_id`, `borrower_id`, `montant`, `devise`, `taux`, `duree`, `description`, `status`, `date_demande`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setInt(1, c.getProject_id());
        pst.setInt(2, c.getBorrower_id());
        pst.setDouble(3, c.getMontant());
        pst.setString(4, c.getDevise());
        pst.setDouble(5, c.getTaux());
        pst.setInt(6, c.getDuree());
        pst.setString(7, c.getDescription());
        pst.setString(8, (c.getStatus() != null) ? c.getStatus().name() : "PENDING");

        // On génère la date de demande automatiquement si elle est nulle
        pst.setTimestamp(9, new java.sql.Timestamp(System.currentTimeMillis()));

        pst.executeUpdate();
        pst.close();
        System.out.println("Succès : Le crédit a été ajouté !");
    }

    @Override
    public void updateOne(credit credit) throws SQLException {
        // On ajoute tous les champs manquants dans la requête SQL
        String req = "UPDATE `credit` SET `project_id` = ?, `montant` = ?, `devise` = ?, " +
                "`taux` = ?, `duree` = ?, `description` = ?, `status` = ? " +
                "WHERE `id_credit` = ?";

        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setInt(1, credit.getProject_id());
        pst.setDouble(2, credit.getMontant());
        pst.setString(3, credit.getDevise());        // Ajouté
        pst.setDouble(4, credit.getTaux());          // Ajouté
        pst.setInt(5, credit.getDuree());           // Ajouté
        pst.setString(6, credit.getDescription());  // Ajouté

        // Gestion du Status (Enum -> String)
        pst.setString(7, (credit.getStatus() != null) ? credit.getStatus().name() : "OPEN");

        // L'ID pour le WHERE (doit être le dernier index : 8)
        pst.setInt(8, credit.getId());

        pst.executeUpdate();
        pst.close();
        System.out.println("Crédit mis à jour avec succès (tous les champs) !");
    }

    @Override
    public void deleteOne(credit credit) throws SQLException {
        // Sécurisation avec PreparedStatement au lieu de la concaténation
        String req = "DELETE FROM credit WHERE id_credit = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, credit.getId());
        pst.executeUpdate();
        pst.close();
        System.out.println("Crédit supprimé !");
    }

    @Override
    public List<credit> SelectAll() throws SQLException {
        List<credit> credits = new ArrayList<>();
        String req = "SELECT * FROM `credit`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            credit c = new credit();
            c.setId(rs.getInt("id_credit"));
            c.setProject_id(rs.getInt("project_id"));
            c.setBorrower_id(rs.getInt("borrower_id"));
            c.setInvestisseur_id(rs.getInt("investisseur_id"));
            c.setMontant(rs.getDouble("montant"));
            c.setDevise(rs.getString("devise"));
            c.setTaux(rs.getDouble("taux"));
            c.setDuree(rs.getInt("duree"));
            c.setDescription(rs.getString("description"));

            // CORRECTION ENUM : String -> Enum
            String statusDB = rs.getString("status");
            if (statusDB != null) {
                c.setStatus(CreditStatus.valueOf(statusDB));
            }

            c.setContrat_id(rs.getString("contrat_id"));
            c.setDate_demande(rs.getTimestamp("date_demande"));
            c.setDate_contrat(rs.getTimestamp("date_contrat"));

            credits.add(c);
        }
        rs.close();
        st.close();
        return credits;
    }
    public int getTotalCredits() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM `credit`";
        // Utilisation de 'cnx' (votre variable locale) et non 'Connection'
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage des crédits : " + e.getMessage());
            e.printStackTrace();
        }
        return count;
    }
    public List<Utilisateur> getAllUsersForCombo() {
        List<Utilisateur> list = new ArrayList<>();
        String req = "SELECT id_user, nom, prenom FROM utilisateur";
        try {
            // CORRECTION 1 : Utiliser 'cnx' au lieu de 'connection'
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                // CORRECTION 2 : Si le constructeur vide ne marche pas,
                // on utilise le constructeur à 8 arguments avec des valeurs par défaut/null
                // ou on ajoute un constructeur vide dans la classe Utilisateur (Recommandé).

                Utilisateur u = new Utilisateur(
                        rs.getInt("id_user"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        null, null, null, null, null // Valeurs null pour les champs inutiles ici
                );
                list.add(u);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            System.out.println("Erreur lecture utilisateurs : " + e.getMessage());
        }
        return list;
    }
}