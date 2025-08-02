CREATE TABLE IF NOT EXISTS event (
    build_id CHARACTER(36),
    component INTEGER,
    invocation_id CHARACTER(36),
    sequence INTEGER,
    event JSONB,
    PRIMARY KEY (
        build_id,
        component,
        sequence,
        invocation_id
    )
);
