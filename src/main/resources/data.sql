-- Данные для рейтингов
INSERT INTO mpa_ratings (code, name, description)
SELECT * FROM (VALUES
    ('G', 'General Audiences', 'Нет возрастных ограничений'),
    ('PG', 'Parental Guidance Suggested', 'Рекомендуется присутствие родителей'),
    ('PG-13', 'Parents Strongly Cautioned', 'Детям до 13 лет просмотр не желателен'),
    ('R', 'Restricted', 'Лицам до 17 лет только с родителями'),
    ('NC-17', 'Adults Only', 'Лицам до 18 лет просмотр запрещён')
) AS vals(code, name, description)
WHERE NOT EXISTS (SELECT 1 FROM mpa_ratings);

-- Данные для жанров
INSERT INTO genres (name)
SELECT * FROM (VALUES
    ('Комедия'),
    ('Драма'),
    ('Мультфильм'),
    ('Триллер'),
    ('Документальный'),
    ('Боевик')
) AS vals(name)
WHERE NOT EXISTS (SELECT 1 FROM genres);