// BarcodeFoodScannerApp.java
// A simple Java Swing app to scan barcodes from an image and fetch product info from OpenFoodFacts

// ----- Imports -----
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

// ----- Main class -----
public class BarcodeFoodScannerApp {

    // Constructor
    public BarcodeFoodScannerApp() {
    }

    // Main entry point
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BarcodeFoodScannerApp().createAndShowGUI());
    }

    // Create GUI window
    private void createAndShowGUI() {
        JFrame frame = new JFrame("ZXing Barcode + OpenFoodFacts Scanner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 700);
        frame.setLayout(new BorderLayout());

        // Label for showing uploaded image
        JLabel previewLabel = new JLabel("No image selected", JLabel.CENTER);
        previewLabel.setPreferredSize(new Dimension(400, 400));
        previewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Text area for displaying decoded barcode & product info
        JTextArea resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(resultArea);

        // Button to upload image
        JButton uploadButton = new JButton("Select Image");
        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedImage image = ImageIO.read(file);
                    if (image == null) {
                        JOptionPane.showMessageDialog(frame, "Invalid image file.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Show image preview
                    Image scaled = image.getScaledInstance(previewLabel.getWidth(), previewLabel.getHeight(), Image.SCALE_SMOOTH);
                    previewLabel.setIcon(new ImageIcon(scaled));
                    previewLabel.setText(null);

                    // Decode barcode
                    String barcodeText = this.decodeBarcode(image);
                    if (barcodeText == null) {
                        resultArea.setText("No barcode found in the image.");
                        return;
                    }

                    resultArea.setText("Decoded barcode: " + barcodeText + "\nFetching product info...");

                    // Lookup product in OpenFoodFacts
                    Map<String, String> productInfo = this.lookupOpenFoodFacts(barcodeText);
                    if (productInfo.isEmpty()) {
                        resultArea.append("\nProduct not found in OpenFoodFacts.");
                    } else {
                        resultArea.append("\n\n--- Product Info ---");
                        productInfo.forEach((key, value) -> resultArea.append("\n" + key + ": " + value));
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Add components to window
        frame.add(uploadButton, BorderLayout.NORTH);
        frame.add(previewLabel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    // Decode barcode from image using ZXing
    private String decodeBarcode(BufferedImage image) {
        try {
            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // Try all barcode formats
            EnumMap<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();

        } catch (NotFoundException e) {
            return null; // no barcode found
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Lookup product info from OpenFoodFacts API using barcode
    private Map<String, String> lookupOpenFoodFacts(String barcode) {
        Map<String, String> info = new LinkedHashMap<>();
        String apiUrl = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                info.put("Error", "HTTP status " + status);
                return info;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while((line = in.readLine()) != null) response.append(line);
            in.close();
            conn.disconnect();

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.toString());
            JsonNode product = root.path("product");

            if (product.isMissingNode() || root.path("status").asInt() != 1) return info;

            info.put("Product Name", product.path("product_name").asText("N/A"));
            info.put("Brands", product.path("brands").asText("N/A"));
            info.put("Ingredients", product.path("ingredients_text").asText("N/A"));
            info.put("Nutriscore", product.path("nutriscore_grade").asText("N/A"));

        } catch (Exception e) {
            e.printStackTrace();
            info.put("Error", e.getMessage());
        }

        return info;
    }
}
