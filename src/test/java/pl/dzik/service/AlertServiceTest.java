package pl.dzik.service;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dzik.dao.AlertDao;
import pl.dzik.enums.Direction;
import pl.dzik.exception.AlertValidationException;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Alert;
import pl.dzik.model.Crypto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {
    @Mock
    private AlertDao alertDao;

    private AlertService alertService;

    @BeforeAll
    static void initJavaFX(){
        try{
            Platform.startup(() -> {});
        } catch (IllegalStateException e){
            //Uruchomiono JavaFX :)
        }
    }

    @BeforeEach
    void setUp(){
        alertService = new AlertService(alertDao);
    }

    @Test
    @DisplayName("Powinien wyzwolić alert, gdy cena wzrośnie powyżej progu ABOVE")
    void checkAndTriggerAlerts_priceAboveThreshold_triggersAlert() throws DatabaseException {
        Crypto crypto = new Crypto(1, "bitcoin", "btc", "Bitcoin", "btc.png", 1);
        BigDecimal currentPrice = new BigDecimal("65000.00");
        Alert alert = new Alert(1, 1, new BigDecimal("60000.00"), Direction.ABOVE, false, LocalDateTime.now(), null);
        when(alertDao.findActiveAlerts()).thenReturn(List.of(alert));
        alertService.checkAndTriggerAlerts(crypto, currentPrice);
        verify(alertDao).markAsTriggered(1);
    }

    @Test
    @DisplayName("Nie powinien wyzwalać alertu, gdy cena wzrosła, ale nie osiągnęła progu ABOVE")
    void checkAndTriggerAlerts_priceBelowThreshold_doesNothing() throws DatabaseException {
        Crypto crypto = new Crypto(1, "bitcoin", "btc", "Bitcoin", "btc.png", 1);
        BigDecimal currentPrice = new BigDecimal("59000.00");
        Alert alert = new Alert(1, 1, new BigDecimal("60000.00"), Direction.ABOVE, false, LocalDateTime.now(), null);
        when(alertDao.findActiveAlerts()).thenReturn(List.of(alert));
        alertService.checkAndTriggerAlerts(crypto, currentPrice);
        verify(alertDao, never()).markAsTriggered(anyInt());
    }

    @Test
    @DisplayName("Powinien wyzwolić alert, gdy cena spadnie poniżej progu BELOW")
    void checkAndTriggerAlerts_priceBelowThreshold_triggersAlert() throws DatabaseException {
        Crypto crypto = new Crypto(1, "ethereum", "eth", "Ethereum", "eth.png", 1);
        BigDecimal currentPrice = new BigDecimal("2900.00");
        Alert alert = new Alert(1, 1, new BigDecimal("3000.00"), Direction.BELOW, false, LocalDateTime.now(), null);
        when(alertDao.findActiveAlerts()).thenReturn(List.of(alert));
        alertService.checkAndTriggerAlerts(crypto, currentPrice);
        verify(alertDao).markAsTriggered(1);
    }

    @Test
    @DisplayName("Walidacja odrzuca alert z ujemną ceną docelową")
    void validateAlert_NegativePrice_ThrowsException() {
        Alert alert = new Alert(0, 1, new BigDecimal("-1"), Direction.ABOVE, false, LocalDateTime.now(), null);
        assertThrows(AlertValidationException.class, () -> alertService.validateAlert(alert));
    }

    @Test
    @DisplayName("Walidacja odrzuca alert z nieprawidłowym crypto_id")
    void validateAlert_IncorrectCryptoId_ThrowsException() {
        Alert alert = new Alert(0, 0, new BigDecimal("100"), Direction.ABOVE, false, LocalDateTime.now(), null);
        assertThrows(AlertValidationException.class, () -> alertService.validateAlert(alert));
    }

    @Test
    @DisplayName("createAlert zapisuje poprawny alert po walidacji")
    void createAlert_CorrectData_SaveToDatabase() throws Exception {
        Alert alert = new Alert(0, 1, new BigDecimal("50000"), Direction.ABOVE, false, LocalDateTime.now(), null);
        alertService.createAlert(alert);
        verify(alertDao).add(alert);
    }

    @Test
    @DisplayName("isTriggered zwraca true dokładnie na progu ABOVE")
    void isTriggered_Exactly_On_Threshold_Above_ReturnsTrue() {
        Alert alert = new Alert(1, 1, new BigDecimal("60000"), Direction.ABOVE, false, LocalDateTime.now(), null);
        assertTrue(alertService.isTriggered(alert, new BigDecimal("60000")));
    }

    @Test
    @DisplayName("buildTriggerMessage zawiera nazwę kryptowaluty i cenę")
    void buildTriggerMessage_ContainsCryptoData() {
        Crypto crypto = new Crypto(1, "bitcoin", "btc", "Bitcoin", "btc.png", 1);
        Alert alert = new Alert(1, 1, new BigDecimal("60000"), Direction.ABOVE, false, LocalDateTime.now(), null);
        String message = alertService.buildTriggerMessage(crypto, alert, new BigDecimal("65000"));
        assertTrue(message.contains("Bitcoin"));
        assertTrue(message.contains("65000"));
    }
}
