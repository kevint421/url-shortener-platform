import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { urlsApi } from '../api';
import { useAuthStore } from '../store/authStore';
import type { Url } from '../types';
import '../styles/Dashboard.css';

const Dashboard = () => {
  const [urls, setUrls] = useState<Url[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState<string | null>(null);
  const user = useAuthStore((state) => state.user);

  useEffect(() => {
    const fetchUrls = async () => {
      if (!user) return;

      try {
        const data = await urlsApi.getUserUrls(user.id);
        setUrls(data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to fetch URLs');
      } finally {
        setLoading(false);
      }
    };

    fetchUrls();

    // Poll for updates every 5 seconds to show real-time click counts
    const interval = setInterval(() => {
      if (user) {
        urlsApi.getUserUrls(user.id)
          .then((data) => setUrls(data))
          .catch(() => {}); // Silently fail on polling errors
      }
    }, 5000);

    return () => clearInterval(interval);
  }, [user]);

  const handleDelete = async (shortCode: string) => {
    if (!window.confirm('Are you sure you want to delete this URL?')) {
      return;
    }

    try {
      await urlsApi.deleteUrl(shortCode);
      setUrls(urls.filter((url) => url.shortCode !== shortCode));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete URL');
    }
  };

  const handleCopy = async (shortCode: string) => {
    // Use API Gateway URL (port 8080) for shortened URLs, not frontend URL (port 3000)
    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const shortUrl = `${API_BASE_URL}/${shortCode}`;
    try {
      await navigator.clipboard.writeText(shortUrl);
      setCopied(shortCode);
      setTimeout(() => setCopied(null), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  if (loading) {
    return <div className="dashboard-container"><div className="loading">Loading...</div></div>;
  }

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h1>My URLs</h1>
        <p>Manage and track your shortened URLs</p>
      </div>

      {error && <div className="error-message">{error}</div>}

      {urls.length === 0 ? (
        <div className="empty-state">
          <p>You haven't created any shortened URLs yet.</p>
          <Link to="/" className="btn btn-primary">
            Create your first URL
          </Link>
        </div>
      ) : (
        <div className="urls-grid">
          {urls.map((url) => (
            <div key={url.id} className="url-card">
              <div className="url-card-header">
                <h3>{url.shortCode}</h3>
                <span className="click-count">{url.clickCount} clicks</span>
              </div>

              <div className="url-card-body">
                <p className="long-url" title={url.longUrl}>
                  {url.longUrl}
                </p>

                <div className="url-meta">
                  <span>Created: {new Date(url.createdAt).toLocaleDateString()}</span>
                  {url.expiresAt && (
                    <span className="expires">
                      Expires: {new Date(url.expiresAt).toLocaleDateString()}
                    </span>
                  )}
                </div>
              </div>

              <div className="url-card-actions">
                <button
                  onClick={() => handleCopy(url.shortCode)}
                  className="btn btn-small btn-copy"
                >
                  {copied === url.shortCode ? 'Copied!' : 'Copy'}
                </button>
                <Link
                  to={`/analytics/${url.shortCode}`}
                  className="btn btn-small btn-secondary"
                >
                  Analytics
                </Link>
                <button
                  onClick={() => handleDelete(url.shortCode)}
                  className="btn btn-small btn-danger"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Dashboard;
