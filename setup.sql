CREATE TABLE questions (
    id serial PRIMARY KEY,
    creationDate TIMESTAMPTZ,
    closedDate TIMESTAMPTZ,
    deletionDate TIMESTAMPTZ,
    score int,
    ownerUserId int,
    answerCount int
);

COPY questions FROM '/tmp/questions.csv'
WITH (HEADER, FORMAT CSV, NULL 'NA');

CREATE INDEX user_idx ON questions(ownerUserId);
