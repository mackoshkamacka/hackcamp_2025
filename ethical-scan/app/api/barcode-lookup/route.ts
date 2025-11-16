import { NextRequest, NextResponse } from 'next/server';

// Add your BarcodeLookup API key to .env.local:
// BARCODE_LOOKUP_API_KEY=your_key_here

export async function POST(request: NextRequest) {
  try {
    const { barcode } = await request.json();

    if (!barcode) {
      return NextResponse.json(
        { error: 'Barcode is required' },
        { status: 400 }
      );
    }

    const apiKey = process.env.BARCODE_LOOKUP_API_KEY;

    if (!apiKey) {
      console.error('BARCODE_LOOKUP_API_KEY not set in environment variables');
      return NextResponse.json(
        { error: 'API key not configured' },
        { status: 500 }
      );
    }

    const apiUrl = `https://api.barcodelookup.com/v3/products?barcode=${barcode}&formatted=y&key=${apiKey}`;

    const response = await fetch(apiUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0',
      },
    });

    if (!response.ok) {
      throw new Error(`BarcodeLookup API error: ${response.status}`);
    }

    const data = await response.json();
    const products = data.products || [];

    if (products.length > 0 && products[0].manufacturer) {
      return NextResponse.json({
        manufacturer: products[0].manufacturer,
      });
    }

    return NextResponse.json({ manufacturer: null });
  } catch (error) {
    console.error('Barcode lookup error:', error);
    return NextResponse.json(
      { error: 'Failed to lookup barcode' },
      { status: 500 }
    );
  }
}