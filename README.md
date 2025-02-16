### Перед запуском
Для работы API был создан docker-compose файл и Dockerfile. 
Однако перед запуском необходимо создать один файл, в котором хранятся приватные данные,
а посему он отсутствует в публичном доступе.

Для корректной работы нужно создать в корневой папке проекта файл .env и указать в нем следующие переменные:

DB_NAME=<имя БД>

DB_USER=<имя пользователя БД>

DB_PASSWORD=<пароль от БД>

JWT_SECRET_KEY=<секрет для генерации JWT> (должен быть не меньше 50 символов)

### Комментарии
От себя добавил еще refresh токены и запрос на получение нового access токена через refresh токен.
Refresh токены обновляются при использовании. Плюс добавил кэшик с редисом.
Для некоторых ручек поменял код ответа с 400 на 404, где было разумно.

При сборке отключил тесты, ибо валились докер образы из интеграционных тестов при инициализации.
Почему, к сожалению, я нарыть не смог. В целом не критичный момент, но решил указать это. 
Но тесты можно просто запустить через Gradle проверить,
и там все нормально отрабатывает:
```
./gradlew test jacocoTestReport
```

Схема ручки:
```
"paths": {
    "/api/refresh": {
        "post": {
            "summary": "Получить новый access токен.",
            "responses": {
                "200": {
                    "description": "Успешный ответ.",
                    "schema": {
                        "$ref": "#/definitions/AuthResponse"
                    }
                },
                "400": {
                    "description": "Неверный запрос.",
                    "schema": {
                        "$ref": "#/definitions/ErrorResponse"
                    }
                },
                "500": {
                    "description": "Внутренняя ошибка сервера.",
                    "schema": {
                        "$ref": "#/definitions/ErrorResponse"
                    }
                }
            },
            "parameters": [
                {
                    "required": true,
                    "name": "body",
                    "in": "body",
                    "schema": {
                        "$ref": "#/definitions/RefreshRequest"
                    }
                }
            ],
            "consumes": [
                "application/json"
            ],
            "produces": [
                "application/json"
            ]
        }
    }
}

"definitions": {
    "RefreshRequest": {
        "type": "object",
        "properties": {
            "refreshToken": {
                "type": "string",
                "description": "Токен для получения нового access токена."
            }
        },
        "required": [
            "refreshToken"
        ]
    }
}