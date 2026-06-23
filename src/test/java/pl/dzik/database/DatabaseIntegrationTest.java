package pl.dzik.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseIntegrationTest {

    private Connection connection;

    @BeforeEach
    void setUpInMemoryDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("""
                CREATE TABLE crypto(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    coingecko_id TEXT NOT NULL UNIQUE
                );
            """);
            stmt.execute("""
                CREATE TABLE watchlist(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    crypto_id INTEGER NOT NULL,
                    FOREIGN KEY (crypto_id) REFERENCES crypto(id) ON DELETE CASCADE
                );
            """);
        }
    }

    @Test
    @DisplayName("Usunięcie waluty z tabeli crypto powinno automatycznie usunąć powiązany rekord z watchlisty (ON DELETE CASCADE)")
    void testCascadeDelete_CryptoRemoval_ClearsWatchlist() throws SQLException {
        int cryptoId;
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO crypto(coingecko_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "bitcoin");
            pstmt.executeUpdate();
            ResultSet keys = pstmt.getGeneratedKeys();
            assertTrue(keys.next());
            cryptoId = keys.getInt(1);
        }
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO watchlist(crypto_id) VALUES (?)")) {
            pstmt.setInt(1, cryptoId);
            pstmt.executeUpdate();
        }
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM watchlist")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
        try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM crypto WHERE id = ?")) {
            pstmt.setInt(1, cryptoId);
            pstmt.executeUpdate();
        }
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM watchlist")) {
            rs.next();
            assertEquals(0, rs.getInt(1), "Rekord z watchlisty nie został usunięty kaskadowo!");
        }
    }
}