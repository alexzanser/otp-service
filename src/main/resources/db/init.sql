-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы конфигурации OTP-кодов (одна запись)
CREATE TABLE IF NOT EXISTS otp_config (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    length INTEGER NOT NULL DEFAULT 6,
    expiration_time_ms INTEGER NOT NULL DEFAULT 300000, -- 5 минут по умолчанию
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Вставка дефолтной конфигурации, если таблица пуста
INSERT INTO otp_config (id, length, expiration_time_ms) 
VALUES (1, 6, 300000)
ON CONFLICT (id) DO NOTHING;

-- Создание таблицы OTP-кодов
CREATE TABLE IF NOT EXISTS otp_codes (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id VARCHAR(50),
    code VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
    delivery_channel VARCHAR(20) NOT NULL CHECK (delivery_channel IN ('SMS', 'EMAIL', 'TELEGRAM', 'FILE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT unique_active_operation_id UNIQUE (operation_id, status)
);

-- Индекс для быстрого поиска активных кодов
CREATE INDEX IF NOT EXISTS idx_otp_codes_user_status ON otp_codes (user_id, status);
CREATE INDEX IF NOT EXISTS idx_otp_codes_expires_at ON otp_codes (expires_at) WHERE status = 'ACTIVE';

-- Индекс для поиска по operation_id
CREATE INDEX IF NOT EXISTS idx_otp_codes_operation_id ON otp_codes (operation_id); 