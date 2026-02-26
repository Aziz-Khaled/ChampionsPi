package tn.esprit.Champions.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import tn.esprit.Champions.models.Order;
import tn.esprit.Champions.models.OrderItem;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;

public class PdfService {

    

    public void generateReceipt(Order order, List<OrderItem> items, String filePath) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Paragraph title = new Paragraph("Reçu de Paiement ChampionsPi", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Référence Commande: " + order.getId()));
        document.add(new Paragraph("Date: " + order.getOrderDate()));
        document.add(new Paragraph("Adresse: " + order.getShippingAddress()));
        document.add(new Paragraph("Téléphone: " + order.getPhoneNumber()));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.addCell("Produit");
        table.addCell("Quantité");
        table.addCell("Prix Unitaire");
        table.addCell("Sous-total");

        for (OrderItem item : items) {
            table.addCell(item.getProduct().getName());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(item.getUnitPrice() + " BTC");
            table.addCell(item.getSubTotal() + " BTC");
        }
        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Total Payé: " + order.getTotalAmount() + " BTC", titleFont));
        document.add(new Paragraph(" "));

        // QR Code
        String qrContent = "Order ID: " + order.getId() + "\nTotal: " + order.getTotalAmount() + " BTC\nStatus: PAID";
        byte[] pngData = generateQRCodeImage(qrContent);

        Image qrImage = Image.getInstance(pngData);
        qrImage.setAlignment(Element.ALIGN_CENTER);
        document.add(qrImage);

        document.close();
    }

    public byte[] generateQRCodeImage(String content) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}
