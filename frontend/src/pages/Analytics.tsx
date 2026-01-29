import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { analyticsApi, urlsApi } from '../api';
import type { UrlAnalytics, DailyAnalytics, GeoAnalytics, UrlClick, Url } from '../types';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { QRCodeSVG } from 'qrcode.react';
import '../styles/Analytics.css';

const Analytics = () => {
  const { shortCode } = useParams<{ shortCode: string }>();
  const [url, setUrl] = useState<Url | null>(null);
  const [analytics, setAnalytics] = useState<UrlAnalytics | null>(null);
  const [dailyStats, setDailyStats] = useState<DailyAnalytics[]>([]);
  const [geoStats, setGeoStats] = useState<GeoAnalytics[]>([]);
  const [recentClicks, setRecentClicks] = useState<UrlClick[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const fetchAnalytics = async () => {
      if (!shortCode) return;

      try {
        const [urlData, analyticsData, dailyData, geoData, clicksData] = await Promise.all([
          urlsApi.getUrl(shortCode),
          analyticsApi.getUrlAnalytics(shortCode),
          analyticsApi.getDailyAnalytics(shortCode, 30),
          analyticsApi.getGeoAnalytics(shortCode),
          analyticsApi.getRecentClicks(shortCode, 10),
        ]);

        setUrl(urlData);
        setAnalytics(analyticsData);
        setDailyStats(dailyData.reverse());
        setGeoStats(geoData);
        setRecentClicks(clicksData);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to fetch analytics');
      } finally {
        setLoading(false);
      }
    };

    fetchAnalytics();

    // Poll for updates every 10 seconds to show real-time analytics
    const interval = setInterval(() => {
      if (shortCode) {
        Promise.all([
          analyticsApi.getUrlAnalytics(shortCode),
          analyticsApi.getDailyAnalytics(shortCode, 30),
          analyticsApi.getGeoAnalytics(shortCode),
          analyticsApi.getRecentClicks(shortCode, 10),
        ])
          .then(([analyticsData, dailyData, geoData, clicksData]) => {
            setAnalytics(analyticsData);
            setDailyStats(dailyData.reverse());
            setGeoStats(geoData);
            setRecentClicks(clicksData);
          })
          .catch(() => {}); // Silently fail on polling errors
      }
    }, 10000);

    return () => clearInterval(interval);
  }, [shortCode]);

  const handleCopy = async () => {
    if (shortCode) {
      // Use API Gateway URL (port 8080) for shortened URLs, not frontend URL (port 3000)
      const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
      const shortUrl = `${API_BASE_URL}/${shortCode}`;
      try {
        await navigator.clipboard.writeText(shortUrl);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } catch (err) {
        console.error('Failed to copy:', err);
      }
    }
  };

  if (loading) {
    return <div className="analytics-container"><div className="loading">Loading analytics...</div></div>;
  }

  if (error) {
    return (
      <div className="analytics-container">
        <div className="error-message">{error}</div>
        <Link to="/dashboard" className="btn btn-primary">Back to Dashboard</Link>
      </div>
    );
  }

  // Use API Gateway URL (port 8080) for shortened URLs, not frontend URL (port 3000)
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  const shortUrl = `${API_BASE_URL}/${shortCode}`;

  return (
    <div className="analytics-container">
      <div className="analytics-header">
        <div>
          <Link to="/dashboard" className="back-link">← Back to Dashboard</Link>
          <h1>Analytics for {shortCode}</h1>
          <p className="long-url">{url?.longUrl}</p>
        </div>
        <div className="header-actions">
          <button onClick={handleCopy} className="btn btn-secondary">
            {copied ? 'Copied!' : 'Copy Short URL'}
          </button>
        </div>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <h3>Total Clicks</h3>
          <p className="stat-value">{analytics?.totalClicks || 0}</p>
        </div>
        <div className="stat-card">
          <h3>Unique Visitors</h3>
          <p className="stat-value">{analytics?.uniqueIps || 0}</p>
        </div>
        <div className="stat-card">
          <h3>First Click</h3>
          <p className="stat-value-small">
            {analytics?.firstClickedAt
              ? new Date(analytics.firstClickedAt).toLocaleDateString()
              : 'No clicks yet'}
          </p>
        </div>
        <div className="stat-card">
          <h3>Last Click</h3>
          <p className="stat-value-small">
            {analytics?.lastClickedAt
              ? new Date(analytics.lastClickedAt).toLocaleDateString()
              : 'No clicks yet'}
          </p>
        </div>
      </div>

      {dailyStats.length > 0 && (
        <div className="chart-section">
          <h2>Clicks Over Time (Last 30 Days)</h2>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={dailyStats}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="clickCount" stroke="#3b82f6" name="Clicks" />
              <Line type="monotone" dataKey="uniqueIps" stroke="#10b981" name="Unique IPs" />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      <div className="analytics-grid">
        {geoStats.length > 0 && (
          <div className="chart-section">
            <h2>Top Locations</h2>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={geoStats.slice(0, 10)}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="city" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="clickCount" fill="#3b82f6" name="Clicks" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}

        <div className="qr-section">
          <h2>QR Code</h2>
          <div className="qr-code-wrapper">
            <QRCodeSVG value={shortUrl} size={250} />
          </div>
        </div>
      </div>

      {recentClicks.length > 0 && (
        <div className="recent-clicks-section">
          <h2>Recent Clicks</h2>
          <div className="table-container">
            <table className="clicks-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Location</th>
                  <th>Device</th>
                  <th>Browser</th>
                  <th>OS</th>
                </tr>
              </thead>
              <tbody>
                {recentClicks.map((click) => (
                  <tr key={click.id}>
                    <td>{new Date(click.clickedAt).toLocaleString()}</td>
                    <td>
                      {click.city && click.country
                        ? `${click.city}, ${click.country}`
                        : click.country || 'Unknown'}
                    </td>
                    <td>{click.deviceType || 'Unknown'}</td>
                    <td>{click.browser || 'Unknown'}</td>
                    <td>{click.os || 'Unknown'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {analytics?.totalClicks === 0 && (
        <div className="empty-state">
          <p>No analytics data available yet. Share your link to start tracking!</p>
        </div>
      )}
    </div>
  );
};

export default Analytics;
