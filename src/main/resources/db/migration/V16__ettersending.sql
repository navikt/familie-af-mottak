CREATE TABLE ettersending(
    id             VARCHAR(100)                                   NOT NULL PRIMARY KEY,
    ettersending_json    BYTEA                                          NOT NULL,
    ettersending_pdf     BYTEA,
    journalpost_id VARCHAR,
    saksnummer     VARCHAR,
    fnr            VARCHAR(50),
    dokumenttype   VARCHAR(256) DEFAULT N'ETTERSENDING' NOT NULL,
    task_opprettet BOOLEAN      DEFAULT FALSE                     NOT NULL,
    opprettet_tid  TIMESTAMP(3) DEFAULT LOCALTIMESTAMP            NOT NULL,
    vedlegg        BYTEA,
    behandle_i_ny_saksbehandling BOOLEAN DEFAULT FALSE
);
