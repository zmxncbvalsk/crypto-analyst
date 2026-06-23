package pl.dzik.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.dzik.database.DatabaseManager;
import pl.dzik.enums.Direction;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Alert;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO odpowiedzialny za operacje na alertach cenowych w bazie danych.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class AlertDao {
    private static final Logger logger = LogManager.getLogger(AlertDao.class);

    /**
     * Dodaje nowy alert cenowy do bazy danych
     *
     * @param alert obiekt alertu do zapisania
     * @throws DatabaseException gdy wystąpi błąd podczas zapisu do bazy
     */
    public void add(Alert alert) throws DatabaseException {
        String sql = """
                INSERT INTO alert (crypto_id, target_price, direction, is_triggered, created_at)
                VALUES (?, ?, ?, 0, ?)
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alert.cryptoId());
            ps.setString(2, alert.targetPrice().toString());
            ps.setString(3, alert.direction().name());
            ps.setString(4, alert.createdAt().toString());
            ps.executeUpdate();
            logger.info("Zapisano nowy alert dla crypto_id: {}", alert.cryptoId());
        } catch (SQLException e) {
            logger.error("Błąd podczas dodawania alertu dla crypto_id: {}", alert.cryptoId(), e);
            throw new DatabaseException("Nie udało się utworzyć alertu cenowego.", e);
        }
    }

    /**
     * Zwraca listę wszystkich aktywnych (jeszcze nie wyzwolonych) alertów cenowych.
     *
     * @return lista aktywnych alertów
     * @throws DatabaseException gdy wystąpi błąd podczas odczytu z bazy lub mapowania danych
     */
    public List<Alert> findActiveAlerts() throws DatabaseException {
        List<Alert> alerts = new ArrayList<>();
        String sql = """
                SELECT
                    id,
                    crypto_id,
                    target_price,
                    direction,
                    is_triggered,
                    created_at,
                    triggered_at
                FROM alert
                WHERE is_triggered = 0
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                alerts.add(mapRowToAlert(rs));
            }
        } catch (SQLException e) {
            logger.error("Błąd podczas pobierania aktywnych alertów.", e);
            throw new DatabaseException("Nie udało się pobrać listy aktywnych alertów.", e);
        }
        return alerts;
    }

    /**
     * Oznacza alert jako wyzwolony i zapisuje datę aktywacji.
     *
     * @param id identyfikator alertu
     * @throws DatabaseException gdy aktualizacja się nie powiedzie
     */
    public void markAsTriggered(int id) throws DatabaseException {
        String sql = """
                UPDATE alert SET is_triggered = 1, triggered_at = ? WHERE id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setInt(2, id);
            ps.executeUpdate();
            logger.info("Alert o ID: {} został pomyślnie oznaczony jako wyzwolony.", id);
        } catch (SQLException e) {
            logger.error("Błąd podczas oznaczania alertu jako wyzwolony dla ID: {}", id, e);
            throw new DatabaseException("Nie udało się zaktualizować statusu alertu.", e);
        }
    }

    /**
     * Usuwa alert z bazy danych.
     *
     * @param id identyfikator alertu do usunięcia
     * @throws DatabaseException gdy operacja usunięcia nie powiedzie się
     */
    public void remove(int id) throws DatabaseException {
        String sql = """
                DELETE FROM alert WHERE id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            logger.info("Usunięto alert o ID: {}", id);
        } catch (SQLException e) {
            logger.error("Błąd podczas usuwania alertu o ID: {}", id, e);
            throw new DatabaseException("Nie udało się usunąć wybranego alertu.", e);
        }
    }

    /**
     * Mapuje wiersz wyniku zapytania na obiekt {@link Alert}.
     *
     * @param rs wynik SQL
     * @return zmapowany alert
     * @throws DatabaseException gdy dane w bazie są uszkodzone, null lub niepoprawne
     */
    public Alert mapRowToAlert(ResultSet rs) throws DatabaseException {
        try {
            String directionStr = rs.getString("direction");
            String createdAtStr = rs.getString("created_at");
            BigDecimal targetPrice = rs.getBigDecimal("target_price");
            if (directionStr == null || createdAtStr == null || targetPrice == null) {
                throw new DatabaseException("Wartości null w wierszu");
            }
            Direction direction = Direction.valueOf(directionStr);
            LocalDateTime createdAt = LocalDateTime.parse(createdAtStr);
            String triggeredAtStr = rs.getString("triggered_at");
            LocalDateTime triggeredAt = (triggeredAtStr != null) ? LocalDateTime.parse(triggeredAtStr) : null;
            return new Alert(
                    rs.getInt("id"),
                    rs.getInt("crypto_id"),
                    targetPrice,
                    direction,
                    rs.getInt("is_triggered") == 1,
                    createdAt,
                    triggeredAt
            );
        } catch (SQLException | DateTimeParseException | IllegalArgumentException e) {
            logger.error("Błąd parsowania lub odczytu danych alertu z SQLite.", e);
            throw new DatabaseException("Nie udało się odczytać danych alertu z bazy.", e);
        }
    }
}
