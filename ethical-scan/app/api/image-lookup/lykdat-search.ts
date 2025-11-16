import FormData from 'form-data';
import fs from 'fs';
import fetch from 'node-fetch';
import type { NextApiRequest, NextApiResponse } from 'next';
import formidable, { File } from 'formidable';

export const config = {
  api: { bodyParser: false },
};

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });

  const form = formidable({ multiples: false, maxFileSize: 10 * 1024 * 1024 });

  form.parse(req, async (err, fields, files: { image?: File[] }) => {
    if (err) return res.status(400).json({ error: 'Error parsing form data' });

    const file = files.image?.[0];
    if (!file) return res.status(400).json({ error: 'No image uploaded' });

    try {
      const lykdatApiKey = process.env.LYKDAT_API_KEY;
      if (!lykdatApiKey) return res.status(500).json({ error: 'LYKDAT_API_KEY not set' });

      const formData = new FormData();
      formData.append('api_key', lykdatApiKey);
      formData.append('image', fs.createReadStream(file.filepath));

      const lykdatResponse = await fetch('https://cloudapi.lykdat.com/v1/global/search', {
        method: 'POST',
        body: formData as any, // node-fetch + FormData
        headers: formData.getHeaders(),
      });

      if (!lykdatResponse.ok) {
        const text = await lykdatResponse.text();
        throw new Error(`Lykdat API returned ${lykdatResponse.status}: ${text}`);
      }

      const data = await lykdatResponse.json();
      const firstResult = data.results?.[0] ?? null;

      return res.status(200).json({ results: firstResult ? [firstResult] : [] });
    } catch (error) {
      console.error('Lykdat search error:', error);
      return res.status(500).json({ error: 'Failed to search image' });
    }
  });
}
