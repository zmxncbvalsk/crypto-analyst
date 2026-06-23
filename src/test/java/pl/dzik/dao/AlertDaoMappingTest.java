package pl.dzik.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dzik.enums.Direction;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Alert;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
public class AlertDaoMappingTest {
    @Mock
    private ResultSet rs;
    private final AlertDao alertDao = new AlertDao();

    @Test
    @DisplayName("mapRowToAlert poprawnie mapuje prawidłowy wiersz z bazy danych")
    void mapRowToAlert_ValidRow_ReturnsAlert() throws Exception{
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getInt("crypto_id")).thenReturn(10);
        when(rs.getBigDecimal("target_price")).thenReturn(new BigDecimal("50000.00"));
        when(rs.getString("direction")).thenReturn("ABOVE");
        when(rs.getInt("is_triggered")).thenReturn(0);
        when(rs.getString("created_at")).thenReturn("2026-06-23T12:00:00");
        when(rs.getString("triggered_at")).thenReturn(null);
        Alert result = alertDao.mapRowToAlert(rs);
        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals(10, result.cryptoId());
        assertEquals(new BigDecimal("50000.00"), result.targetPrice());
        assertEquals(Direction.valueOf("ABOVE"), result.direction());
        assertFalse(result.isTriggered());
        assertEquals(LocalDateTime.of(2026, 6, 23, 12, 0, 0), result.createdAt());
        assertNull(result.triggeredAt());
    }

    @Test
    @DisplayName("mapRowToAlert rzuca DatabaseException gdy created_at jest NULL")
    void mapRowToAlert_NullCreatedAt_ThrowDatabaseException() throws Exception{
        when(rs.getString("direction")).thenReturn("ABOVE");
        when(rs.getString("created_at")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> alertDao.mapRowToAlert(rs));
    }

    @Test
    @DisplayName("mapRowToAlert rzuca DatabaseException gdy target_price jest NULL")
    void mapRowToAlert_NullTargetPrice_ThrowDatabaseException() throws Exception{
        when(rs.getString("direction")).thenReturn("ABOVE");
        when(rs.getString("created_at")).thenReturn("2026-06-23T12:00:00");
        when(rs.getBigDecimal("target_price")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> alertDao.mapRowToAlert(rs));
    }

    @Test
    @DisplayName("mapRowToAlert rzuca DatabaseException gdy direction jest NULL")
    void mapRowToAlert_NullDirection_ThrowDatabaseException() throws Exception{
        when(rs.getString("direction")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> alertDao.mapRowToAlert(rs));
    }

    @Test
    @DisplayName("mapRowToAlert rzuca DateTimeParseException gdy created_at jest w błędnym formacie")
    void mapRowToAlert_InvalidCreatedAtFormat_ThrowDateTimeParseException() throws Exception{
        when(rs.getString("direction")).thenReturn("ABOVE");
        when(rs.getString("created_at")).thenReturn("nie-prawidłowy-format");
        assertThrows(DatabaseException.class, () -> alertDao.mapRowToAlert(rs));
    }
}
