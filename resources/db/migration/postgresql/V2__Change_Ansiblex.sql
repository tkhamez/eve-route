
ALTER TABLE ansiblex RENAME TO ansiblexV02;

CREATE TABLE IF NOT EXISTS ansiblex (
    id BIGINT,
    "name" VARCHAR(255) NOT NULL,
    "solarSystemId" INT NOT NULL,
    CONSTRAINT PK_Ansiblex PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ansiblexalliance (
    "ansiblexId" BIGINT,
    "allianceId" INT,
    CONSTRAINT PK_AnsiblexAlliance PRIMARY KEY ("ansiblexId", "allianceId"),
    CONSTRAINT fk_ansiblexalliance_ansiblexid_id FOREIGN KEY ("ansiblexId") REFERENCES ansiblex(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ansiblexalliance_allianceid_id FOREIGN KEY ("allianceId") REFERENCES alliance(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- copy data

INSERT INTO ansiblex (id, "name", "solarSystemId")
SELECT DISTINCT "ansiblexId", "name", "solarSystemId" FROM ansiblexV02;

INSERT INTO ansiblexalliance ("ansiblexId", "allianceId")
SELECT "ansiblexId", "allianceId" FROM ansiblexV02;

DROP TABLE ansiblexV02;
