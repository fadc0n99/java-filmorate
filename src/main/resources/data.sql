-- Данные для рейтингов
INSERT INTO mpa_ratings (name, description)
SELECT * FROM (VALUES
    ('G', 'Нет возрастных ограничений'),
    ('PG', 'Рекомендуется присутствие родителей'),
    ('PG-13', 'Детям до 13 лет просмотр не желателен'),
    ('R', 'Лицам до 17 лет только с родителями'),
    ('NC-17', 'Лицам до 18 лет просмотр запрещён')
) AS vals(name, description)
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