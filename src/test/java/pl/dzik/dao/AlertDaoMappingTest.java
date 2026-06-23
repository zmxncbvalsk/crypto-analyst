package pl.dzik.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dzik.exception.DatabaseException;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.ResultSet;

@ExtendWith(MockitoExtension.class)
public class AlertDaoMappingTest {
    @Mock
    private ResultSet rs;
    private final AlertDao alertDao = new AlertDao();

    @Test
    @DisplayName("mapRowToAlert rzuca DatabaseException gdy created_at jest NULL")
    void mapRowToAlert_NullCreatedAt_ThrowDatabaseException() throws Exception{
        when(rs.getString("target_price")).thenReturn("50000");
        when(rs.getString("direction")).thenReturn("ABOVE");
        when(rs.getString("created_at")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> alertDao.mapRowToAlert(rs));
    }

    @Test
    @DisplayName("mapRowToAlert rzuca DatabaseException gdy target_price jest NULL")
    void mapRowToAlert_NullTargetPrice_ThrowDatabaseException() throws Exception{
        when(rs.getString("target_price")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> alertDao.mapRowToAlert(rs));
    }

    @Test
    @DisplayName("mapRowToAlert rzuca DatabaseException gdy direction jest NULL")
    void mapRowToAlert_NullDirection_ThrowDatabaseException() throws Exception{
        when(rs.getString("target_price")).thenReturn("50000");
        when(rs.getString("direction")).thenReturn(null);
        assertThrows(DatabaseException.class, () -> alertDao.mapRowToAlert(rs));
    }
}
