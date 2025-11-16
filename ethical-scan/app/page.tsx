import Image from "next/image";
import "./home.css";
import React from "react";
import { FaApple, FaShoppingCart, FaTshirt } from "react-icons/fa";

export default function Home() {
  return (
    <div>
      <div id="introbox">
        <p id="introdesc">
          Welcome to the one and only product barcode scanner! Here's how it works: First, you upload a picture of a barcode,
          then you must select the category of the product you want to scan, then voila, you get detailed background info on your scanned product.
        </p>
      </div>
      <div className="ccc">Select the barcode product category you want to scan.</div>

      <div id="product_type_links">

        <div className="button-container">
          <a href="app/food.tsx" className="iconlink">
            <FaApple size={70} color="white" />
            <span className="buttonlabel">Food</span>
          </a>

          <a href="INSERT_RETAIL_API_PAGE" className="iconlink">
            <FaShoppingCart size={70} color="white" />
            <span className="buttonlabel">Retail</span>
          </a>

          <a href="INSERT_CLOTHES_API_PAGE" className="iconlink">
            <FaTshirt size={70} color="white" />
            <span className="buttonlabel">Clothes</span>
          </a>
        </div>
      </div>
    </div>
  );
}
