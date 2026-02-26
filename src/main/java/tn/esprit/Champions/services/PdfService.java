package tn.esprit.Champions.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PdfService {

    // MÉTHODE POUR LE REÇU (Utilisée lors du paiement)
    public static void genererRecuPaiement(String nomEtudiant, String nomFormation, double montant, String path) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();

            Font fontTitre = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, BaseColor.BLUE);
            Paragraph titre = new Paragraph("REÇU DE PAIEMENT\nCHAMPIONS ACADEMY", fontTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);

            document.add(new Paragraph("\n------------------------------------------------------------------\n"));
            document.add(new Paragraph("Date du paiement : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            document.add(new Paragraph("Étudiant : " + nomEtudiant));
            document.add(new Paragraph("Formation : " + nomFormation));
            document.add(new Paragraph("Montant payé : " + montant + " DT"));
            document.add(new Paragraph("\nStatut : Confirmé (Payé via Stripe)"));
            document.add(new Paragraph("\n------------------------------------------------------------------\n"));
            document.add(new Paragraph("\nCe document sert de preuve d'inscription officielle."));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MÉTHODE POUR LE CERTIFICAT (À utiliser pour la fin de formation)
    public static void genererCertificat(String nomEtudiant, String nomFormation, String path) {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();
            Font fontTitre = new Font(Font.FontFamily.HELVETICA, 40, Font.BOLD, BaseColor.ORANGE);
            Paragraph p1 = new Paragraph("CERTIFICAT DE RÉUSSITE", fontTitre);
            p1.setAlignment(Element.ALIGN_CENTER);
            document.add(p1);
            document.add(new Paragraph("\n\n"));
            Paragraph p2 = new Paragraph("Décerné à : " + nomEtudiant + "\nPour avoir terminé : " + nomFormation,
                    new Font(Font.FontFamily.HELVETICA, 20));
            p2.setAlignment(Element.ALIGN_CENTER);
            document.add(p2);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}