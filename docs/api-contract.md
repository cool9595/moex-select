# API contract

Base URL локального backend: `http://localhost:8000/api`

> Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем параметрах и открытых рыночных данных MOEX ISS.

JSON-поля используют `camelCase`, перечисления передаются в верхнем регистре.

## GET /health

Ответ `200 OK`:

```json
{
  "status": "ok",
  "service": "MOEX Select API"
}
```

## GET /instruments

Возвращает нормализованные инструменты.

| Query parameter | Описание | По умолчанию |
| --- | --- | --- |
| `assetClass` | `STOCK`, `BOND`, `FUTURE`, `OPTION` | все классы |
| `query` | часть тикера или названия | без фильтра |
| `limit` | число строк, `1..200` | `50` |
| `page` | страница, начиная с `0` | `0` |
| `sortBy` | `ticker`, `name`, `price` | `ticker` |

Пример:

```http
GET /api/instruments?assetClass=STOCK&query=sber&limit=20&page=0&sortBy=ticker
```

Элемент ответа:

```json
{
  "ticker": "SBER",
  "name": "Сбербанк",
  "assetClass": "STOCK",
  "price": 315.4,
  "currency": "RUB",
  "yieldValue": 8.2,
  "volume": 18500000,
  "turnover": 5820000000,
  "maturityDate": null,
  "board": "TQBR",
  "raw": {}
}
```

## GET /instruments/{ticker}

Возвращает нормализованные сведения о найденном тикере. Для отсутствующего инструмента возвращается `404 Not Found`.

## POST /recommendations

Тело запроса:

```json
{
  "goal": "STABLE_INCOME",
  "riskProfile": "LOW",
  "horizon": "MEDIUM",
  "budget": 200000,
  "experience": "BEGINNER",
  "assetClasses": ["BOND", "STOCK"],
  "limit": 10
}
```

Публичный ответ:

```json
{
  "userProfile": "CONSERVATIVE",
  "disclaimer": "Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем параметрах и открытых рыночных данных MOEX ISS.",
  "recommendations": [
    {
      "ticker": "RU000A1014L8",
      "name": "ОФЗ 26235",
      "assetClass": "BOND",
      "price": 94.7,
      "currency": "RUB",
      "yieldValue": 13.8,
      "maturityDate": "2031-03-12",
      "riskLevel": "LOW",
      "liquidityLevel": "HIGH",
      "profileMatch": true,
      "explanation": [
        "Соответствует выбранному уровню риска.",
        "Имеет достаточную ликвидность для частного инвестора."
      ],
      "warnings": [],
      "moexUrl": "https://www.moex.com/ru/issue.aspx?board=TQOB&code=RU000A1014L8"
    }
  ]
}
```

Поля карточки:

| Поле | Назначение |
| --- | --- |
| `riskLevel` | понятный уровень риска: `LOW`, `MEDIUM`, `HIGH` |
| `liquidityLevel` | понятный уровень ликвидности: `LOW`, `MEDIUM`, `HIGH` |
| `profileMatch` | сильное совпадение с выбранным риском |
| `explanation` | причины включения карточки в результат |
| `warnings` | сообщение для сложного финансового инструмента, если применимо |
| `moexUrl` | ссылка для перехода на сайт MOEX |

## Diagnostic mode

`POST /api/recommendations?debug=true` использует тот же запрос, но добавляет поле `internalScores`:

```json
{
  "internalScores": {
    "finalScore": 84.7,
    "liquidityScore": 82,
    "yieldScore": 71,
    "riskScore": 88,
    "creditQualityScore": 90,
    "fitScore": 94
  }
}
```

`internalScores` предназначены для проверки ранжирования командой. Публичный frontend отправляет запрос без `debug` и не отображает это поле.

## MOEX URLs

Если у инструмента известен режим торгов:

```text
https://www.moex.com/ru/issue.aspx?board={board}&code={ticker}
```

Если режим отсутствует:

```text
https://www.moex.com/ru/issue.aspx?code={ticker}
```
