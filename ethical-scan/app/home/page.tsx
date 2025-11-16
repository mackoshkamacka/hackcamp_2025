"use client";

import React from "react";
import "./home.css";
import { useRouter } from "next/navigation";

const HomePage = () => {
  const router = useRouter();

  const handleFood = () => {
    router.push("/scanner");      // 
  };

  const handleLearnMore = () => {
    router.push("/retail");    // or whatever page you want
  };

  const handleContact = () => {
    router.push("/clothes");   // or whatever page you want
  };

  return (
    <div className="homepage-container">
      <div className="content-wrapper">

        <div className="welcome-container">
          <div className="welcome-section">
            <h1 className="welcome-title typewriter">
              <span className="typewriter-text">SnapScan</span>
              <span className="cursor">|</span>
            </h1>
          </div>
        </div>

        <div className="description-section">
          <ol className="description-list">
            <li>Select the product category you want to scan.</li>
            <li>Upload a picture of the barcode you want to scan.</li>
            <li>Get detailed background info.</li>
          </ol>
        </div>

        <div className="button-section">
          <button className="btn btn-primary" onClick={handleFood}>
            Groceries
          </button>

          <button className="btn btn-secondary" onClick={handleLearnMore}>
            Retail (DLC)
          </button>

          <button className="btn btn-tertiary" onClick={handleContact}>
            Clothes
          </button>
        </div>

        <p className="stip">(retail and grocery functionality not implemented)</p>
      </div>
    </div>
  );
};

export default HomePage;
