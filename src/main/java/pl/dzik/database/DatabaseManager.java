package pl.dzik.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.dzik.config.AppConfig;
import pl.dzik.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Zarządza połączeniami z bazą danych SQLite.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);

    /**
     * Zwraca nowe połączenie z bazą danych
     *
     * @return aktywne połączenie z bazą danych
     * @throws DatabaseException gdy nie uda się nawiązać połączenia
     */
    public static Connection getConnection() throws DatabaseException {
        String dbUrl = AppConfig.get("db.url", "jdbc:sqlite:cryptoanalyst.db");
        try {
            Connection connection = DriverManager.getConnection(dbUrl);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
            return connection;
        } catch (SQLException e) {
            logger.error("Nie udało się otworzyć połączenia z bazą danych pod adresem: {}", dbUrl, e);
            throw new DatabaseException("Błąd połączenia z bazą danych.", e);
        }
    }
}
