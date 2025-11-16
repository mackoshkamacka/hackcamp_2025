import type { NextApiRequest, NextApiResponse } from 'next';
import formidable from 'formidable';
import fs from 'fs';
import fetch from 'node-fetch';

export const config = {
  api: {
    bodyParser: false, // required to handle file uploads with formidable
  },
};

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const form = formidable({ multiples: false });

  form.parse(req, async (err, fields, files: any) => {
    if (err) return res.status(400).json({ error: 'Error parsing form data' });
    if (!files?.image) return res.status(400).json({ error: 'No image uploaded' });

    try {
      const file = files.image;
      const imageBuffer = fs.readFileSync(file.filepath);

      // Call Lykdat API
      const lykdatApiKey = process.env.LYKDAT_API_KEY;
      if (!lykdatApiKey) {
        return res.status(500).json({ error: 'LYKDAT_API_KEY not set in environment variables' });
      }

      const lykdatResponse = await fetch('https://api.lykdat.com/search', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${lykdatApiKey}`,
          'Content-Type': 'application/octet-stream', // send raw image
        },
        body: imageBuffer,
      });

      if (!lykdatResponse.ok) {
        throw new Error(`Lykdat API returned status ${lykdatResponse.status}`);
      }

      const data = await lykdatResponse.json();

      // Return only first result
      const firstResult = data.results?.[0] || null;

      return res.status(200).json({ results: firstResult ? [firstResult] : [] });
    } catch (error) {
      console.error('Lykdat search error:', error);
      return res.status(500).json({ error: 'Failed to search image' });
    }
  });
}
