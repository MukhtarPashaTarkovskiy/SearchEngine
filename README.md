# SearchEngine

Учебный проект **поискового движка** на Java + Spring Boot.\
Приложение индексирует сайты, сохраняет их содержимое в базу данных
MySQL, выполняет лемматизацию текста и позволяет выполнять
полнотекстовый поиск по индексированным страницам.

------------------------------------------------------------------------

##  Функционал

-   Индексация сайтов из конфигурационного файла `application.yaml`
-   Возможность остановки и запуска индексации
-   Индексация отдельной страницы
-   Хранение информации в базе данных (сайты, страницы, леммы, индексы)
-   Лемматизация текста с помощью **Apache Lucene Morphology**
-   REST API для:
    -   запуска/остановки индексации
    -   получения статистики
    -   поиска по ключевым словам
-   Веб-интерфейс (Thymeleaf)

------------------------------------------------------------------------

## Технологии

-   **Java 17**
-   **Spring Boot 2.7.1**
-   **Spring Data JPA (Hibernate)**
-   **MySQL 8**
-   **Apache Lucene Morphology**
-   **JSoup**
-   **Thymeleaf**
-   **Lombok**

------------------------------------------------------------------------

##  Установка и запуск локально

### 1. Клонировать репозиторий

``` bash
git clone https://github.com/MukhtarPashaTarkovskiy/SearchEngine.git
cd SearchEngine
```

### 2. Настроить базу данных

Создать базу в MySQL:

``` sql
CREATE DATABASE search_engine CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Указать настройки подключения

В файле `application.yaml` задать свои данные:

``` yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/search_engine?useSSL=false&allowPublicKeyRetrieval=true
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### 4. Запуск приложения

Через Maven:

``` bash
mvn spring-boot:run
```

или через IDE (**Application.main()**).

После запуска приложение будет доступно по адресу:\
 <http://localhost:8080>

------------------------------------------------------------------------

##  REST API (основные эндпоинты)

-   `GET /api/startIndexing` --- запуск индексации
-   `GET /api/stopIndexing` --- остановка индексации
-   `POST /api/indexPage?url=...` --- индексация одной страницы
-   `GET /api/statistics` --- статистика
-   `GET /api/search?query=...` --- поиск

------------------------------------------------------------------------

##  Автор Tarkovskiy Mykhtar-Pasha

Проект выполнен в рамках курса **Skillbox**.
