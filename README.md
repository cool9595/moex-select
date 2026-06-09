# MOEX Select

**MOEX Select** - сервис команды **Delta team** для подбора и поиска финансовых инструментов Московской Биржи по параметрам частного инвестора.

Проект объединяет frontend на React/Vite и backend на Spring Boot. Backend получает открытые рыночные данные из MOEX ISS, нормализует разные классы инструментов в единую модель, применяет rule-based ранжирование и возвращает понятные карточки акций, облигаций, фьючерсов и опционов.

> Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем параметрах и открытых рыночных данных.

## Демо

Опубликованная версия доступна по адресу:

```text
http://45.144.178.175:5173/
```

Backend API на сервере ожидается по адресу:

```text
http://45.144.178.175:8000/api
```

## Возможности

- Два режима интерфейса: `Новичок` для анкетного подбора и `Опытный` для расширенного поиска.
- Подбор по цели, риск-профилю, горизонту, бюджету, опыту и выбранным классам инструментов.
- Расширенный поиск по тикеру или названию с фильтрами по цене, доходности, объему, обороту, волатильности, сроку погашения, капитализации, типу опциона и strike price.
- Пагинация и сортировка по ключевым рыночным показателям.
- Единые карточки инструментов с ценой, валютой, доходностью, датой погашения, уровнем риска, ликвидностью, уровнем уверенности и ссылкой на MOEX.
- Предупреждения для сложных инструментов, включая фьючерсы и опционы.
- Резервный каталог инструментов на случай временной недоступности MOEX ISS.
- Диагностический режим API с внутренними оценками ранжирования для проверки методики.

## Технологии

- **Backend:** Java 21, Spring Boot 3.5, Spring Web, WebClient, Spring Validation, springdoc OpenAPI.
- **Frontend:** React 19, Vite 6, TypeScript, CSS.
- **Данные:** MOEX ISS API.
- **Хранение:** in-memory cache на 5 минут и встроенный резервный каталог.
- **Инфраструктура:** Docker и Docker Compose.

## Структура проекта

```text
moex-select/
  docker-compose.yml
  backend/
    Dockerfile
    src/main/java/com/moexdelta/moexselect/
      config/
      controller/
      data/
      dto/
      enums/
      exception/
      model/
      service/
  frontend/
    Dockerfile
    src/
      components/
  docs/
    api-contract.md
    analytics-and-success-metrics.md
    iss-integration.md
    project-requirements.md
    recommendation-engine.md
```

## Быстрый запуск через Docker

Требуется установленный Docker с Compose.

Из корня проекта:

```bash
docker compose up --build
```

Будут запущены два контейнера:

- `moex-select-backend` - REST API на `http://localhost:8000`;
- `moex-select-frontend` - интерфейс на `http://localhost:5173`.

После старта откройте:

```text
http://localhost:5173
```

Swagger UI доступен на:

```text
http://localhost:8000/swagger-ui.html
```

Остановить контейнеры:

```bash
docker compose down
```

Пересобрать после изменений:

```bash
docker compose up --build --force-recreate
```

## Запуск на сервере

При запуске на сервере frontend должен знать публичный адрес backend API. Для текущего сервера можно создать файл `.env` в корне проекта:

```env
VITE_API_URL=http://45.144.178.175:8000/api
```

Затем запустить:

```bash
docker compose up --build -d
```

Проверка после запуска:

```bash
docker compose ps
curl http://45.144.178.175:8000/api/health
```

Ожидаемый ответ healthcheck:

```json
{
  "status": "ok",
  "service": "MOEX Select API"
}
```

В `CorsConfig` уже разрешены origins:

- `http://localhost:5173`
- `http://127.0.0.1:5173`
- `http://45.144.178.175:5173`

Если адрес сервера изменится, нужно обновить `VITE_API_URL` и список разрешенных origins в backend.

## Локальный запуск без Docker

Требуются JDK 21+ и Node.js LTS.

Backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

После запуска:

- интерфейс: `http://localhost:5173`
- REST API: `http://localhost:8000/api`
- Swagger UI: `http://localhost:8000/swagger-ui.html`

Для Linux/macOS команды аналогичны:

```bash
cd backend
./mvnw spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

## Основные API endpoints

### GET `/api/health`

Проверка состояния backend.

### GET `/api/instruments`

Возвращает пагинированный каталог инструментов.

Пример:

```http
GET /api/instruments?assetClass=STOCK&query=sber&limit=6&page=0&sortBy=turnover&sortDirection=desc
```

Основные query-параметры:

- `assetClass`: `STOCK`, `BOND`, `FUTURE`, `OPTION`
- `query`: часть тикера или названия
- `limit`, `page`
- `sortBy`: `ticker`, `name`, `price`, `yield`, `volume`, `turnover`, `liquidity`, `maturity`, `volatility`, `marketCap`, `strikePrice`
- `sortDirection`: `asc`, `desc`
- диапазоны: `minPrice`, `maxPrice`, `minYield`, `maxYield`, `minVolume`, `maxVolume`, `minTurnover`, `maxTurnover`, `minVolatility`, `maxVolatility`, `maturityFrom`, `maturityTo`, `minMarketCap`, `maxMarketCap`, `optionType`, `minStrikePrice`, `maxStrikePrice`

### GET `/api/instruments/{ticker}`

Возвращает один инструмент по тикеру. Если инструмент не найден, backend возвращает `404`.

### POST `/api/recommendations`

Возвращает подборку инструментов под профиль пользователя.

Пример тела запроса:

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

Поддерживается диагностический режим:

```http
POST /api/recommendations?debug=true
```

Он добавляет к карточкам поле `internalScores`. Публичный frontend эти оценки не показывает.

## MOEX ISS

Backend запрашивает открытые данные MOEX ISS по классам:

- акции: `stock/shares`
- облигации: `stock/bonds`
- фьючерсы: `futures/forts`
- опционы: `futures/options`

`MoexIssClient` читает табличные блоки `columns` и `data`, `InstrumentNormalizationService` приводит рынки к единой модели `Instrument`, а `InstrumentService` кэширует результат и подставляет резервные данные при сбоях источника.

## Документация

- [API contract](docs/api-contract.md)
- [ISS integration](docs/iss-integration.md)
- [Recommendation engine](docs/recommendation-engine.md)
- [Требования и архитектура](docs/project-requirements.md)
- [Метрики продукта](docs/analytics-and-success-metrics.md)
- [Правила командной разработки](CONTRIBUTING.md)

## Проверка

Backend tests:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend build:

```powershell
cd frontend
npm.cmd run build
```
