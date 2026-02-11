package tn.esprit.Champions.services;

import tn.esprit.Champions.models.certificats;
import tn.esprit.Champions.models.MentionCertificat;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CertificatService implements CRUD<certificats> {

    private Connection cnx;

    public CertificatService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    // ================= INSERT =================
    @Override
    public void insertOne(certificats c) throws SQLException {

        String sql = "INSERT INTO certificats " +
                "(idParticipation, dateEmission, codeVerification, mention, urlFichier) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setLong(1, c.getIdParticipation());
        ps.setDate(2, Date.valueOf(c.getDateEmission()));
        ps.setString(3, c.getCodeVerification());
        ps.setString(4, c.getMention().name());
        ps.setString(5, c.getUrlFichier());

        ps.executeUpdate();

        // Récupérer l’ID généré
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            c.setIdCertificat(rs.getInt(1));
        }
    }

    // ================= UPDATE =================
    @Override
    public void updateOne(certificats c) throws SQLException {

        String sql = "UPDATE certificats SET " +
                "idParticipation=?, dateEmission=?, codeVerification=?, mention=?, urlFichier=? " +
                "WHERE idCertificat=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setLong(1, c.getIdParticipation());
        ps.setDate(2, Date.valueOf(c.getDateEmission()));
        ps.setString(3, c.getCodeVerification());
        ps.setString(4, c.getMention().name());
        ps.setString(5, c.getUrlFichier());
        ps.setInt(6, c.getIdCertificat());

        ps.executeUpdate();
    }

    // ================= DELETE =================
    @Override
    public void deleteOne(certificats c) throws SQLException {

        String sql = "DELETE FROM certificats WHERE idCertificat=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, c.getIdCertificat());
        ps.executeUpdate();
    }

    // ================= SELECT ALL =================
    @Override
    public List<certificats> SelectAll() throws SQLException {

        List<certificats> list = new ArrayList<>();

        String sql = "SELECT * FROM certificats";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            certificats c = new certificats();
            c.setIdCertificat(rs.getInt("idCertificat"));
            c.setIdParticipation(rs.getLong("idParticipation"));
            c.setDateEmission(rs.getDate("dateEmission").toLocalDate());
            c.setCodeVerification(rs.getString("codeVerification"));
            c.setMention(MentionCertificat.valueOf(rs.getString("mention")));
            c.setUrlFichier(rs.getString("urlFichier"));

            list.add(c);
        }

        return list;
    }
}
