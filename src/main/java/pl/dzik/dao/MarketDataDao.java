package pl.dzik.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pl.dzik.database.DatabaseManager;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.MarketData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * DAO odpowiedzialny za operacje na tabeli {@code market_data}.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class MarketDataDao {
    private static final Logger logger = LogManager.getLogger(MarketDataDao.class);

    /**
     * Zapisuje nową próbkę danych rynkowych dla kryptowaluty.
     *
     * @param data obiekt z aktualnymi danymi rynkowymi
     * @throws DatabaseException gdy wystąpi błąd podczas zapisu do bazy
     */
    public void save(MarketData data) throws DatabaseException {
        String sql = """
                    INSERT INTO market_data (crypto_id, price, volume, market_cap, change_24h, recorded_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, data.cryptoId());
            ps.setString(2, data.price().toString());
            ps.setString(3, data.volume().toString());
            ps.setString(4, data.marketCap().toString());
            ps.setString(5, data.change24h().toString());
            ps.setString(6, data.recordedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Błąd zapisu próbki historycznej dla crypto_id: {}", data.cryptoId(), e);
            throw new DatabaseException("Nie udało się zarchiwizować notowań rynkowych.", e);
        }
    }

    /**
     * Pobiera najnowszą zapisaną próbkę danych rynkowych dla danej kryptowaluty.
     *
     * @param cryptoId identyfikator kryptowaluty w lokalnej bazie
     * @return najnowszy obiekt {@link MarketData} lub {@code null}, jeśli brak danych
     * @throws DatabaseException gdy wystąpi błąd podczas odczytu z bazy lub mapowania danych
     */
    public MarketData findLatestByCryptoId(int cryptoId) throws DatabaseException {
        String sql = """
                    SELECT id, crypto_id, price, volume, market_cap, change_24h, recorded_at
                    FROM market_data
                    WHERE crypto_id = ?
                    ORDER BY recorded_at DESC LIMIT 1
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cryptoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMarketData(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Błąd pobierania najnowszych notowań dla crypto_id: {}", cryptoId, e);
            throw new DatabaseException("Nie udało się pobrać aktualnych danych historycznych.", e);
        }
        return null;
    }

    /**
     * Mapuje wiersz wyniku zapytania na obiekt {@link MarketData}.
     *
     * @param rs wynik SQL
     * @return zmapowane dane rynkowe
     * @throws DatabaseException gdy dane w bazie są uszkodzone, null lub niepoprawne
     */
    private MarketData mapRowToMarketData(ResultSet rs) throws DatabaseException {
        try {
            return new MarketData(
                    rs.getInt("id"),
                    rs.getInt("crypto_id"),
                    rs.getBigDecimal("price"),
                    rs.getBigDecimal("volume"),
                    rs.getBigDecimal("market_cap"),
                    rs.getBigDecimal("change_24h"),
                    LocalDateTime.parse(rs.getString("recorded_at"))
            );
        } catch (SQLException | DateTimeParseException | IllegalArgumentException e) {
            logger.error("Błąd parsowania lub odczytu danych rynkowych z bazy.", e);
            throw new DatabaseException("Nie udało się odczytać danych rynkowych z bazy.", e);
        }
    }
}
