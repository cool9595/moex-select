# MOEX Select: требования и архитектура

**Команда:** Delta team  
**Слоган:** Человекоориентированный слой подбора финансовых инструментов поверх MOEX ISS.

> Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем параметрах и открытых рыночных данных MOEX ISS.

## Назначение

MOEX Select помогает частному инвестору найти инструменты Московской Биржи по выбранным параметрам: цели, уровню риска, горизонту, сумме, опыту и интересующим классам инструментов. Продукт превращает технические данные MOEX ISS в понятный пользовательский сценарий с объяснимой подборкой и ссылками на страницы инструментов.

Система подбирает и ранжирует инструменты по заданной методике. Она не формулирует действие пользователя и не обещает результат инвестирования.

## Пользовательский сценарий

1. Пользователь заполняет форму подбора.
2. Backend получает и нормализует открытые данные MOEX ISS.
3. Система применяет фильтрацию и внутреннее ранжирование.
4. Пользователь получает карточки инструментов с понятными причинами включения в подборку.
5. Пользователь может открыть страницу инструмента на сайте MOEX.

## Ввод пользователя

| Поле | Значения |
| --- | --- |
| `goal` | `CAPITAL_PRESERVATION`, `STABLE_INCOME`, `CAPITAL_GROWTH`, `SPECULATION` |
| `riskProfile` | `LOW`, `MEDIUM`, `HIGH` |
| `horizon` | `SHORT`, `MEDIUM`, `LONG` |
| `budget` | число от `0` |
| `experience` | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |
| `assetClasses` | `STOCK`, `BOND`, `FUTURE`, `OPTION` |
| `limit` | от `1` до `50` |

## Данные в интерфейсе

Пользователь видит:

- тикер, название и класс инструмента;
- цену, валюту, расчетную доходность и дату погашения, когда эти данные доступны;
- человекочитаемые уровни риска и ликвидности;
- признак соответствия профилю;
- причины попадания в подборку;
- сообщение о сложности фьючерсов и опционов;
- кнопку перехода на страницу MOEX.

Внутренние числовые оценки предназначены только для порядка выдачи и отсутствуют в обычном пользовательском ответе API и в интерфейсе.

## Backend

Backend реализован на Java 21 и Spring Boot 3:

```text
src/main/java/com/moexdelta/moexselect/
  controller/
    HealthController.java
    InstrumentController.java
    RecommendationController.java
  service/
    MoexIssClient.java
    InstrumentNormalizationService.java
    InstrumentService.java
    ScoringService.java
    ExplanationService.java
    RecommendationService.java
  dto/
    HealthResponse.java
    InstrumentDto.java
    RecommendationRequest.java
    RecommendationResponse.java
    PublicInstrumentRecommendation.java
    InternalScores.java
  model/
    Instrument.java
    UserProfile.java
  enums/
    AssetClass.java
    Goal.java
    RiskProfile.java
    Horizon.java
    Experience.java
    UserProfileType.java
    Level.java
  config/
    WebClientConfig.java
    CorsConfig.java
  exception/
    GlobalExceptionHandler.java
  data/
    ReserveInstrumentCatalog.java
```

## Frontend

React/Vite/TypeScript интерфейс:

- использует публичный `POST /api/recommendations` без диагностического параметра;
- показывает форму сверху или слева и результаты ниже или справа в зависимости от ширины экрана;
- использует строгую бело-красную визуальную систему без официального логотипа;
- сохраняет disclaimer видимым на главной странице.

## Данные и устойчивость

`MoexIssClient` обращается к MOEX ISS через `WebClient`. `InstrumentService` хранит полученный каталог в памяти пять минут. Если отдельный набор ISS временно недоступен, для соответствующего класса используется встроенный резервный каталог, сохраняя работу пользовательского сценария.

Подробности приведены в [iss-integration.md](iss-integration.md), логика ранжирования - в [recommendation-engine.md](recommendation-engine.md), HTTP-контракт - в [api-contract.md](api-contract.md).
