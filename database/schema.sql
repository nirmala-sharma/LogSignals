CREATE  TABLE app_users(
  user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE applications (
  app_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  owner_user_id BIGINT NOT NULL REFERENCES app_users(user_id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (owner_user_id, name)
);

CREATE TABLE application_api_keys (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  application_id BIGINT NOT NULL REFERENCES applications(app_id) ON DELETE CASCADE,
  key_hash TEXT NOT NULL UNIQUE,
  key_prefix TEXT NOT NULL,
  name TEXT,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE log_analysis_runs (
  log_run_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  application_id BIGINT NOT NULL REFERENCES applications(app_id) ON DELETE CASCADE,
  status TEXT NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'PARTIAL')),
  message TEXT,
  total_lines INT NOT NULL DEFAULT 0,
  invalid_lines INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE logs (
  log_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  analysis_run_id BIGINT NOT NULL REFERENCES log_analysis_runs(log_run_id) ON DELETE CASCADE,
  application_id BIGINT NOT NULL REFERENCES applications(app_id) ON DELETE CASCADE,
  service_name TEXT,
  hostname TEXT,
  error_code TEXT,
  level TEXT CHECK (
    level IS NULL OR level IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL')
  ),
  message TEXT NOT NULL,
  occurred_at TIMESTAMPTZ,
  raw_line TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE anomalies (
  anomaly_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  analysis_run_id BIGINT NOT NULL REFERENCES log_analysis_runs(log_run_id) ON DELETE CASCADE,
  application_id BIGINT NOT NULL REFERENCES applications(app_id) ON DELETE CASCADE,
  service_name TEXT NOT NULL,
  error_code TEXT NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

