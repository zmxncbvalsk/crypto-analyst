package pl.dzik.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.dzik.exception.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Inicjalizator bazy danych.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class DatabaseInitializer {
    private static final Logger logger = LogManager.getLogger(DatabaseInitializer.class);

    /**
     * Inicjalizuje strukturę bazy danych – tworzy brakujące tabele i klucze obce.
     *
     * @throws DatabaseException gdy wystąpi błąd SQL podczas tworzenia tabel
     */
    public static void init() throws DatabaseException {
        logger.info("Rozpoczęcie sprawdzania i inicjalizacji tabel strukturalnych bazy danych...");
        try (Connection connection = DatabaseManager.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                        CREATE TABLE IF NOT EXISTS crypto(
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            coingecko_id TEXT NOT NULL UNIQUE,
                            symbol TEXT NOT NULL,
                            name TEXT NOT NULL,
                            image TEXT NOT NULL,
                            market_cap_rank INTEGER NOT NULL
                        );
                    """);
            statement.execute("""
                        CREATE TABLE IF NOT EXISTS watchlist(
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            crypto_id INTEGER NOT NULL UNIQUE,
                            is_active INTEGER NOT NULL DEFAULT 1,
                            FOREIGN KEY (crypto_id) REFERENCES crypto(id) ON DELETE CASCADE
                        );
                    """);
            statement.execute("""
                        CREATE TABLE IF NOT EXISTS alert(
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            crypto_id INTEGER NOT NULL,
                            target_price NUMERIC NOT NULL,
                            direction TEXT NOT NULL,
                            is_triggered INTEGER NOT NULL DEFAULT 0,
                            created_at TEXT NOT NULL,
                            triggered_at TEXT,
                            FOREIGN KEY (crypto_id) REFERENCES crypto(id) ON DELETE CASCADE
                        );
                    """);
            statement.execute("""
                        CREATE TABLE IF NOT EXISTS market_data(
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            crypto_id INTEGER NOT NULL,
                            price NUMERIC NOT NULL,
                            volume NUMERIC NOT NULL,
                            market_cap NUMERIC NOT NULL,
                            change_24h NUMERIC NOT NULL,
                            recorded_at TEXT NOT NULL,
                            FOREIGN KEY (crypto_id) REFERENCES crypto(id) ON DELETE CASCADE
                        );
                    """);
            statement.execute("""
                        CREATE INDEX IF NOT EXISTS idx_market_data_crypto_date 
                        ON market_data (crypto_id, recorded_at DESC);
                    """);
            logger.info("Wszystkie tabele bazy danych zostały zweryfikowane pomyślnie.");
        } catch (SQLException e) {
            logger.error("Błąd krytyczny podczas inicjalizacji tabel SQL.", e);
            throw new DatabaseException("Nie udało się utworzyć struktur bazy danych.", e);
        }
    }
}
