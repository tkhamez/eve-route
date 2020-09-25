
CREATE TABLE IF NOT EXISTS alliance (
    id INT,
    updated TIMESTAMP NOT NULL,
    CONSTRAINT PK_Alliance PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ansiblex (
    id SERIAL PRIMARY KEY,
    "ansiblexId" BIGINT NOT NULL,
    "allianceId" INT NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    "solarSystemId" INT NOT NULL
);
ALTER TABLE ansiblex ADD CONSTRAINT ansiblex_ansiblexid_allianceid_unique
    UNIQUE ("ansiblexId", "allianceId");

CREATE TABLE IF NOT EXISTS temporaryconnection (
    id SERIAL PRIMARY KEY,
    "system1Id" INT NOT NULL,
    "system2Id" INT NOT NULL,
    "characterId" INT NOT NULL,
    "system1Name" VARCHAR(255) NOT NULL,
    "system2Name" VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL
);
ALTER TABLE temporaryconnection ADD CONSTRAINT temporaryconnection_system1id_system2id_characterid_unique
    UNIQUE ("system1Id", "system2Id", "characterId");
