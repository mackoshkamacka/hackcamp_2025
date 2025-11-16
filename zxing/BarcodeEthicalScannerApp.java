/**
 * BarcodeEthicalScannerApp.java
 *
 * Full application:
 * - Scan barcode from image
 * - Fetch product info (OpenFoodFacts for food)
 * - Fetch ethical info (Good On You JSON for clothing/personal care)
 * - Fallback: Google Custom Search API for other products
 *
 * Dependencies:
 * ZXing (core + javase), Jackson (databind, core, annotations)
 *
 * Compile:
 * javac -cp ".:core-3.5.2.jar:javase-3.5.2.jar:jackson-databind-2.15.2.jar:jackson-core-2.15.2.jar:jackson-annotations-2.15.2.jar" BarcodeEthicalScannerApp.java
 *
 * Run:
 * java -cp ".:core-3.5.2.jar:javase-3.5.2.jar:jackson-databind-2.15.2.jar:jackson-core-2.15.2.jar:jackson-annotations-2.15.2.jar" BarcodeEthicalScannerApp
 */

 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.google.zxing.*;
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
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.Map;
 
 public class BarcodeEthicalScannerApp {
 
     private JFrame frame;
     private JLabel previewLabel;
     private JTextArea resultArea;
     private JButton selectButton;
 
     // Google Custom Search credentials
     private static final String GOOGLE_API_KEY = "AIzaSyDgkQ4RWoX1Kvn9GcusxSadR9B4PvVpuQo";
     private static final String GOOGLE_CSE_ID = "c05a0c9e0acc247d8";
 
     // Good On You JSON filename
     private static final String GOOD_ON_YOU_FILE = "goodonyou.json";
 
     public static void main(String[] args) {
         SwingUtilities.invokeLater(() -> new BarcodeEthicalScannerApp().createAndShowGUI());
     }
 
     private void createAndShowGUI() {
         frame = new JFrame("Barcode Ethical Scanner");
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         frame.setSize(800, 900);
         frame.setLayout(new BorderLayout());
 
         // Image preview
         previewLabel = new JLabel("No image selected", SwingConstants.CENTER);
         previewLabel.setPreferredSize(new java.awt.Dimension(500, 400));
         Border border = BorderFactory.createLineBorder(Color.GRAY);
         previewLabel.setBorder(border);
 
         // Results text area
         resultArea = new JTextArea(20, 50);
         resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
         resultArea.setEditable(false);
         JScrollPane scrollPane = new JScrollPane(resultArea);
 
         // Select image button
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
 
                     String category = productInfo.getOrDefault("Category", "unknown");
                     String brand = productInfo.getOrDefault("Brand", "");
 
                     // Ethical info
                     Map<String, String> ethicalInfo = lookupEthicalInfo(category, brand);
                     if (!ethicalInfo.isEmpty()) {
                         resultArea.append("\n\n--- Ethical Info ---");
                         ethicalInfo.forEach((k, v) -> resultArea.append("\n" + k + ": " + v));
                     } else {
                         // Fallback to Google Custom Search
                         Map<String, String> googleEthical = lookupEthicalViaGoogle(brand);
                         if (!googleEthical.isEmpty()) {
                             resultArea.append("\n\n--- Google Search Ethical Info ---");
                             googleEthical.forEach((k, v) -> resultArea.append("\n" + k + ": " + v));
                         } else {
                             resultArea.append("\n\nNo ethical info available for this product/brand.");
                         }
                     }
                 }
 
             } catch (IOException ex) {
                 ex.printStackTrace();
                 JOptionPane.showMessageDialog(frame, "Error reading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
             }
         }
     }
 
     // ZXing barcode decoding
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
 
     // OpenFoodFacts API lookup
     private Map<String, String> lookupProductInfo(String barcode) {
         Map<String, String> info = new LinkedHashMap<>();
         try {
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
             if (root.path("status").asInt() != 1) return info;
 
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
 
     // Lookup ethical info for food / clothing
     private Map<String, String> lookupEthicalInfo(String category, String brand) {
         Map<String, String> ethical = new LinkedHashMap<>();
         try {
             // Clothing / Personal Care from Good On You JSON
             if (category.toLowerCase().contains("clothing") || category.toLowerCase().contains("personal care")) {
                 File file = new File(GOOD_ON_YOU_FILE);
                 if (file.exists()) {
                     ObjectMapper mapper = new ObjectMapper();
                     JsonNode root = mapper.readTree(file);
                     JsonNode brandNode = root.path(brand);
                     if (!brandNode.isMissingNode()) {
                         ethical.put("GoodOnYou Rating", brandNode.path("rating").asText());
                         ethical.put("Comment", brandNode.path("comment").asText());
                     }
                 }
             }
 
             // Food ethical info
             if (category.toLowerCase().contains("food")) {
                 if (brand.toLowerCase().contains("organic") || brand.toLowerCase().contains("fair")) {
                     ethical.put("Ethical Status", "Organic / Fair Trade friendly");
                 } else {
                     ethical.put("Ethical Status", "Standard food product");
                 }
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
         return ethical;
     }
 
     // Fallback: Google Custom Search API lookup
     private Map<String, String> lookupEthicalViaGoogle(String brand) {
         Map<String, String> ethical = new LinkedHashMap<>();
         if (brand == null || brand.isEmpty()) return ethical;
 
         try {
             String query = brand.replace(" ", "+") + "+ethical+rating";
             String urlStr = "https://www.googleapis.com/customsearch/v1?q=" + query +
                     "&key=" + GOOGLE_API_KEY + "&cx=" + GOOGLE_CSE_ID;
             URL url = new URL(urlStr);
             HttpURLConnection conn = (HttpURLConnection) url.openConnection();
             conn.setRequestMethod("GET");
             conn.setConnectTimeout(5000);
             conn.setReadTimeout(5000);
 
             BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
             StringBuilder sb = new StringBuilder();
             String line;
             while ((line = reader.readLine()) != null) sb.append(line);
             reader.close();
             conn.disconnect();
 
             ObjectMapper mapper = new ObjectMapper();
             JsonNode root = mapper.readTree(sb.toString());
             JsonNode items = root.path("items");
             if (items.isArray() && items.size() > 0) {
                 JsonNode first = items.get(0);
                 String snippet = first.path("snippet").asText();
                 ethical.put("Google Search Snippet", snippet);
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
         return ethical;
     }
 }
 