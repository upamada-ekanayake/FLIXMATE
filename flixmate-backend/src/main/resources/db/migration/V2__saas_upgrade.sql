-- Migration script to upgrade FlixMate to Startup-Grade SaaS Platform

-- 1. Create Coupon Table
CREATE TABLE IF NOT EXISTS coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value NUMERIC(10, 2) NOT NULL,
    min_booking_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    max_discount_amount NUMERIC(10, 2),
    usage_limit INT,
    usage_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expiry_date TIMESTAMP NOT NULL
);

-- 2. Create Watchlist Table
CREATE TABLE IF NOT EXISTS watchlists (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    movie_id UUID NOT NULL,
    added_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_watchlist_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT uq_watchlist_user_movie UNIQUE (user_id, movie_id)
);

-- 3. Create Notification Table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Create Audit Log Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    details VARCHAR(2000),
    ip_address VARCHAR(255),
    timestamp TIMESTAMP NOT NULL
);

-- 5. Modify User Table
ALTER TABLE users ADD COLUMN IF NOT EXISTS loyalty_points INT NOT NULL DEFAULT 0;

-- 6. Modify Screen Table
ALTER TABLE screens ADD COLUMN IF NOT EXISTS rows_count INT;
ALTER TABLE screens ADD COLUMN IF NOT EXISTS cols_count INT;

-- 7. Modify Booking Table
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS coupon_id UUID;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS points_earned INT NOT NULL DEFAULT 0;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS points_redeemed INT NOT NULL DEFAULT 0;
ALTER TABLE bookings ADD CONSTRAINT fk_booking_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE SET NULL;
