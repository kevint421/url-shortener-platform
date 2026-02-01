-- Insert test user for integration tests (idempotent)
INSERT INTO users (id, username, email, password_hash, created_at, updated_at, is_active)
VALUES (1, 'testuser', 'test@example.com', '$2a$10$dummy.hash.for.testing', NOW(), NOW(), true)
ON CONFLICT (id) DO NOTHING;
