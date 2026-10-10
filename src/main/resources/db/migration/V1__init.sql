CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE vpn_nodes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    host VARCHAR(255) NOT NULL,
    region VARCHAR(100) NOT NULL,
    protocol VARCHAR(30) NOT NULL DEFAULT 'VLESS_REALITY',
    capacity INT NOT NULL,
    active_client_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE vpn_clients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    node_id BIGINT NOT NULL REFERENCES vpn_nodes(id),
    external_client_id VARCHAR(100) NOT NULL,
    config TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_vpn_clients_user_id ON vpn_clients(user_id);
CREATE INDEX idx_vpn_clients_node_id ON vpn_clients(node_id);

-- Seed a couple of nodes so the API is demoable immediately after startup.
INSERT INTO vpn_nodes (name, host, region, protocol, capacity) VALUES
    ('oracle-fra-01', 'fra01.example-vpnfleet.net', 'eu-frankfurt', 'VLESS_REALITY', 50),
    ('oracle-sgp-01', 'sgp01.example-vpnfleet.net', 'ap-singapore', 'VLESS_REALITY', 50);
