CREATE TABLE IF NOT EXISTS event (build_id TEXT, component INTEGER, invocation_id TEXT, sequence INTEGER, event TEXT, PRIMARY KEY (build_id, component, sequence, invocation_id));
