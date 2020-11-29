
CREATE TABLE IF NOT EXISTS login (
    id SERIAL PRIMARY KEY,
    "characterId" INT NOT NULL,
    "year" INT NOT NULL,
    "month" INT NOT NULL,
    "count" INT NOT NULL
);

ALTER TABLE login ADD CONSTRAINT login_characterid_year_month_unique UNIQUE ("characterId", "year", "month");
