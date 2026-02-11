package tn.esprit.Champions.services;

import tn.esprit.Champions.models.participations;
import tn.esprit.Champions.models.StatutParticipation;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService implements CRUD<participations> {

    private Connection cnx;

    public ParticipationService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    // ================= INSERT =================
    @Override
    public void insertOne(participations p) throws SQLException {
        String sql = "INSERT INTO participations " +
                "(idFormation, idUtilisateur, dateInscription, statut, presence, note) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, p.getIdFormation());
        ps.setInt(2, p.getIdUtilisateur());
        ps.setTimestamp(3, Timestamp.valueOf(p.getDateInscription()));
        ps.setString(4, p.getStatut().name());
        ps.setBoolean(5, p.isPresence());
        ps.setFloat(6, p.getNote());

        ps.executeUpdate();

        // Récupérer l'ID généré
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            p.setIdParticipation(rs.getInt(1));
        }
    }

    // ================= UPDATE =================
    @Override
    public void updateOne(participations p) throws SQLException {
        String sql = "UPDATE participations SET " +
                "idFormation=?, idUtilisateur=?, dateInscription=?, statut=?, presence=?, note=? " +
                "WHERE idParticipation=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, p.getIdFormation());
        ps.setInt(2, p.getIdUtilisateur());
        ps.setTimestamp(3, Timestamp.valueOf(p.getDateInscription()));
        ps.setString(4, p.getStatut().name());
        ps.setBoolean(5, p.isPresence());
        ps.setFloat(6, p.getNote());
        ps.setInt(7, p.getIdParticipation());

        ps.executeUpdate();
    }

    // ================= DELETE =================
    @Override
    public void deleteOne(participations p) throws SQLException {
        String sql = "DELETE FROM participations WHERE idParticipation=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, p.getIdParticipation());
        ps.executeUpdate();
    }

    // ================= SELECT ALL =================
    @Override
    public List<participations> SelectAll() throws SQLException {
        List<participations> list = new ArrayList<>();

        String sql = "SELECT * FROM participations";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            participations p = new participations();
            p.setIdParticipation(rs.getInt("idParticipation"));
            p.setIdFormation(rs.getInt("idFormation"));
            p.setIdUtilisateur(rs.getInt("idUtilisateur"));
            p.setDateInscription(rs.getTimestamp("dateInscription").toLocalDateTime());
            p.setStatut(StatutParticipation.valueOf(rs.getString("statut")));
            p.setPresence(rs.getBoolean("presence"));
            p.setNote(rs.getFloat("note"));

            list.add(p);
        }

        return list;
    }
}
