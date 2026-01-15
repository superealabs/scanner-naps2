CREATE TABLE IF NOT EXISTS scan_sessions (
    scan_id TEXT PRIMARY KEY,
    status TEXT NOT NULL,
    scanner_name TEXT,
    options_json TEXT,
    created_at INTEGER NOT NULL,
    started_at INTEGER,
    completed_at INTEGER,
    cancelled_at INTEGER,
    error_message TEXT,
    pdf_path TEXT,
    pdf_size INTEGER,
    created_at_iso TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_scan_sessions_status ON scan_sessions(status);
CREATE INDEX IF NOT EXISTS idx_scan_sessions_created_at ON scan_sessions(created_at DESC);

