package tn.esprit.Champions.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.util.Map;

public class CloudinaryService {

    // Informations de configuration
    private static final String CLOUD_NAME = "dwdvyr2gc";
    private static final String API_KEY = "284219136459467";
    private static final String API_SECRET = "66qrNsE0D_BO5E" + "htF4fvYQJhqkE";

    // Initialisation de l'objet Cloudinary
    private static final Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", CLOUD_NAME,
            "api_key", API_KEY,
            "api_secret", API_SECRET,
            "secure", true // Force l'utilisation du HTTPS pour toutes les opérations
    ));

    /**
     * Upload un fichier sur Cloudinary et retourne son URL sécurisée.
     * @final : Utilise resource_type 'raw' pour éviter les restrictions sur les PDF.
     */
    public static String uploadFile(File file) {
        try {
            // Configuration de l'upload
            Map uploadParams = ObjectUtils.asMap(
                    "resource_type", "raw",   // Indispensable pour les fichiers PDF/ZIP
                    "access_mode", "public",  // Assure que le fichier est consultable par tout le monde
                    "use_filename", true,     // Garde une trace du nom d'origine
                    "unique_filename", true   // Ajoute un suffixe pour éviter les doublons
            );

            // Exécution de l'upload
            Map uploadResult = cloudinary.uploader().upload(file, uploadParams);

            // On retourne le lien HTTPS (secure_url)
            if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                return uploadResult.get("secure_url").toString();
            }

            return null;

        } catch (Exception e) {
            System.err.println("ERREUR CLOUDINARY : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}