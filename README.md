# MOEX Select

**MOEX Select** - сервис команды **Delta team** для понятного подбора финансовых инструментов Московской Биржи по параметрам частного инвестора.

**Слоган:** Человекоориентированный слой подбора финансовых инструментов поверх MOEX ISS.

Сервис получает открытые рыночные данные через MOEX ISS, нормализует различающиеся поля рынков, применяет объяснимое rule-based ранжирование и показывает карточки акций, облигаций, фьючерсов и опционов с причинами попадания в подборку и ссылками на сайт Московской Биржи.

> Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем параметрах и открытых рыночных данных MOEX ISS.

## Возможности

- Анкета с целью инвестирования, уровнем риска, горизонтом, суммой, опытом и классами инструментов.
- Данные акций, облигаций, фьючерсов и опционов из MOEX ISS с резервным каталогом при временной недоступности источника.
- Публичная выдача с ценой, валютой, доходностью и сроком погашения при наличии данных.
- Понятные уровни риска и ликвидности, причины подбора и сообщения для сложных инструментов.
- Переход к выбранному инструменту на сайте MOEX.
- Внутренние оценки релевантности только для сортировки; пользовательский интерфейс их не показывает.

## Технологии

- Backend: Java 21, Spring Boot 3, Spring Web, WebClient, Spring Validation, springdoc OpenAPI.
- Frontend: React, Vite, TypeScript, CSS.
- Источник рыночных данных: MOEX ISS API.
- Хранение данных: in-memory cache на пять минут и встроенный резервный каталог.

## Структура

```text
moex-select/
  backend/
    src/main/java/com/moexdelta/moexselect/
      config/
      controller/
      data/
      dto/
      enums/
      exception/
      model/
      service/
    src/test/java/com/moexdelta/moexselect/
  frontend/
    src/
      components/
  docs/
    project-requirements.md
    recommendation-engine.md
    api-contract.md
    iss-integration.md
    analytics-and-success-metrics.md
```

## Запуск На Windows

Требуются JDK 21+ и Node.js LTS.

Откройте первый PowerShell для backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Откройте второй PowerShell для frontend:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

После запуска:

- Интерфейс: `http://localhost:5173`
- REST API: `http://localhost:8000/api`
- Swagger UI: `http://localhost:8000/swagger-ui.html`

## Запуск На Linux/macOS

```bash
cd backend
./mvnw spring-boot:run
```

В другом терминале:

```bash
cd frontend
npm install
npm run dev
```

## REST API

`GET /api/health`

```json
{
  "status": "ok",
  "service": "MOEX Select API"
}
```

`GET /api/instruments?assetClass=STOCK&query=sber&limit=6&page=0&sortBy=turnover&sortDirection=desc`

Возвращает пагинированный каталог инструментов с частичным поиском по тикеру/названию, сортировкой и расширенными фильтрами. Поддерживаются `page`, `limit`, `sortBy`, `sortDirection`, диапазоны цены, доходности, объема, оборота, волатильности, сроков погашения, капитализации, типа опциона и strike price. Дополнительный запрос `GET /api/instruments/{ticker}` возвращает один инструмент.

`POST /api/recommendations`

```json
{
  "goal": "CAPITAL_GROWTH",
  "riskProfile": "MEDIUM",
  "horizon": "LONG",
  "budget": 100000,
  "experience": "BEGINNER",
  "assetClasses": ["STOCK", "BOND"],
  "limit": 10
}
```

Публичный ответ содержит профиль, disclaimer и карточки без числовых внутренних оценок:

```json
{
  "userProfile": "BALANCED",
  "disclaimer": "Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем параметрах и открытых рыночных данных MOEX ISS.",
  "recommendations": [
    {
      "ticker": "SBER",
      "name": "Сбербанк",
      "assetClass": "STOCK",
      "price": 315.4,
      "currency": "RUB",
      "riskLevel": "MEDIUM",
      "liquidityLevel": "HIGH",
      "profileMatch": true,
      "explanation": ["Соответствует выбранному уровню риска."],
      "warnings": [],
      "moexUrl": "https://www.moex.com/ru/issue.aspx?board=TQBR&code=SBER"
    }
  ]
}
```

Для валидации методики команда может вызвать `POST /api/recommendations?debug=true`. Только этот запрос добавляет к карточкам объект `internalScores`; frontend его не использует.

## MOEX ISS

Приложение запрашивает:

- Акции: `https://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/securities.json?iss.meta=off`
- Облигации: `https://iss.moex.com/iss/engines/stock/markets/bonds/securities.json?iss.meta=off`
- Фьючерсы: `https://iss.moex.com/iss/engines/futures/markets/forts/securities.json?iss.meta=off`
- Опционы: `https://iss.moex.com/iss/engines/futures/markets/options/securities.json?iss.meta=off`

MOEX ISS возвращает табличные блоки `columns` и `data`. `MoexIssClient` преобразует строки в карты, `InstrumentNormalizationService` объединяет сведения по `SECID` и приводит поля рынков к единой модели `Instrument`.

## Документация

- [Требования и архитектура](docs/project-requirements.md)
- [Recommendation engine](docs/recommendation-engine.md)
- [API contract](docs/api-contract.md)
- [ISS integration](docs/iss-integration.md)
- [Метрики продукта](docs/analytics-and-success-metrics.md)

## Проверка

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm.cmd run build
```

Правила командной разработки описаны в [CONTRIBUTING.md](CONTRIBUTING.md).
