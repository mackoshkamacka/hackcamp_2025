"use client"

import "./results.css";

export default function ResultsPage() {
    const productName = "Organic Banana";
    const barcode = "123456789012uiyiu3";
    const ecoscore = "B";
    const ethicalRating = "Good";
    const description = "This product scores well on environmental sustainability and ethical sourcing.";

    return (
        <div className="results-container">
            <div className="results-card">
                <h1 className="results-title">Scan Results</h1>

                <div className="results-section">
                    <p><span className="label">Product:</span> {productName}</p>
                    <p><span className="label">Barcode:</span> {barcode}</p>
                    <p><span className="label">Ecoscore:</span> {ecoscore}</p>
                    <p><span className="label">Ethical Rating:</span> {ethicalRating}</p>
                </div>

                <div className="results-description">
                    <p>{description}</p>
                </div>

                <button className="back-button" onClick={() => window.history.back()}>
                    Scan Another Item
                </button>
            </div>
            
        </div>
    );
}
