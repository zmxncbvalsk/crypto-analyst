package pl.dzik.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pl.dzik.database.DatabaseManager;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Watchlist;

/**
 * DAO odpowiedzialny za operacje na tabeli {@code watchlist}.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class WatchlistDao {
    private static final Logger logger = LogManager.getLogger(WatchlistDao.class);

    /**
     * Dodaje kryptowalutę do listy obserwowanych (jeśli jeszcze jej tam nie ma).
     *
     * @param cryptoId identyfikator kryptowaluty z tabeli {@code crypto}
     * @throws DatabaseException gdy wystąpi błąd podczas zapisu
     */
    public void add(int cryptoId) throws DatabaseException {
        String sql = """
                INSERT OR IGNORE INTO watchlist (crypto_id, is_active)
                VALUES(?,1)
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cryptoId);
            ps.executeUpdate();
            logger.info("Dodano crypto_id: {} do listy obserwowanych.", cryptoId);
        } catch (SQLException e) {
            logger.error("Błąd podczas dodawania do listy obserwowanych dla crypto_id: {}", cryptoId, e);
            throw new DatabaseException("Nie udało się dodać pozycji do listy obserwowanych", e);
        }
    }

    /**
     * Usuwa kryptowalutę z listy obserwowanych na podstawie jej ID.
     *
     * @param cryptoId identyfikator kryptowaluty
     * @throws DatabaseException gdy operacja usunięcia nie powiedzie się
     */
    public void removeByCryptoId(int cryptoId) throws DatabaseException {
        String sql = """
                DELETE FROM watchlist WHERE crypto_id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cryptoId);
            ps.executeUpdate();
            logger.info("Usunięto crypto_id: {} z listy obserwowanych.", cryptoId);
        } catch (SQLException e) {
            logger.error("Błąd podczas usuwania z listy obserwowanych dla crypto_id: {}", cryptoId, e);
            throw new DatabaseException("Nie udało się usunąć pozycji z listy obserwowanych.", e);
        }
    }

    /**
     * Przełącza status aktywności kryptowaluty (włącz/wyłącz obserwację).
     *
     * @param watchlistId identyfikator rekordu w tabeli {@code watchlist} (crypto_id)
     * @throws DatabaseException gdy aktualizacja się nie powiedzie
     */
    public void toggleStatus(int watchlistId) throws DatabaseException {
        String sql = """
                UPDATE watchlist
                SET is_active =
                    CASE WHEN is_active = 1 THEN 0
                    ELSE 1 END WHERE id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, watchlistId);
            ps.executeUpdate();
            logger.info("Zmieniono status aktywności dla pozycji watchlist ID: {}", watchlistId);
        } catch (SQLException e) {
            logger.error("Błąd zmiany statusu dla pozycji watchlist ID: {}", watchlistId, e);
            throw new DatabaseException("Nie udało się przełączyć statusu monitorowania.", e);
        }
    }

    /**
     * Zwraca wszystkie pozycje z listy obserwowanych.
     *
     * @return lista wszystkich obiektów {@link Watchlist}
     * @throws DatabaseException gdy wystąpi błąd podczas odczytu z bazy
     */
    public List<Watchlist> findAll() throws DatabaseException {
        List<Watchlist> list = new ArrayList<>();
        String sql = "SELECT id, crypto_id, is_active FROM watchlist";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Watchlist(
                        rs.getInt("id"),
                        rs.getInt("crypto_id"),
                        rs.getInt("is_active") == 1)
                );
            }
        } catch (SQLException e) {
            logger.error("Błąd podczas pobierania całej listy obserwowanych.", e);
            throw new DatabaseException("Nie udało się załadować listy obserwowanych.", e);
        }
        return list;
    }
}
