
CREATE TABLE IF NOT EXISTS Alliance (
    id INT,
    updated DATETIME(6) NOT NULL,
    CONSTRAINT PK_Alliance PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS Ansiblex (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ansiblexId BIGINT NOT NULL,
    allianceId INT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    solarSystemId INT NOT NULL
);
ALTER TABLE Ansiblex ADD CONSTRAINT Ansiblex_ansiblexId_allianceId_unique UNIQUE (ansiblexId, allianceId);

CREATE TABLE IF NOT EXISTS TemporaryConnection (
    id INT AUTO_INCREMENT PRIMARY KEY,
    system1Id INT NOT NULL,
    system2Id INT NOT NULL,
    characterId INT NOT NULL,
    system1Name VARCHAR(255) NOT NULL,
    system2Name VARCHAR(255) NOT NULL,
    created DATETIME(6) NOT NULL
);
ALTER TABLE TemporaryConnection ADD CONSTRAINT TemporaryConnection_system1Id_system2Id_characterId_unique
    UNIQUE (system1Id, system2Id, characterId);
