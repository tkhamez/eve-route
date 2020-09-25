
ALTER TABLE Ansiblex RENAME TO AnsiblexV02;

CREATE TABLE IF NOT EXISTS Ansiblex (
    id BIGINT NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    solarSystemId INT NOT NULL,
    CONSTRAINT PK_Ansiblex PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS AnsiblexAlliance (
    ansiblexId BIGINT NOT NULL,
    allianceId INT NOT NULL,
    CONSTRAINT PK_AnsiblexAlliance PRIMARY KEY (ansiblexId, allianceId),
    CONSTRAINT fk_AnsiblexAlliance_ansiblexId_id FOREIGN KEY (ansiblexId) REFERENCES Ansiblex(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_AnsiblexAlliance_allianceId_id FOREIGN KEY (allianceId) REFERENCES Alliance(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- copy data

INSERT INTO Ansiblex (id, "name", solarSystemId)
SELECT DISTINCT ansiblexId, "name", solarSystemId FROM AnsiblexV02;

INSERT INTO AnsiblexAlliance (ansiblexId, allianceId)
SELECT ansiblexId, allianceId FROM AnsiblexV02;

DROP TABLE AnsiblexV02;
