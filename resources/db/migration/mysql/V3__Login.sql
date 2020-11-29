
CREATE TABLE IF NOT EXISTS Login (
    id INT AUTO_INCREMENT PRIMARY KEY,
    characterId INT NOT NULL,
    `year` INT NOT NULL,
    `month` INT NOT NULL,
    `count` INT NOT NULL
);

ALTER TABLE Login ADD CONSTRAINT Login_characterId_year_month_unique UNIQUE (characterId, `year`, `month`);
