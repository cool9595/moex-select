# MOEX Select Backend

Java/Spring Boot REST API сервиса **MOEX Select** от **Delta team**. Backend загружает рыночные данные MOEX ISS через `WebClient`, нормализует их, применяет rule-based recommendation engine и возвращает пользовательские карточки инструментов.

> Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем параметрах и открытых рыночных данных MOEX ISS.

## Запуск

Требуется JDK 21+.

```powershell
.\mvnw.cmd spring-boot:run
```

API будет доступен на `http://localhost:8000`, Swagger UI - на `http://localhost:8000/swagger-ui.html`.

## Endpoints

- `GET /api/health` - состояние API.
- `GET /api/instruments` - пагинированный каталог с `assetClass`, `query`, `limit`, `page`, `sortBy`, `sortDirection` и диапазонами цены, доходности, объема, оборота, волатильности, сроков погашения, капитализации, типа опциона и strike price.
- `GET /api/instruments/{ticker}` - нормализованные сведения об инструменте.
- `POST /api/recommendations` - публичная подборка без внутренних оценок.
- `POST /api/recommendations?debug=true` - диагностический ответ с `internalScores`.

## Пакеты

- `controller/` - HTTP endpoints.
- `service/MoexIssClient.java` - вызовы ISS и парсинг таблиц `columns + data`.
- `service/InstrumentNormalizationService.java` - единая модель инструмента.
- `service/InstrumentService.java` - каталог, поиск, кэш и резервные данные.
- `service/ScoringService.java` - внутреннее ранжирование.
- `service/ExplanationService.java` - пользовательские объяснения и сообщения для сложных инструментов.
- `service/RecommendationService.java` - сборка результата подбора.
- `dto/`, `enums/`, `model/` - контракт API и доменная модель.

## Тесты

```powershell
.\mvnw.cmd test
```
