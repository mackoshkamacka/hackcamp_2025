/**
 * BarcodeEthicalScannerApp.java
 *
 * A Java Swing application to:
 * 1. Upload an image
 * 2. Decode the barcode (1D/2D) using ZXing
 * 3. Lookup product info from OpenFoodFacts / Barcode Lookup API
 * 4. Display product name, brand, category, and ethical info
 *
 * Dependencies:
 * - ZXing core + javase jars
 * - Jackson Databind, Core, Annotations for JSON parsing
 * 
 * Compile example:
 * javac -cp ".:core-3.5.2.jar:javase-3.5.2.jar:jackson-databind-2.15.2.jar:jackson-core-2.15.2.jar:jackson-annotations-2.15.2.jar" BarcodeEthicalScannerApp.java
 *
 * Run example:
 * java -cp ".:core-3.5.2.jar:javase-3.5.2.jar:jackson-databind-2.15.2.jar:jackson-core-2.15.2.jar:jackson-annotations-2.15.2.jar" BarcodeEthicalScannerApp
 */

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
 
 import javax.imageio.ImageIO;
 import javax.swing.*;
 import javax.swing.border.Border;
 import java.awt.*;
 import java.awt.event.ActionEvent;
 import java.awt.image.BufferedImage;
 import java.io.*;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.util.EnumMap;
 import java.util.EnumSet;
 import java.util.LinkedHashMap;
 import java.util.Map;
 
 public class BarcodeEthicalScannerApp {
 
     private JFrame frame;
     private JLabel previewLabel;
     private JTextArea resultArea;
     private JButton selectButton;
 
     public static void main(String[] args) {
         SwingUtilities.invokeLater(() -> new BarcodeEthicalScannerApp().createAndShowGUI());
     }
 
     private void createAndShowGUI() {
         frame = new JFrame("Barcode Ethical Scanner");
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         frame.setSize(700, 800);
         frame.setLayout(new BorderLayout());
 
         // Label to preview uploaded image
         previewLabel = new JLabel("No image selected", SwingConstants.CENTER);
         previewLabel.setPreferredSize(new java.awt.Dimension(500, 400)); // fixed ambiguity
         Border border = BorderFactory.createLineBorder(Color.GRAY);
         previewLabel.setBorder(border);
 
         // Text area to show results
         resultArea = new JTextArea(15, 50);
         resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
         resultArea.setEditable(false);
         JScrollPane scrollPane = new JScrollPane(resultArea);
 
         // Button to select image
         selectButton = new JButton("Select Image");
         selectButton.addActionListener(this::handleSelectImage);
 
         frame.add(selectButton, BorderLayout.NORTH);
         frame.add(previewLabel, BorderLayout.CENTER);
         frame.add(scrollPane, BorderLayout.SOUTH);
 
         frame.setVisible(true);
     }
 
     private void handleSelectImage(ActionEvent e) {
         JFileChooser chooser = new JFileChooser();
         int choice = chooser.showOpenDialog(frame);
         if (choice == JFileChooser.APPROVE_OPTION) {
             File file = chooser.getSelectedFile();
             try {
                 BufferedImage img = ImageIO.read(file);
                 if (img == null) {
                     JOptionPane.showMessageDialog(frame, "Invalid image file.", "Error", JOptionPane.ERROR_MESSAGE);
                     return;
                 }
                 previewLabel.setIcon(new ImageIcon(img.getScaledInstance(previewLabel.getWidth(), previewLabel.getHeight(), Image.SCALE_SMOOTH)));
                 previewLabel.setText("");
 
                 String barcode = decodeBarcode(img);
                 if (barcode == null) {
                     resultArea.setText("No barcode found in the image.");
                     return;
                 }
 
                 resultArea.setText("Decoded barcode: " + barcode + "\nFetching product info...");
 
                 // Lookup product info
                 Map<String, String> productInfo = lookupProductInfo(barcode);
 
                 if (productInfo.isEmpty()) {
                     resultArea.append("\nProduct not found in database.");
                 } else {
                     resultArea.append("\n\n--- Product Info ---");
                     productInfo.forEach((k, v) -> resultArea.append("\n" + k + ": " + v));
 
                     // Fetch ethical info based on category or brand
                     String category = productInfo.getOrDefault("Category", "unknown");
                     String brand = productInfo.getOrDefault("Brand", "");
 
                     Map<String, String> ethicalInfo = lookupEthicalInfo(category, brand);
                     if (!ethicalInfo.isEmpty()) {
                         resultArea.append("\n\n--- Ethical Info ---");
                         ethicalInfo.forEach((k, v) -> resultArea.append("\n" + k + ": " + v));
                     } else {
                         resultArea.append("\n\nNo ethical info available for this product/brand.");
                     }
                 }
 
             } catch (IOException ex) {
                 ex.printStackTrace();
                 JOptionPane.showMessageDialog(frame, "Error reading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
             }
         }
     }
 
     // Decode barcode using ZXing
     private String decodeBarcode(BufferedImage image) {
         try {
             BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
             BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
             EnumMap<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
             hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
             hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
 
             Result result = new MultiFormatReader().decode(bitmap, hints);
             return result.getText();
         } catch (NotFoundException e) {
             return null;
         } catch (Exception e) {
             e.printStackTrace();
             return null;
         }
     }
 
     // Lookup product info from OpenFoodFacts or Barcode Lookup API
     private Map<String, String> lookupProductInfo(String barcode) {
         Map<String, String> info = new LinkedHashMap<>();
         try {
             // Example: OpenFoodFacts API
             String urlStr = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";
             URL url = new URL(urlStr);
             HttpURLConnection conn = (HttpURLConnection) url.openConnection();
             conn.setRequestMethod("GET");
             conn.setConnectTimeout(5000);
             conn.setReadTimeout(5000);
 
             if (conn.getResponseCode() != 200) {
                 info.put("Error", "HTTP " + conn.getResponseCode());
                 return info;
             }
 
             BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
             StringBuilder sb = new StringBuilder();
             String line;
             while ((line = reader.readLine()) != null) sb.append(line);
             reader.close();
             conn.disconnect();
 
             ObjectMapper mapper = new ObjectMapper();
             JsonNode root = mapper.readTree(sb.toString());
 
             if (root.path("status").asInt() != 1) return info; // product not found
 
             JsonNode product = root.path("product");
             info.put("Product Name", product.path("product_name").asText("N/A"));
             info.put("Brand", product.path("brands").asText("N/A"));
             info.put("Category", product.path("categories").asText("N/A"));
             info.put("Labels", product.path("labels").asText("N/A"));
             info.put("Ingredients Info", product.path("ingredients_text").asText("N/A"));
 
         } catch (Exception e) {
             e.printStackTrace();
             info.put("Error", e.getMessage());
         }
         return info;
     }
 
     // Lookup ethical info for brand/category (demo with mock data)
     private Map<String, String> lookupEthicalInfo(String category, String brand) {
         Map<String, String> ethical = new LinkedHashMap<>();
 
         if (category.toLowerCase().contains("food")) {
             if (brand.toLowerCase().contains("organic") || brand.toLowerCase().contains("fair")) {
                 ethical.put("Ethical Status", "Organic / Fair Trade friendly");
             } else {
                 ethical.put("Ethical Status", "Standard food product");
             }
         }
 
         if (category.toLowerCase().contains("clothing") || category.toLowerCase().contains("personal care")) {
             ethical.put("GoodOnYou Rating", "B (somewhat ethical brand)"); // demo
         }
 
         if (ethical.isEmpty() && !brand.isEmpty()) {
             ethical.put("Ethical Status", "No data available; please verify manually");
         }
 
         return ethical;
     }
 }
 