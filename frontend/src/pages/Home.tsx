import { useState } from 'react';
import { urlsApi } from '../api';
import type { ShortenUrlResponse } from '../types';
import { QRCodeSVG } from 'qrcode.react';
import '../styles/Home.css';

const Home = () => {
  const [longUrl, setLongUrl] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [shortenedUrl, setShortenedUrl] = useState<ShortenUrlResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    setShortenedUrl(null);

    try {
      const data = {
        longUrl,
        ...(customAlias && { customAlias }),
      };
      const response = await urlsApi.shorten(data);
      setShortenedUrl(response);
      setLongUrl('');
      setCustomAlias('');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to shorten URL. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = async () => {
    if (shortenedUrl) {
      try {
        await navigator.clipboard.writeText(shortenedUrl.shortUrl);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } catch (err) {
        console.error('Failed to copy:', err);
      }
    }
  };

  return (
    <div className="home-container">
      <div className="home-content">
        <h1>URL Shortener</h1>
        <p className="home-subtitle">Transform long URLs into short, shareable links</p>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit} className="shorten-form">
          <div className="form-group">
            <label htmlFor="longUrl">Enter your long URL</label>
            <input
              type="url"
              id="longUrl"
              value={longUrl}
              onChange={(e) => setLongUrl(e.target.value)}
              placeholder="https://example.com/very/long/url"
              required
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="customAlias">Custom alias (optional)</label>
            <input
              type="text"
              id="customAlias"
              value={customAlias}
              onChange={(e) => setCustomAlias(e.target.value)}
              placeholder="my-custom-link"
              pattern="[a-zA-Z0-9-_]+"
              title="Only letters, numbers, hyphens, and underscores allowed"
            />
          </div>

          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Shortening...' : 'Shorten URL'}
          </button>
        </form>

        {shortenedUrl && (
          <div className="result-card">
            <h2>Your shortened URL:</h2>
            <div className="shortened-url-container">
              <a href={shortenedUrl.shortUrl} target="_blank" rel="noopener noreferrer" className="shortened-url">
                {shortenedUrl.shortUrl}
              </a>
              <button onClick={handleCopy} className="btn btn-copy">
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>

            <div className="url-details">
              <p>
                <strong>Original URL:</strong> {shortenedUrl.longUrl}
              </p>
              <p>
                <strong>Short Code:</strong> {shortenedUrl.shortCode}
              </p>
              <p>
                <strong>Created:</strong> {new Date(shortenedUrl.createdAt).toLocaleString()}
              </p>
              {shortenedUrl.expiresAt && (
                <p>
                  <strong>Expires:</strong> {new Date(shortenedUrl.expiresAt).toLocaleString()}
                </p>
              )}
            </div>

            <div className="qr-code-container">
              <h3>QR Code</h3>
              <QRCodeSVG value={shortenedUrl.shortUrl} size={200} />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Home;
