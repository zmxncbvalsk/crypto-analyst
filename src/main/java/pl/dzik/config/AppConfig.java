package pl.dzik.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import pl.dzik.exception.ApplicationException;

/**
 * Zarządza konfiguracją aplikacji
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class AppConfig {
    private static final Logger logger = LogManager.getLogger(AppConfig.class);

    /**
     * Magazyn właściwości konfiguracyjnych.
     */
    private static final Properties prop = new Properties();

    /**
     * Ładuje konfigurację z pliku lub tworzy domyślną, jeśli plik nie istnieje.
     *
     * @throws ApplicationException gdy wystąpi błąd odczytu/zapisu pliku konfiguracyjnego
     */
    public static void load() throws ApplicationException {
        logger.debug("Ładowanie konfiguracji aplikacji...");
        prop.clear();
        Path file = configFile();
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                prop.load(input);
                logger.info("Pomyślnie załadowano konfigurację aplikacji.");
            } catch (IOException e) {
                logger.error("Błąd podczas ładowania konfiguracji aplikacji", e);
                throw new ApplicationException("Nie udało się załadować konfiguracji aplikacji.", e);
            }
        } else {
            logger.debug("Tworzenie domyślnej konfiguracji aplikacji.");
            setDefaults();
            logger.info("Pomyślnie utworzono domyślną konfigurację.");
            save();
        }
    }

    /**
     * Zapisuje aktualny stan konfiguracji do pliku.
     *
     * @throws ApplicationException gdy zapis do pliku nie powiedzie się
     */
    public static synchronized void save() throws ApplicationException {
        logger.debug("Zapisywanie konfiguracji aplikacji...");
        try {
            Path file = configFile();
            Files.createDirectories(file.getParent());
            try (OutputStream os = Files.newOutputStream(file)) {
                prop.store(os, "Konfiguracja aplikacji");
                logger.info("Pomyślnie zapisano konfiguracje aplikacji.");
            }
        } catch (IOException e) {
            logger.error("Błąd podczas zapisywania konfiguracji aplikacji.", e);
            throw new ApplicationException("Nie udało się zapisać konfiguracji aplikacji.", e);
        }
    }

    /**
     * Zwraca wartość właściwości dla podanego klucza.
     *
     * @param key          klucz konfiguracji
     * @param defaultValue wartość domyślna zwracana, gdy klucz nie istnieje lub ma wartość null
     * @return wartość konfiguracji lub {@code defaultValue}
     */
    public static synchronized String get(String key, String defaultValue) {
        return prop.getProperty(key, defaultValue);
    }

    /**
     * Zwraca wartość właściwości dla podanego klucza.
     *
     * @param key klucz konfiguracji
     * @return wartość konfiguracji lub {@code null}, jeśli klucz nie istnieje
     */
    public static synchronized String get(String key) {
        return prop.getProperty(key);
    }

    /**
     * Ustawia nową wartość dla klucza i natychmiast zapisuje konfigurację do pliku.
     *
     * @param key   klucz konfiguracji
     * @param value nowa wartość
     * @throws ApplicationException gdy zapis do pliku się nie powiedzie
     */
    public static synchronized void set(String key, String value) throws ApplicationException {
        prop.setProperty(key, value);
        save();
    }

    /**
     * Ustawia domyślne wartości konfiguracji.
     */
    private static void setDefaults() {
        logger.debug("Tworzenie domyślnej konfiguracji aplikacji...");
        prop.setProperty("db.url", "jdbc:sqlite:cryptoanalyst.db");
        prop.setProperty("api.url", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd");
        prop.setProperty("app.interval", "1");
    }

    /**
     * Zwraca ścieżkę do pliku konfiguracyjnego w katalogu użytkownika.
     *
     * @return ścieżka do pliku konfiguracyjnego
     */
    private static Path configFile() {
        return Path.of(System.getProperty("user.home"), ".crypto-analyst", "config.properties");
    }
}
