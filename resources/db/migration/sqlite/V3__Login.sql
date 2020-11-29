
CREATE TABLE IF NOT EXISTS Login (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    characterId INT NOT NULL,
    "year" INT NOT NULL,
    "month" INT NOT NULL,
    "count" INT NOT NULL
);

CREATE UNIQUE INDEX Login_characterId_year_month ON Login (characterId, "year", "month");
