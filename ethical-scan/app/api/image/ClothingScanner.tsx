'use client';

import { useState, useRef } from 'react';
import styles from './ClothingScanner.module.css';

interface LykdatResult {
  title?: string;
  brand?: string;
  url?: string;
  image?: string;
}

export default function ClothingScanner() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [scanning, setScanning] = useState(false);
  const [result, setResult] = useState<LykdatResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [scanLog, setScanLog] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const addLog = (message: string) => {
    setScanLog(prev => [...prev, `${new Date().toLocaleTimeString()}: ${message}`]);
  };

  const handleImageUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setError(null);
    setResult(null);
    setScanLog([]);

    // Display image preview
    const reader = new FileReader();
    reader.onload = (e) => setSelectedImage(e.target?.result as string);
    reader.readAsDataURL(file);

    setScanning(true);
    addLog('Uploading image to Lykdat...');

    try {
      const formData = new FormData();
      formData.append('image', file);

      const response = await fetch('/api/lykdat-search', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) throw new Error('Lykdat lookup failed');

      const data = await response.json();

      const firstResult = data.results?.[0];
      if (!firstResult) throw new Error('No clothing results found');

      addLog(`Result found: ${firstResult.title || firstResult.brand || 'Unknown'}`);

      setResult({
        title: firstResult.title,
        brand: firstResult.brand,
        url: firstResult.url,
        image: firstResult.image,
      });
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'An error occurred';
      setError(msg);
      addLog(`Error: ${msg}`);
    } finally {
      setScanning(false);
    }
  };

  const handleReset = () => {
    setSelectedImage(null);
    setResult(null);
    setError(null);
    setScanLog([]);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  return (
    <div className={styles.container}>
      <div className={styles.wrapper}>
        <h1 className={styles.title}>👗 Clothing Scanner</h1>
        <p className={styles.subtitle}>
          Upload a clothing photo to detect brand or product via Lykdat
        </p>

        {/* Upload Section */}
        <div className={styles.uploadCard}>
          <label htmlFor="image-upload" className={styles.uploadLabel}>
            <div className={styles.uploadArea}>
              {selectedImage ? (
                <div className={styles.imagePreview}>
                  <img src={selectedImage} alt="Selected clothing" className={styles.previewImage} />
                  <button onClick={(e) => { e.preventDefault(); handleReset(); }} className={styles.resetButton}>
                    Choose Different Image
                  </button>
                </div>
              ) : (
                <div>
                  <p className={styles.uploadText}>Click to upload clothing image (PNG, JPG, GIF up to 10MB)</p>
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

        {/* Loading */}
        {scanning && (
          <div className={styles.loadingCard}>
            <p>Scanning image...</p>
            <div className={styles.logContainer}>
              {scanLog.map((log, idx) => <p key={idx}>{log}</p>)}
            </div>
          </div>
        )}

        {/* Error */}
        {error && <div className={styles.errorCard}>{error}</div>}

        {/* Result */}
        {result && (
          <div className={styles.resultsCard}>
            {result.image && <img src={result.image} alt={result.title || 'Clothing'} className={styles.resultImage} />}
            <p><strong>Title:</strong> {result.title || 'N/A'}</p>
            <p><strong>Brand:</strong> {result.brand || 'N/A'}</p>
            {result.url && (
              <p>
                <a href={result.url} target="_blank" rel="noopener noreferrer" className={styles.externalLink}>
                  View on Lykdat
                </a>
              </p>
            )}
            <button onClick={handleReset} className={styles.scanAgainButton}>Scan Another Item</button>
          </div>
        )}
      </div>
    </div>
  );
}