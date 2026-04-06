# Reminder & Notification Service

Standalone backend-сервис для создания напоминаний и асинхронной отправки уведомлений по разным каналам.

## Возможности

- создание напоминания
- получение напоминания по id
- список напоминаний с фильтром по статусу
- отмена напоминания
- ручной retry failed-напоминания
- scheduler-оркестрация отправки
- async dispatch через отдельный `TaskExecutor`
- retry policy с конфигурацией через `application.yml`
- Strategy pattern для каналов отправки (`LOG`, `WEBHOOK`, `EMAIL`)
- единый JSON-формат ошибок
- валидация входящих данных

## Технологии

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Bean Validation
- Scheduling + Async
- PostgreSQL
- Lombok
- Gradle

## Запуск

### Локально

```bash
./gradlew bootRun
```

### Через Docker Compose

```bash
docker compose up --build
```

## Конфигурация

Параметры сервиса:

- `reminder.max-attempts`
- `reminder.retry-delay-seconds`
- `reminder.scheduler-delay-ms`
- `reminder.webhook-timeout-ms`
- `reminder.async.core-pool-size`
- `reminder.async.max-pool-size`
- `reminder.async.queue-capacity`
- `reminder.async.thread-name-prefix`

Параметры подключения к БД задаются через env:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

## Каналы отправки и где задаются данные

- `LOG`: фейковый канал, отправляет в логи приложения. В `recipient` можно передавать любую строку.
- `WEBHOOK`: реальный HTTP POST. В `recipient` нужно передать полный URL вебхука (`http://...` или `https://...`).
- `EMAIL`: в текущей версии это stub-канал (без SMTP), он имитирует успешную отправку и пишет событие в лог.

Формат JSON, который уходит в `WEBHOOK`:

```json
{
  "reminderId": 1,
  "message": "Оплатить интернет",
  "subject": "Напоминание",
  "channel": "WEBHOOK",
  "scheduledAt": "2026-04-10T18:00:00Z"
}
```

## Примеры API

### Создать напоминание

```bash
curl -X POST http://localhost:8080/api/reminders \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Оплатить интернет",
    "subject": "Напоминание",
    "channel": "WEBHOOK",
    "recipient": "https://example.com/callback",
    "scheduledAt": "2026-04-10T18:00:00Z"
  }'
```

### Получить по id

```bash
curl http://localhost:8080/api/reminders/1
```

### Список по статусу

```bash
curl "http://localhost:8080/api/reminders?status=FAILED"
```

### Ручной retry

```bash
curl -X POST http://localhost:8080/api/reminders/1/retry
```

## Тесты

```bash
./gradlew test
```

Покрытие:
- unit test для `ReminderService`
- unit test выбора `NotificationSender` в `ReminderDispatchService`
- integration test для `ReminderController` и `WebhookNotificationSender`
