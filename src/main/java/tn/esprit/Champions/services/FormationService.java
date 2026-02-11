package tn.esprit.Champions.services;

import tn.esprit.Champions.models.formations;
import tn.esprit.Champions.models.StatutFormation;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FormationService implements CRUD<formations> {
    private Connection cnx;

    public FormationService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    @Override
    public void insertOne(formations f) throws SQLException {

        String sql = "INSERT INTO formations " +
                "(titre, description, domaine, dateDebut, dateFin, prix, capaciteMax, statut, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, f.getTitre());
        ps.setString(2, f.getDescription());
        ps.setString(3, f.getDomaine());
        ps.setDate(4, Date.valueOf(f.getDateDebut()));
        ps.setDate(5, Date.valueOf(f.getDateFin()));
        ps.setDouble(6, f.getPrix());
        ps.setInt(7, f.getCapaciteMax());
        ps.setString(8, f.getStatut().name());

//  OBLIGATOIRE
        ps.setLong(9, 1); // user_id EXISTANT dans la table users

        ps.executeUpdate();

    }
    @Override
    public void updateOne(formations f) throws SQLException {

        String sql = "UPDATE formations SET " +
                "titre=?, description=?, domaine=?, dateDebut=?, dateFin=?, prix=?, capaciteMax=?, statut=? " +
                "WHERE idFormation=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, f.getTitre());
        ps.setString(2, f.getDescription());
        ps.setString(3, f.getDomaine());
        ps.setDate(4, Date.valueOf(f.getDateDebut()));
        ps.setDate(5, Date.valueOf(f.getDateFin()));
        ps.setDouble(6, f.getPrix());
        ps.setInt(7, f.getCapaciteMax());
        ps.setString(8, f.getStatut().name());
        ps.setLong(9, f.getIdFormation());

        ps.executeUpdate();
    }

    @Override
    public void deleteOne(formations f) throws SQLException {

        String sql = "DELETE FROM formations WHERE idFormation=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setLong(1, f.getIdFormation());

        ps.executeUpdate();
    }
    @Override
    public List<formations> SelectAll() throws SQLException {

        List<formations> list = new ArrayList<>();

        String sql = "SELECT * FROM formations";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            formations f = new formations();

            // ✅ CAST ICI
            f.setIdFormation((int) rs.getLong("idFormation"));

            f.setTitre(rs.getString("titre"));
            f.setDescription(rs.getString("description"));
            f.setDomaine(rs.getString("domaine"));
            f.setDateDebut(rs.getDate("dateDebut").toLocalDate());
            f.setDateFin(rs.getDate("dateFin").toLocalDate());
            f.setPrix(rs.getDouble("prix"));
            f.setCapaciteMax(rs.getInt("capaciteMax"));

            f.setStatut(
                    StatutFormation.valueOf(rs.getString("statut"))
            );

            list.add(f);
        }

        return list;
    }


}