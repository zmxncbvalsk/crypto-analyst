package pl.dzik;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dzik.controller.AlertController;
import pl.dzik.controller.SystemStatusController;
import pl.dzik.dao.AlertDao;
import pl.dzik.dao.CryptoDao;
import pl.dzik.dao.WatchlistDao;
import pl.dzik.enums.Direction;
import pl.dzik.exception.AlertValidationException;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Alert;
import pl.dzik.service.AlertService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertControllerTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Środowisko JavaFX jest już uruchomione
        }
    }

    @Mock private WebEngine webEngine;
    @Mock private WatchlistDao watchlistDao;
    @Mock private CryptoDao cryptoDao;
    @Mock private AlertDao alertDao;
    @Mock private AlertService alertService;
    @Mock private SystemStatusController systemStatusController;
    @Mock private App app;

    @InjectMocks
    private AlertController alertController;

    @Test
    void handleCreateAlert_ValidData_SavesAlertAndRefreshesUI() throws Exception {
        when(webEngine.executeScript(contains("alert-crypto-select"))).thenReturn("1");
        when(webEngine.executeScript(contains("alert-direction-select"))).thenReturn("ABOVE");
        when(webEngine.executeScript(contains("alert-price-input"))).thenReturn("50000.00");
        alertController.handleCreateAlert();
        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertService).createAlert(captor.capture());
        Alert savedAlert = captor.getValue();
        assertEquals(1, savedAlert.cryptoId());
        assertEquals(Direction.ABOVE, savedAlert.direction());
        assertEquals(new BigDecimal("50000.00"), savedAlert.targetPrice());
        verify(app).refreshAllUI();
    }

    @Test
    void deleteAlert_ExistingId_DeletesAlertAndRefreshesUI() throws DatabaseException {
        alertController.deleteAlert(99);
        verify(alertService).deleteAlert(99);
        verify(app).refreshAllUI();
    }

    @Test
    void handleCreateAlert_InvalidPriceFormat_DoesNotSaveAlert() throws Exception {
        when(webEngine.executeScript(contains("alert-crypto-select"))).thenReturn("1");
        when(webEngine.executeScript(contains("alert-direction-select"))).thenReturn("ABOVE");
        when(webEngine.executeScript(contains("alert-price-input"))).thenReturn("abc");
        alertController.handleCreateAlert();
        verify(alertService, never()).createAlert(any(Alert.class));
        verify(app, never()).refreshAllUI();
    }

    @Test
    void handleCreateAlert_ValidationFailed_DoesNotRefreshUI() throws Exception {
        when(webEngine.executeScript(contains("alert-crypto-select"))).thenReturn("1");
        when(webEngine.executeScript(contains("alert-direction-select"))).thenReturn("ABOVE");
        when(webEngine.executeScript(contains("alert-price-input"))).thenReturn("0");
        doThrow(new AlertValidationException("Cena docelowa musi być większa od zera.")).when(alertService).createAlert(any(Alert.class));
        alertController.handleCreateAlert();
        verify(app, never()).refreshAllUI();
    }
}