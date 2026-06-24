# Crypto Analyst

Aplikacja desktopowa w Javie do monitorowania rynku kryptowalut, alertów cenowych oraz zapisu danych w lokalnej bazie SQLite. Dane rynkowe pobierane są z API CoinGecko.

---

## Funkcje

- Pobieranie TOP 250 kryptowalut z CoinGecko
- Monitorowanie kursów wybranych kryptowalut
- System alertów cenowych (próg cenowy)
- Watchlista użytkownika
- Historia i archiwizacja danych rynkowych
- GUI w JavaFX
- Lokalna baza danych SQLite
- Logowanie zdarzeń (INFO / WARN / ERROR)
- Konfiguracja przez plik `config.properties`

---

## Technologie

- Java 21
- JavaFX
- SQLite (JDBC)
- Gson
- Log4j2
- JUnit 5 + Mockito
- Maven

---

## Wymagania

- Java 21+
- Maven 3.8+

---

## Uruchomienie

### 1. Budowa projektu
```bash
mvn clean package
```

##  Uruchomienie aplikacji
```bash
java -jar target/crypto-analyst-1.0-SNAPSHOT.jar
```

## Instrukcja Użytkownika (Obsługa Programu)

Aplikacja `Crypto Analyst`
posiada intuicyjny interfejs graficzny podzielony na panele funkcjonalne.
Poniżej znajduje się opis podstawowych operacji:

### 1. Pierwsze uruchomienie i synchronizacja danych
* Po włączeniu aplikacji system automatycznie wysyła zapytanie do API CoinGecko w celu pobrania listy **TOP 250 kryptowalut**.
* Na dolnym pasku stanu zobaczysz komunikat `RUNNING (SYSTEM AKTYWNY)`.
* Jeśli ikona świeci się na zielono, oznacza to, że aplikacja poprawnie komunikuje się z siecią i bazą danych.

### 2. Zarządzanie listą obserwowanych (Watchlist)
*  W bocznym panelu znajduje się pole do wpisania kryptowaulty z autocomplite należy wpisać wybrać z listy i nacisnąć + w liście obserwowanych pojawi się kryptowaluta, można zmienić jej śledzenie albo usunąć

### 3. Konfiguracja alertów cenowych
*  Wypełnij formularz:
    * Wybierz kryptowalutę z listy
    * Wybierz kierunek warunku z listy rozwijanej: **ABOVE** (jeśli cena ma wzrosnąć do tego poziomu) lub **BELOW** (jeśli ma spaść).
    * Wpisz cenę docelową
    * Kliknij przycisk **"Zapisz alert"**.

* Gdy system w tle wykryje, że cena rynkowa osiągnęła Twój próg, na ekranie pojawi się powiadomienie wizualne, a status alertu zmieni się na `TRIGGERED` (Wyzwolony).
