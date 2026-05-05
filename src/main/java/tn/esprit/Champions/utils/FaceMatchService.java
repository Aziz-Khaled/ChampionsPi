package tn.esprit.Champions.utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;


import java.io.File;

public class FaceMatchService {

    private final String API_KEY = "IaRF-JlNjC5MDVYBftiapYl4Bmd1Ou0V";
    private final String API_SECRET = "dg8DTQUFWs1B35ebHuWcIbvg-CysFPbJ";
    private final String URL = "https://api-us.faceplusplus.com/facepp/v3/compare";

    public float compareFaces(File sourceFile, File targetFile) {
        if (sourceFile == null || targetFile == null || !sourceFile.exists() || !targetFile.exists()) {
            return 0f;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost uploadFile = new HttpPost(URL);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("api_key", API_KEY);
            builder.addTextBody("api_secret", API_SECRET);

            // We use 'FileBody' to send the actual image data
            builder.addPart("image_file1", new FileBody(sourceFile, ContentType.create(getMimeType(sourceFile)), sourceFile.getName()));
            builder.addPart("image_file2", new FileBody(targetFile, ContentType.create(getMimeType(targetFile)), targetFile.getName()));

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(uploadFile)) {
                String result = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(result);

                // Debug: Print the full JSON to the console so you can see what Face++ is thinking
                System.out.println("Face++ Raw Response: " + result);

                if (json.has("confidence")) {
                    return (float) json.getDouble("confidence");
                }

                // If Face++ couldn't find a face in one of the images
                if (json.has("error_message") && json.getString("error_message").contains("NO_FACE_FOUND")) {
                    System.err.println("AI Warning: Face not detected in one of the images. Ensure photos are clear.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0f;
    }
    private String getMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".bmp")) return "image/bmp";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".tiff") || name.endsWith(".tif")) return "image/tiff";
        return "image/jpeg"; // default fallback
    }
}