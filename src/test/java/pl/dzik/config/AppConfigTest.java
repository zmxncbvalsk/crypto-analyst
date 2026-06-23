package pl.dzik.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.dzik.config.AppConfig;
import pl.dzik.exception.ApplicationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class AppConfigTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void resetConfig() throws Exception {
        System.setProperty("user.home", tempDir.toString());
        Path configDir = tempDir.resolve(".crypto-analyst");
        Files.createDirectories(configDir);
        Files.deleteIfExists(configDir.resolve("config.properties"));
    }

    @Test
    @DisplayName("load tworzy domyślną konfigurację, gdy plik nie istnieje")
    void load_NoFile_CreatesDefaultConfiguration() throws ApplicationException {
        AppConfig.load();
        assertEquals("jdbc:sqlite:cryptoanalyst.db", AppConfig.get("db.url"));
        assertEquals("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd", AppConfig.get("api.url"));
        assertEquals("1", AppConfig.get("app.interval"));
    }

    @Test
    @DisplayName("get zwraca wartość domyślną, gdy klucz nie istnieje")
    void get_MissingKey_ReturnsDefault() throws ApplicationException {
        AppConfig.load();
        assertEquals("fallback", AppConfig.get("nie.istnieje", "fallback"));
    }

    @Test
    @DisplayName("set zapisuje nową wartość do pliku konfiguracyjnego")
    void set_NewValue_SavesToFile() throws Exception {
        AppConfig.load();
        AppConfig.set("app.interval", "5");
        assertEquals("5", AppConfig.get("app.interval"));
        Path configFile = tempDir.resolve(".crypto-analyst").resolve("config.properties");
        assertTrue(Files.exists(configFile));
        Properties props = new Properties();
        try (var is = Files.newInputStream(configFile)) {
            props.load(is);
        }
        assertEquals("5", props.getProperty("app.interval"));
    }
}