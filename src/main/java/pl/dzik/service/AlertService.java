package pl.dzik.service;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.dzik.dao.AlertDao;
import pl.dzik.enums.Direction;
import pl.dzik.exception.AlertValidationException;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Alert;
import pl.dzik.model.Crypto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Serwis biznesowy odpowiedzialny za walidację, tworzenie i wyzwalanie alertów cenowych.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class AlertService {
    private static final Logger logger = LogManager.getLogger(AlertService.class);
    private final AlertDao alertDao;

    /**
     * Tworzy serwis z domyślnym DAO alertów
     */
    public AlertService() {
        this.alertDao = new AlertDao();
    }

    /**
     * Konstruktor tworzący zależności i konfigurujący serwis alertów,
     * wraz ze wstrzykniętymi zależnościami, pozwalającymi na mock w testach
     *
     * @param alertDao warstwa dostępu do alertów w bazie danych
     */
    public AlertService(AlertDao alertDao) {
        this.alertDao = alertDao;
    }

    /**
     * Usuwa alert o podanym identyfikatorze
     *
     * @param alertId identyfikator alertu do usunięcia
     * @throws DatabaseException gdy operacja usunięcia nie powiedzie się
     */
    public void deleteAlert(int alertId) throws DatabaseException {
        alertDao.remove(alertId);
        logger.info("Usunięto alert o ID: {}", alertId);
    }

    /**
     * Waliduje dane alertu przez zapisem do bazy.
     *
     * @param alert obiekt alertu do sprawdzenia
     * @throws AlertValidationException gdy identyfikator kryptowaluty, cena lub kierunek są nieprawidłowe
     */
    public void validateAlert(Alert alert) throws AlertValidationException {
        if (alert == null) {
            throw new AlertValidationException("Alert nie może być pusty.");
        }
        if (alert.cryptoId() <= 0) {
            throw new AlertValidationException("Nieprawidłowy identyfikator kryptowaluty.");
        }
        if (alert.targetPrice() == null || alert.targetPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AlertValidationException("Cena docelowa musi być większa od zera.");
        }
        if (alert.direction() == null) {
            throw new AlertValidationException("Kierunek alertu jest wymagany.");
        }
    }

    /**
     * Sprawdza, czy aktualna cena spełnia warunek alertu
     *
     * @param alert        alert do oceny
     * @param currentPrice bieżąca cena rynkowa
     * @return {@code true}, gdy warunek alertu został spełniony
     */
    public boolean isTriggered(Alert alert, BigDecimal currentPrice) {
        if (alert.direction() == Direction.ABOVE) {
            return currentPrice.compareTo(alert.targetPrice()) >= 0;
        }
        return currentPrice.compareTo(alert.targetPrice()) <= 0;
    }

    /**
     * Buduje komunikat powiadomienia dla wyzwolonego alertu.
     *
     * @param crypto       kryptowaluta objęta alertem
     * @param alert        wyzwolony alert
     * @param currentPrice aktualna cena rynkowa
     * @return sformatowany komunikat
     */
    public String buildTriggerMessage(Crypto crypto, Alert alert, BigDecimal currentPrice) {
        String type = alert.direction() == Direction.ABOVE ? "przebiło cenę docelową" : "spadło poniżej ceny docelowej";
        return String.format(
                "Kryptowaluta %s (%S) %s %s!. Obecna cena: %s",
                crypto.name(), crypto.symbol(), type, alert.targetPrice(), currentPrice
        );
    }

    /**
     * Tworzy i zapisuje nowy alert cenowy w bazie danych po walidacji.
     *
     * @param alert gotowy obiekt alertu
     * @throws AlertValidationException gdy dane alertu są nieprawidłowe
     * @throws DatabaseException        gdy wystąpi błąd podczas zapisu do bazy
     */
    public void createAlert(Alert alert) throws AlertValidationException, DatabaseException {
        validateAlert(alert);
        alertDao.add(alert);
        logger.info("Utworzono alert cenowy dla crypto_id: {}", alert.cryptoId());
    }

    /**
     * Sprawdza wszystkie aktywne alerty dla danej kryptowaluty i wyzwala te, których warunek został spełniony.
     *
     * @param crypto       kryptowaluta, której cena została właśnie zaktualizowana
     * @param currentPrice aktualna cena rynkowa
     * @throws DatabaseException gdy wystąpi błąd podczas odczytu lub zapisu alertów
     */
    public void checkAndTriggerAlerts(Crypto crypto, BigDecimal currentPrice) throws DatabaseException {
        List<Alert> activeAlerts = alertDao.findActiveAlerts();
        for (Alert alert : activeAlerts) {
            if (alert.cryptoId() != crypto.id()) {
                continue;
            }
            if (!isTriggered(alert, currentPrice)) {
                continue;
            }
            String message = buildTriggerMessage(crypto, alert, currentPrice);
            logger.info("ALERT! {}", message);
            alertDao.markAsTriggered(alert.id());
            showAlertDialog(message);
        }
    }

    /**
     * Deleguje wykonanie wątku wyświetlenia okna dialogowego alertu cenowego w JavaFX.
     *
     * @param message treść alertu
     */
    private void showAlertDialog(String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert dialog = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            dialog.setTitle("Alert Cenowy!");
            dialog.setHeaderText("Ruch cenowy na rynku kryptowalut");
            dialog.setContentText(message);
            dialog.show();
        });
    }
}
