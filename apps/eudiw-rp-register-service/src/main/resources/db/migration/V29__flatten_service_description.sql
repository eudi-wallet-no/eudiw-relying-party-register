UPDATE `wallet_relying_party_service`
SET `srv_description` = COALESCE((
    SELECT JSON_ARRAYAGG(JSON_OBJECT('lang', description.lang, 'content', description.content)
                         ORDER BY description.group_position, description.position)
    FROM JSON_TABLE(`wallet_relying_party_service`.`srv_description`, '$[*]' COLUMNS (
        group_position FOR ORDINALITY,
        NESTED PATH '$[*]' COLUMNS (
            position FOR ORDINALITY,
            lang VARCHAR(64) PATH '$.lang',
            content TEXT PATH '$.content'
        )
    )) AS description
), JSON_ARRAY())
WHERE `srv_description` IS NOT NULL
  AND JSON_TYPE(JSON_EXTRACT(`srv_description`, '$[0]')) = 'ARRAY';
