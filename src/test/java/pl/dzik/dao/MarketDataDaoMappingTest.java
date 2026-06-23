package pl.dzik.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.MarketData;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MarketDataDaoMappingTest {
    @Mock
    private ResultSet resultSet;
    private final MarketDataDao marketDataDao = new MarketDataDao();

    @Test
    @DisplayName("mapRowToMarketData poprawnie mapuje prawidłowy wiersz z bazy danych")
    void mapRowToMarketData_ValidRow_ReturnsMarketData() throws Exception{
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getInt("crypto_id")).thenReturn(10);
        when(resultSet.getBigDecimal("price")).thenReturn(new BigDecimal("50000.00"));
        when(resultSet.getBigDecimal("volume")).thenReturn(new BigDecimal("10000000.00"));
        when(resultSet.getBigDecimal("market_cap")).thenReturn(new BigDecimal("800000.00"));
        when(resultSet.getBigDecimal("change_24h")).thenReturn(new BigDecimal("2.5"));
        when(resultSet.getString("recorded_at")).thenReturn("2026-06-23T12:00:00");
        MarketData result = marketDataDao.mapRowToMarketData(resultSet);
        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals(10, result.cryptoId());
        assertEquals(new BigDecimal("50000.00"), result.price());
        assertEquals(new BigDecimal("10000000.00"), result.volume());
        assertEquals(new BigDecimal("800000.00"), result.marketCap());
        assertEquals(new BigDecimal("2.5"), result.change24h());
        assertEquals(LocalDateTime.of(2026, 6, 23, 12, 0, 0), result.recordedAt());
    }

    @Test
    @DisplayName("mapRowToMarketData rzuca DatabaseException gdy recorded_at jest null")
    void mapToMarketData_NullRecordedAt_ThrowDatabaseException() throws Exception{
        when(resultSet.getBigDecimal("price")).thenReturn(new BigDecimal("100"));
        when(resultSet.getBigDecimal("volume")).thenReturn(new BigDecimal("200"));
        when(resultSet.getBigDecimal("market_cap")).thenReturn(new BigDecimal("300"));
        when(resultSet.getBigDecimal("change_24h")).thenReturn(new BigDecimal("2.5"));
        when(resultSet.getString("recorded_at")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> marketDataDao.mapRowToMarketData(resultSet));
    }

    @Test
    @DisplayName("mapRowToMarketData rzuca DatabaseException gdy change_24h jest null")
    void mapToMarketData_NullChange24h_ThrowDatabaseException() throws Exception{
        when(resultSet.getBigDecimal("price")).thenReturn(new BigDecimal("100"));
        when(resultSet.getBigDecimal("volume")).thenReturn(new BigDecimal("200"));
        when(resultSet.getBigDecimal("market_cap")).thenReturn(new BigDecimal("300"));
        when(resultSet.getBigDecimal("change_24h")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> marketDataDao.mapRowToMarketData(resultSet));
    }

    @Test
    @DisplayName("mapRowToMarketData rzuca DatabaseException gdy market_cap jest null")
    void mapToMarketData_NullMarketCap_ThrowDatabaseException() throws Exception{
        when(resultSet.getBigDecimal("price")).thenReturn(new BigDecimal("100"));
        when(resultSet.getBigDecimal("volume")).thenReturn(new BigDecimal("200"));
        when(resultSet.getBigDecimal("market_cap")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> marketDataDao.mapRowToMarketData(resultSet));
    }

    @Test
    @DisplayName("mapRowToMarketData rzuca DatabaseException gdy volume jest null")
    void mapToMarketData_NullVolume_ThrowDatabaseException() throws Exception{
        when(resultSet.getBigDecimal("price")).thenReturn(new BigDecimal("100"));
        when(resultSet.getBigDecimal("volume")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> marketDataDao.mapRowToMarketData(resultSet));
    }

    @Test
    @DisplayName("mapRowToMarketData rzuca DatabaseException gdy price jest null")
    void mapToMarketData_NullPrice_ThrowDatabaseException() throws Exception{
        when(resultSet.getBigDecimal("price")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> marketDataDao.mapRowToMarketData(resultSet));
    }

    @Test
    @DisplayName("mapToMarketData rzuca DateTimeParseException gdy recorded_at jest w błędnym formacie")
    void mapToMarketData_InvalidCreatedAtFormat_ThrowDateTimeParseException() throws Exception{
        when(resultSet.getBigDecimal("price")).thenReturn(new BigDecimal("100"));
        when(resultSet.getBigDecimal("volume")).thenReturn(new BigDecimal("200"));
        when(resultSet.getBigDecimal("market_cap")).thenReturn(new BigDecimal("300"));
        when(resultSet.getBigDecimal("change_24h")).thenReturn(new BigDecimal("2.5"));
        when(resultSet.getString("recorded_at")).thenReturn("niepoprawny-format-daty");
        assertThrows(DatabaseException.class, () -> marketDataDao.mapRowToMarketData(resultSet));
    }
}
