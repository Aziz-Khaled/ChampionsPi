package tn.esprit.Champions.services;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import java.io.File;

public class IdentityAIService {

    private final Tesseract tesseract;

    public IdentityAIService() {
        this.tesseract = new Tesseract();

        // Pointing to your local tessdata folder
        File tessDataFolder = new File("src/main/resources/tessdata");

        if (tessDataFolder.exists()) {
            this.tesseract.setDatapath(tessDataFolder.getAbsolutePath());
            this.tesseract.setLanguage("eng+fra"); // Supports English and French
        } else {
            System.err.println("AI Error: tessdata folder not found!");
        }
    }

    /**
     * Reads the text from an ID card image.
     */
    public String extractTextFromID(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                return "ERROR: File not found";
            }
            return tesseract.doOCR(imageFile);
        } catch (TesseractException e) {
            //System.err.println("OCR Error: " + e.getMessage());
            return "OCR_FAILED";
        } catch (Exception e) {
            return "AI_UNAVAILABLE";
        }
    }

    /**
     * Compare the name from OCR with the database name.
     */
    public boolean verifyNameMatch(String idText, String dbName) {
        if (idText == null || dbName == null || idText.isEmpty()) return false;

        // Simple fuzzy check: checks if the name exists anywhere in the OCR text
        return idText.toLowerCase().contains(dbName.toLowerCase());
    }
}