package pl.dzik.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pl.dzik.database.DatabaseManager;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Alert;
import pl.dzik.model.Crypto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO odpowiedzialny za operacje na tabeli {@code crypto}.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class CryptoDao {
    private static final Logger logger = LogManager.getLogger(CryptoDao.class);

    /**
     * Zapisuje nową kryptowalutę lub aktualizuje istniejącą (na podstawie {@code coingecko_id}).
     *
     * @param crypto obiekt kryptowaluty do zapisania/aktualizacji
     * @throws DatabaseException gdy wystąpi błąd SQL podczas zapisu
     */
    public void saveOrUpdate(Crypto crypto) throws DatabaseException {
        String sql = """
                INSERT INTO crypto(coingecko_id, symbol, name, image, market_cap_rank)
                VALUES(?,?,?,?,?)
                ON CONFLICT(coingecko_id) DO UPDATE SET
                    symbol = excluded.symbol,
                    name = excluded.name,
                    image = excluded.image,
                    market_cap_rank = excluded.market_cap_rank
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, crypto.coingeckoId());
            ps.setString(2, crypto.symbol());
            ps.setString(3, crypto.name());
            ps.setString(4, crypto.image());
            ps.setInt(5, crypto.marketCapRank());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Błąd podczas zapisu/aktualizacji kryptowaluty: {}", crypto.coingeckoId(), e);
            throw new DatabaseException("Nie udało sie zapisać danych kryptowaluty.", e);
        }
    }

    /**
     * Wyszukuje kryptowaluty, których nazwa lub symbol zawiera podaną frazę (wyszukiwanie częściowe).
     *
     * @param phrase fraza wyszukiwania
     * @return lista dopasowanych kryptowalut (posortowana domyślnie przez bazę)
     * @throws DatabaseException gdy wystąpi błąd podczas wykonywania zapytania
     */
    public List<Crypto> searchByPhrase(String phrase) throws DatabaseException {
        List<Crypto> results = new ArrayList<>();
        String sql = """
                SELECT
                    id,
                    coingecko_id,
                    symbol,
                    name,
                    image,
                    market_cap_rank
                FROM crypto
                WHERE
                    name LIKE ?
                    OR symbol LIKE ?
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String likeStr = "%" + phrase + "%";
            ps.setString(1, likeStr);
            ps.setString(2, likeStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToCrypto(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Błąd podczas wyszukiwania kryptowaluty dla frazy: {}", phrase, e);
            throw new DatabaseException("Błąd podczas przeszukiwania słownika kryptowalut.", e);
        }
        return results;
    }

    /**
     * Pobiera kryptowalutę na podstawie identyfikatora CoinGecko.
     *
     * @param coingeckoId unikalny identyfikator z API CoinGecko
     * @return obiekt {@link Crypto} lub {@code null}, jeśli nie znaleziono
     * @throws DatabaseException gdy wystąpi błąd SQL
     */
    public Crypto findByCoinGeckoId(String coingeckoId) throws DatabaseException {
        String sql = """
                SELECT
                    id,
                    coingecko_id,
                    symbol,
                    name,
                    image,
                    market_cap_rank
                FROM crypto
                WHERE coingecko_id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coingeckoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCrypto(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Błąd podczas pobierania kryptowaluty o coingeckoId: {}", coingeckoId, e);
            throw new DatabaseException("Nie udało się pobrać kryptowaluty ze słownika", e);
        }
        return null;
    }

    /**
     * Pobiera kryptowalutę na podstawie klucza głównego (ID z bazy).
     *
     * @param id klucz główny rekordu w tabeli {@code crypto}
     * @return obiekt {@link Crypto} lub {@code null}, jeśli nie znaleziono
     * @throws DatabaseException gdy wystąpi błąd SQL
     */
    public Crypto findById(int id) throws DatabaseException {
        String sql = """
                SELECT
                    id,
                    coingecko_id,
                    symbol,
                    name,
                    image,
                    market_cap_rank
                FROM crypto
                WHERE id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCrypto(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Błąd podczas pobierania kryptowaluty o id: {}", id, e);
            throw new DatabaseException("Nie udało się pobrać kryptowaluty ze słownika", e);
        }
        return null;
    }

    /**
     * Mapuje wiersz wyniku zapytania na obiekt {@link Crypto}.
     *
     * @param rs wynik SQL
     * @return zmapowany alert
     * @throws DatabaseException gdy dane w bazie są uszkodzone, null lub niepoprawne
     */
    private Crypto mapRowToCrypto(ResultSet rs) throws DatabaseException {
        try {
            return new Crypto(
                    rs.getInt("id"),
                    rs.getString("coingecko_id"),
                    rs.getString("symbol"),
                    rs.getString("name"),
                    rs.getString("image"),
                    rs.getInt("market_cap_rank")
            );
        } catch (SQLException e) {
            logger.error("Błąd parsowania lub odczytu danych kryptowaluty z SQLite.", e);
            throw new DatabaseException("Nie udało się odczytać danych kryptowaluty z bazy.", e);
        }
    }
}
