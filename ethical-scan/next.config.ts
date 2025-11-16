// next.config.ts
const nextConfig: any = {
    experimental: {
      serverActions: true,
    },
    api: {
      bodyParser: false, // allow formidable to handle file uploads
    },
  };
  
  export default nextConfig;
  