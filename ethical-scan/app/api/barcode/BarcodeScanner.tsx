'use client';

import { useState, useRef } from 'react';
import { BrowserMultiFormatReader, NotFoundException } from '@zxing/library';
import styles from './BarcodeScanner.module.css';
import './BarcodeScanner.module.css'; 

import Cheerio from "./media/bcCHEERIOS.png"; 
import Hellman from "./media/bcHELLMAN.jpg"; 
import Nutella from "./media/bcNUTELLA.png"; 
import Philadelphia from "./media/bcPHILADELPHIA.png"; 
import Silk from "./media/bcSILK.png"; 


interface ScanResult {
  barcode: string;
  openFoodFacts?: {
    productName?: string;
    brands?: string;
    ingredients?: string;
    nutriscore?: string;
  };
  manufacturer?: string;
  ethicalConsumer?: {
    title: string;
    url: string;
  };
}

export default function BarcodeScanner() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [scanning, setScanning] = useState(false);
  const [result, setResult] = useState<ScanResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [scanLog, setScanLog] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const addLog = (message: string) => {
    setScanLog(prev => [...prev, `${new Date().toLocaleTimeString()}: ${message}`]);
  };

  // ---------------------------------------------------------
  // 1. IMAGE → BARCODE DECODER (using @zxing/library)
  // ---------------------------------------------------------
  const decodeBarcode = async (file: File): Promise<string | null> => {
    try {
      addLog('Decoding barcode from image...');
      const codeReader = new BrowserMultiFormatReader();
      
      const imageUrl = URL.createObjectURL(file);
      const result = await codeReader.decodeFromImageUrl(imageUrl);
      
      URL.revokeObjectURL(imageUrl);
      addLog(`Barcode detected: ${result.getText()}`);
      return result.getText();
    } catch (err) {
      if (err instanceof NotFoundException) {
        addLog('No barcode found in image');
      } else {
        addLog(`Error decoding: ${err}`);
      }
      return null;
    }
  };

  // ---------------------------------------------------------
  // 2. BARCODE → MANUFACTURER (via Next.js API route)
  // ---------------------------------------------------------
  const searchManufacturer = async (barcode: string): Promise<string | null> => {
    try {
      addLog('Looking up manufacturer...');
      const response = await fetch('/api/barcode-lookup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ barcode }),
      });

      if (!response.ok) {
        addLog('Manufacturer lookup failed');
        return null;
      }

      const data = await response.json();
      if (data.manufacturer) {
        addLog(`Manufacturer found: ${data.manufacturer}`);
      } else {
        addLog('No manufacturer data available');
      }
      return data.manufacturer || null;
    } catch (err) {
      addLog(`Manufacturer lookup error: ${err}`);
      return null;
    }
  };

  // ---------------------------------------------------------
  // 3. OPENFOODFACTS INFO
  // ---------------------------------------------------------
  const getOpenFoodFactsInfo = async (barcode: string) => {
    try {
      addLog('Fetching OpenFoodFacts data...');
      const response = await fetch(
        `https://world.openfoodfacts.org/api/v0/product/${barcode}.json`
      );

      if (!response.ok) {
        addLog('OpenFoodFacts lookup failed');
        return null;
      }

      const data = await response.json();

      if (!data.product) {
        addLog('No OpenFoodFacts data found');
        return null;
      }

      const product = data.product;
      addLog('OpenFoodFacts data retrieved');

      return {
        productName: product.product_name,
        brands: product.brands,
        ingredients: product.ingredients_text,
        nutriscore: product.nutriscore_grade,
      };
    } catch (err) {
      addLog(`OpenFoodFacts error: ${err}`);
      return null;
    }
  };

  // ---------------------------------------------------------
  // 4. ETHICAL CONSUMER SEARCH (via Next.js API route)
  // ---------------------------------------------------------
  const searchEthicalConsumer = async (brand: string) => {
    try {
      addLog(`Searching Ethical Consumer for: ${brand}`);
      const response = await fetch('/api/ethical-search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ brand }),
      });

      if (!response.ok) {
        addLog('Ethical Consumer search failed');
        return null;
      }

      const data = await response.json();
      if (data.title && data.url) {
        addLog(`Ethical rating found: ${data.title}`);
        return { title: data.title, url: data.url };
      }
      
      addLog('No ethical ratings found');
      return null;
    } catch (err) {
      addLog(`Ethical Consumer error: ${err}`);
      return null;
    }
  };

  // ---------------------------------------------------------
  // MAIN SCAN FUNCTION
  // ---------------------------------------------------------
  const handleImageUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Reset state
    setError(null);
    setResult(null);
    setScanLog([]);

    // Display image preview
    const reader = new FileReader();
    reader.onload = (e) => {
      setSelectedImage(e.target?.result as string);
    };
    reader.readAsDataURL(file);

    // Start scanning
    setScanning(true);
    addLog('Starting barcode scan...');

    try {
      // Step 1: Decode barcode
      const barcode = await decodeBarcode(file);
      if (!barcode) {
        throw new Error('No barcode detected in image');
      }

      // Step 2: Get OpenFoodFacts info
      const openFoodFacts = await getOpenFoodFactsInfo(barcode);

      // Step 3: Get manufacturer
      const manufacturer = await searchManufacturer(barcode);

      // Step 4: Get ethical rating (if manufacturer found)
      let ethicalConsumer = null;
      if (manufacturer) {
        ethicalConsumer = await searchEthicalConsumer(manufacturer);
      }

      // Set final result
      setResult({
        barcode,
        openFoodFacts: openFoodFacts || undefined,
        manufacturer: manufacturer || undefined,
        ethicalConsumer: ethicalConsumer || undefined,
      });

      addLog('Scan complete!');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      setError(errorMessage);
      addLog(`Error: ${errorMessage}`);
    } finally {
      setScanning(false);
    }
  };

  const handleReset = () => {
    setSelectedImage(null);
    setResult(null);
    setError(null);
    setScanLog([]);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.wrapper}>
        <div className={styles.header}>
          <h1 className={styles.title}>
            {/* 🛒 Barcode Ethical Scanner */}
            Barcode Ethical Scanner
          </h1>
          <p className={styles.subtitle}>
            Scan product barcodes to discover ethical ratings and product information
          </p>
        </div>

        {/* Upload Section */}
        <div className={styles.uploadCard}>
          <label htmlFor="image-upload" className={styles.uploadLabel}>
            <div className={styles.uploadArea}>
              {selectedImage ? (
                <div className={styles.imagePreview}>
                  <img
                    src={selectedImage}
                    alt="Selected barcode"
                    className={styles.previewImage}
                  />
                  <button
                    onClick={(e) => {
                      e.preventDefault();
                      handleReset();
                    }}
                    className={styles.resetButton}
                  >
                    Choose Different Image
                  </button>
                </div>
              ) : (
                <div>
                  <svg
                    className={styles.uploadIcon}
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                    />
                  </svg>
                  <p className={styles.uploadTitle}>
                    Click to upload barcode image
                  </p>
                  <p className={styles.uploadSubtext}>
                    PNG, JPG, GIF up to 10MB
                  </p>
                  <p className={styles.uploadHint}>
                    Take a clear photo of the barcode
                  </p>
                </div>
              )}
            </div>
          </label>
          <input
            ref={fileInputRef}
            id="image-upload"
            type="file"
            accept="image/*"
            capture="environment"
            onChange={handleImageUpload}
            className={styles.fileInput}
          />
        </div>

        {/* Loading State */}
        {scanning && (
          <div className={styles.loadingCard}>
            <div className={styles.loadingHeader}>
              <svg
                className={styles.spinner}
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              <span className={styles.loadingText}>Scanning barcode...</span>
            </div>
            {scanLog.length > 0 && (
              <div className={styles.logContainer}>
                {scanLog.map((log, idx) => (
                  <p key={idx} className={styles.logEntry}>
                    {log}
                  </p>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Error State */}
        {error && (
          <div className={styles.errorCard}>
            <div className={styles.errorContent}>
              <svg
                className={styles.errorIcon}
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path
                  fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                  clipRule="evenodd"
                />
              </svg>
              <div>
                <p className={styles.errorTitle}>Error</p>
                <p className={styles.errorMessage}>{error}</p>
              </div>
            </div>
          </div>
        )}

        {/* Results */}
        {result && (
          <div className={styles.resultsCard}>
            <div className={styles.resultHeader}>
              <h2 className={styles.resultTitle}>Scan Results</h2>
              <p className={styles.barcodeDisplay}>
                {result.barcode}
              </p>
            </div>

            {/* OpenFoodFacts */}
            {result.openFoodFacts && (
              <div className={`${styles.section} ${styles.sectionGreen}`}>
                <h3 className={styles.sectionTitle}>
                  {/* <span className={styles.sectionIcon}>🍎</span> */}
                  Product Information
                </h3>
                <div className={styles.sectionContent}>
                  {result.openFoodFacts.productName && (
                    <p>
                      <span className={styles.fieldLabel}>Name:</span>{' '}
                      {result.openFoodFacts.productName}
                    </p>
                  )}
                  {result.openFoodFacts.brands && (
                    <p>
                      <span className={styles.fieldLabel}>Brand:</span>{' '}
                      {result.openFoodFacts.brands}
                    </p>
                  )}
                  {result.openFoodFacts.nutriscore && (
                    <p>
                      <span className={styles.fieldLabel}>Nutriscore:</span>{' '}
                      <span className={styles.nutriscoreBadge}>
                        {result.openFoodFacts.nutriscore}
                      </span>
                    </p>
                  )}
                  {result.openFoodFacts.ingredients && (
                    <p className={styles.ingredientsText}>
                      <span className={styles.fieldLabel}>Ingredients:</span>{' '}
                      {result.openFoodFacts.ingredients}
                    </p>
                  )}
                </div>
              </div>
            )}

            {/* Manufacturer */}
            {result.manufacturer && (
              <div className={`${styles.section} ${styles.sectionBlue}`}>
                <h3 className={styles.sectionTitle}>
                  {/* <span className={styles.sectionIcon}>🏭</span> */}
                  Manufacturer
                </h3>
                <p className={styles.manufacturerText}>{result.manufacturer}</p>
              </div>
            )}

            {/* Ethical Consumer */}
            {result.ethicalConsumer ? (
              <div className={`${styles.section} ${styles.sectionPurple}`}>
                <h3 className={styles.sectionTitle}>
                  <span className={styles.sectionIcon}>✨</span>
                  Ethical Rating
                </h3>
                <div className={styles.ethicalLinks}>
                  <a
                    href={result.ethicalConsumer.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={styles.externalLink}
                  >
                    {result.ethicalConsumer.title}
                    <svg
                      className={styles.linkIcon}
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M11 3a1 1 0 100 2h2.586l-6.293 6.293a1 1 0 101.414 1.414L15 6.414V9a1 1 0 102 0V4a1 1 0 00-1-1h-5z" />
                      <path d="M5 5a2 2 0 00-2 2v8a2 2 0 002 2h8a2 2 0 002-2v-3a1 1 0 10-2 0v3H5V7h3a1 1 0 000-2H5z" />
                    </svg>
                  </a>
                  <div className={styles.sourceAttribution}>
                    <span className={styles.sourceText}>Source:</span>{' '}
                    <a
                      href="https://www.ethicalconsumer.org"
                      target="_blank"
                      rel="noopener noreferrer"
                      className={styles.sourceLink}
                    >
                      Ethical Consumer
                      <svg
                        className={styles.sourceLinkIcon}
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path d="M11 3a1 1 0 100 2h2.586l-6.293 6.293a1 1 0 101.414 1.414L15 6.414V9a1 1 0 102 0V4a1 1 0 00-1-1h-5z" />
                        <path d="M5 5a2 2 0 00-2 2v8a2 2 0 002 2h8a2 2 0 002-2v-3a1 1 0 10-2 0v3H5V7h3a1 1 0 000-2H5z" />
                      </svg>
                    </a>
                  </div>
                </div>
              </div>
            ) : result.manufacturer ? (
              <div className={`${styles.section} ${styles.sectionGray}`}>
                <h3 className={styles.sectionTitle}>
                  {/* <span className={styles.sectionIcon}>ℹ️</span> */}
                  Ethical Rating
                </h3>
                <p className={styles.noResultText}>
                  Learn more about the company/industry practices here:{' '}
                  <a
                    href={`https://www.ethicalconsumer.org/search?keywords=${encodeURIComponent(result.manufacturer)}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={styles.manualSearchLink}
                  >
                    {result.manufacturer}
                  </a>
                </p>
              </div>
            ) : null}

            {/* Scan Again Button */}
            <div>
              <button
                onClick={handleReset}
                className={styles.scanAgainButton}
              >
                Scan Another Product
              </button>
            </div>
          </div>
        )}

        <h2 className = {styles.sample}>Use some of the sample barcodes below to see functionality (or feel free to upload your own)</h2>
        <div className={styles.sampleBarcodeContainer}>
            <img className={styles.barcodeImg} src={Cheerio.src} alt="Cheerios" />
            <img className={styles.barcodeImg} src={Hellman.src} alt="Hellman" />
            <img className={styles.barcodeImg} src={Nutella.src} alt="Nutella" />
            <img className={styles.barcodeImg} src={Philadelphia.src} alt="Philadelphia" />
            <img className={styles.barcodeImg} src={Silk.src} alt="Silk" />
        </div>

        

        {/* Info Footer */}
        <div className={styles.footer}>
          <p>Data sources: OpenFoodFacts, BarcodeLookup, Ethical Consumer</p>
        </div>
      </div>
    </div>
  );
}
