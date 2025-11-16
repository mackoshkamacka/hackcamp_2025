import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

public class BarcodeEthicalScannerApp extends JFrame {

    private JLabel previewLabel;
    private JTextArea resultArea;
    private JButton scanButton;

    // ---- TODO: insert your BarcodeLookup API key here ----
    private static final String BARCODE_LOOKUP_API_KEY = "oxutdjyhjspmaocz1np90n18rn319y";

    public BarcodeEthicalScannerApp() {
        super("Barcode Ethical Scanner");

        previewLabel = new JLabel("Choose an image to scan", SwingConstants.CENTER);
        previewLabel.setPreferredSize(new Dimension(500, 400));

        resultArea = new JTextArea();
        resultArea.setEditable(false);

        scanButton = new JButton("Scan Barcode");
        scanButton.addActionListener(new ScanAction());

        add(previewLabel, BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
        add(scanButton, BorderLayout.SOUTH);

        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // ---------------------------------------------------------
    // 1. IMAGE → BARCODE DECODER
    // ---------------------------------------------------------
    private String decodeBarcode(String imagePath) {
        try {
            File file = new File(imagePath);
            var bufferedImage = ImageIO.read(file);

            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ---------------------------------------------------------
    // 2. BARCODE → MANUFACTURER (BarcodeLookup API)
    // ---------------------------------------------------------
    private String searchManufacturerByBarcode(String barcode) {
        try {
            String apiUrl = "https://api.barcodelookup.com/v3/products?barcode=" + barcode
                    + "&formatted=y&key=" + BARCODE_LOOKUP_API_KEY;

            Document jsonDoc = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonDoc.text());
            JsonNode products = root.get("products");

            if (products != null && products.isArray() && products.size() > 0) {
                JsonNode product = products.get(0);
                if (product.has("manufacturer")) {
                    return product.get("manufacturer").asText();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ---------------------------------------------------------
    // 3. ETHICAL CONSUMER SEARCH → RETURN FIRST RESULT
    // ---------------------------------------------------------
    private String searchEthicalConsumerTopHit(String brand) {
        try {
            // Encode brand properly for URLs
            String encodedBrand = java.net.URLEncoder.encode(brand, "UTF-8");
            String searchUrl = "https://www.ethicalconsumer.org/search?keywords=" + encodedBrand;
            System.out.println(searchUrl);
    
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(12000)
                    .get();
            System.out.println(doc);
            // Select the first result reliably
            Element topLink = doc.selectFirst(".search-result__title a");
            if (topLink != null) {
                return topLink.text() + " -> https://www.ethicalconsumer.org" + topLink.attr("href");
            }
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return null;
    }
    

    // ---------------------------------------------------------
    // 4. OPENFOODFACTS (EXTRA INFO)
    // ---------------------------------------------------------
    private String getOpenFoodFactsInfo(String barcode) {
        try {
            String apiUrl = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";

            Document jsonDoc = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .timeout(8000)
                    .get();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonDoc.text());

            if (!root.has("product")) return "No OpenFoodFacts data.";

            JsonNode product = root.get("product");

            StringBuilder sb = new StringBuilder();
            sb.append("--- OpenFoodFacts Info ---\n");

            if (product.has("product_name"))
                sb.append("Product Name: ").append(product.get("product_name").asText()).append("\n");

            if (product.has("brands"))
                sb.append("Brands: ").append(product.get("brands").asText()).append("\n");

            if (product.has("ingredients_text"))
                sb.append("Ingredients: ").append(product.get("ingredients_text").asText()).append("\n");

            if (product.has("nutriscore_grade"))
                sb.append("Nutriscore: ").append(product.get("nutriscore_grade").asText()).append("\n");

            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OpenFoodFacts error.";
    }

    // ---------------------------------------------------------
    // 5. GUI SCAN BUTTON ACTION
    // ---------------------------------------------------------
    private class ScanAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {

            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(null);

            if (result != JFileChooser.APPROVE_OPTION) return;

            File file = chooser.getSelectedFile();

            previewLabel.setIcon(new ImageIcon(
                    new ImageIcon(file.getAbsolutePath())
                            .getImage()
                            .getScaledInstance(500, 400, Image.SCALE_SMOOTH)));

            resultArea.setText("Decoding barcode...\n");

            String barcode = BarcodeEthicalScannerApp.this.decodeBarcode(file.getAbsolutePath());

            if (barcode == null) {
                resultArea.append("No barcode detected.\n");
                return;
            }

            resultArea.append("Decoded barcode: " + barcode + "\n\n");

            // 1. OpenFoodFacts
            resultArea.append(BarcodeEthicalScannerApp.this.getOpenFoodFactsInfo(barcode) + "\n");

            // 2. Manufacturer lookup
            String manufacturer = BarcodeEthicalScannerApp.this.searchManufacturerByBarcode(barcode);
            resultArea.append("Manufacturer: " + manufacturer + "\n");

            // 3. Ethical Consumer
            String ethical = (manufacturer != null)
                    ? BarcodeEthicalScannerApp.this.searchEthicalConsumerTopHit(manufacturer)
                    : null;

            if (ethical != null) {
                resultArea.append("\nEthicalConsumer Top Match: " + ethical + "\n");
            } else {
                resultArea.append("\nNo EthicalConsumer results found. Consider searching manually for: "
                        + manufacturer + " ethical rating.\n");
            }
        }
    }

    // ---------------------------------------------------------
    // MAIN
    // ---------------------------------------------------------
    public static void main(String[] args) {
        new BarcodeEthicalScannerApp();
    }
}
