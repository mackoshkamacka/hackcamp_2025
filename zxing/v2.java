// BarcodeGoogleScannerApp.java
// A simple Java Swing app to scan barcodes from an image and search on Google

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Desktop;
import java.net.URI;

public class BarcodeGoogleScannerApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BarcodeGoogleScannerApp().createAndShowGUI());
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("ZXing Barcode + Google Lookup");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new java.awt.BorderLayout());

        // Label to preview uploaded image
        JLabel previewLabel = new JLabel("No image selected", JLabel.CENTER);
        previewLabel.setPreferredSize(new java.awt.Dimension(400, 300)); // fully qualified
        previewLabel.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY));

        // Text area to show barcode info
        JTextArea resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        // Button to select image
        JButton uploadButton = new JButton("Select Image");
        uploadButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    BufferedImage image = ImageIO.read(file);
                    if (image == null) {
                        JOptionPane.showMessageDialog(frame, "Invalid image file.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Show preview
                    java.awt.Image scaled = image.getScaledInstance(previewLabel.getWidth(), previewLabel.getHeight(), java.awt.Image.SCALE_SMOOTH);
                    previewLabel.setIcon(new ImageIcon(scaled));
                    previewLabel.setText(null);

                    // Decode barcode
                    String barcodeText = decodeBarcode(image);
                    if (barcodeText == null) {
                        resultArea.setText("No barcode found in the image.");
                        return;
                    }

                    resultArea.setText("Decoded barcode: " + barcodeText + "\nOpening Google search...");

                    // Open Google search in default browser
                    String url = "https://www.google.com/search?q=" + barcodeText;
                    Desktop.getDesktop().browse(new URI(url));

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Add components to window
        frame.add(uploadButton, java.awt.BorderLayout.NORTH);
        frame.add(previewLabel, java.awt.BorderLayout.CENTER);
        frame.add(scrollPane, java.awt.BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    // Decode barcode from a BufferedImage using ZXing
    private String decodeBarcode(BufferedImage image) {
        try {
            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

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
}
