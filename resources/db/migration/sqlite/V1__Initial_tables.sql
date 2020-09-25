
CREATE TABLE IF NOT EXISTS Alliance (
    id INT NOT NULL,
    updated TEXT NOT NULL,
    CONSTRAINT PK_Alliance PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS Ansiblex (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ansiblexId BIGINT NOT NULL,
    allianceId INT NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    solarSystemId INT NOT NULL
);
CREATE UNIQUE INDEX Ansiblex_ansiblexId_allianceId ON Ansiblex (ansiblexId, allianceId);

CREATE TABLE IF NOT EXISTS TemporaryConnection (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    system1Id INT NOT NULL,
    system2Id INT NOT NULL,
    characterId INT NOT NULL,
    system1Name VARCHAR(255) NOT NULL,
    system2Name VARCHAR(255) NOT NULL,
    created TEXT NOT NULL
);
CREATE UNIQUE INDEX TemporaryConnection_system1Id_system2Id_characterId
    ON TemporaryConnection (system1Id, system2Id, characterId);
