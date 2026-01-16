-- This script creates schemas and tables for both URL Service and Analytics Service

-- ============================================
-- URL SERVICE SCHEMA
-- ============================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- URLs table
CREATE TABLE IF NOT EXISTS urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    click_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    custom_alias BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for URL Service
CREATE INDEX IF NOT EXISTS idx_urls_user_id ON urls(user_id);
CREATE INDEX IF NOT EXISTS idx_urls_short_code ON urls(short_code);
CREATE INDEX IF NOT EXISTS idx_urls_created_at ON urls(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_urls_expires_at ON urls(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- ============================================
-- ANALYTICS SERVICE SCHEMA
-- ============================================

-- Raw click events table
CREATE TABLE IF NOT EXISTS url_clicks (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    clicked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    referer TEXT,
    country VARCHAR(100),
    city VARCHAR(100),
    device_type VARCHAR(50),
    browser VARCHAR(50),
    operating_system VARCHAR(50)
);

-- Aggregated analytics table
CREATE TABLE IF NOT EXISTS url_analytics (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) UNIQUE NOT NULL,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    unique_ips BIGINT NOT NULL DEFAULT 0,
    last_clicked_at TIMESTAMP,
    first_clicked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Daily analytics aggregates
CREATE TABLE IF NOT EXISTS url_daily_analytics (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    date DATE NOT NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    unique_ips BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(short_code, date)
);

-- Geographic analytics
CREATE TABLE IF NOT EXISTS url_geo_analytics (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    country VARCHAR(100),
    city VARCHAR(100),
    click_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(short_code, country, city)
);

-- Indexes for Analytics Service
CREATE INDEX IF NOT EXISTS idx_url_clicks_short_code ON url_clicks(short_code);
CREATE INDEX IF NOT EXISTS idx_url_clicks_clicked_at ON url_clicks(clicked_at DESC);
CREATE INDEX IF NOT EXISTS idx_url_clicks_ip_address ON url_clicks(ip_address);
CREATE INDEX IF NOT EXISTS idx_url_analytics_short_code ON url_analytics(short_code);
CREATE INDEX IF NOT EXISTS idx_url_daily_analytics_short_code_date ON url_daily_analytics(short_code, date DESC);
CREATE INDEX IF NOT EXISTS idx_url_geo_analytics_short_code ON url_geo_analytics(short_code);

-- ============================================
-- SEED DATA (for development/testing)
-- ============================================

-- Insert test user (password is 'password123' - BCrypt hashed)
INSERT INTO users (username, email, password_hash) VALUES
    ('testuser', 'test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
    ('admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy')
ON CONFLICT (username) DO NOTHING;

-- Insert sample URLs for testing
INSERT INTO urls (short_code, long_url, user_id, expires_at) VALUES
    ('abc123', 'https://www.google.com', 1, NULL),
    ('xyz789', 'https://www.github.com', 1, CURRENT_TIMESTAMP + INTERVAL '30 days'),
    ('test01', 'https://www.stackoverflow.com', 2, NULL)
ON CONFLICT (short_code) DO NOTHING;

-- ============================================
-- FUNCTIONS AND TRIGGERS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for users table
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for url_analytics table
DROP TRIGGER IF EXISTS update_url_analytics_updated_at ON url_analytics;
CREATE TRIGGER update_url_analytics_updated_at
    BEFORE UPDATE ON url_analytics
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for url_geo_analytics table
DROP TRIGGER IF EXISTS update_url_geo_analytics_updated_at ON url_geo_analytics;
CREATE TRIGGER update_url_geo_analytics_updated_at
    BEFORE UPDATE ON url_geo_analytics
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- VIEWS (for convenient querying)
-- ============================================

-- View for URL details with user information
CREATE OR REPLACE VIEW url_details AS
SELECT 
    u.id,
    u.short_code,
    u.long_url,
    u.created_at,
    u.expires_at,
    u.click_count,
    u.is_active,
    u.custom_alias,
    usr.username,
    usr.email
FROM urls u
JOIN users usr ON u.user_id = usr.id;

-- View for top URLs by clicks
CREATE OR REPLACE VIEW top_urls AS
SELECT 
    short_code,
    long_url,
    click_count,
    created_at
FROM urls
WHERE is_active = TRUE
ORDER BY click_count DESC;

-- Grant permissions (if needed for specific roles)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO urluser;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO urluser;