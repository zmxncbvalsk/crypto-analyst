package pl.dzik.controller;

import javafx.scene.web.WebEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pl.dzik.App;
import pl.dzik.dao.AlertDao;
import pl.dzik.dao.CryptoDao;
import pl.dzik.dao.WatchlistDao;
import pl.dzik.enums.Direction;
import pl.dzik.enums.SystemStatus;
import pl.dzik.exception.AlertValidationException;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Alert;
import pl.dzik.model.Crypto;
import pl.dzik.model.Watchlist;
import pl.dzik.service.AlertService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Kontroler odpowiedzialny za zarządzanie alertami cenowymi w warstwie GUI.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class AlertController {
    private static final Logger logger = LogManager.getLogger(AlertController.class);
    private final WebEngine webEngine;
    private final WatchlistDao watchlistDao;
    private final CryptoDao cryptoDao;
    private final AlertDao alertDao;
    private final AlertService alertService;
    private final SystemStatusController systemStatusController;
    private final App app;

    /**
     * Tworzy kontroler alertów powiązany z silnikiem WebView i zależnościami danych.
     *
     * @param webEngine              silnik renderujący HTML
     * @param watchlistDao           DAO listy obserwowanych
     * @param cryptoDao              DAO kryptowalut
     * @param alertDao               DAO alertów
     * @param alertService           serwis logiki alertów
     * @param systemStatusController kontroler statusu systemu
     * @param app                    główna aplikacja JavaFX
     */
    public AlertController(
            WebEngine webEngine,
            WatchlistDao watchlistDao,
            CryptoDao cryptoDao,
            AlertDao alertDao,
            AlertService alertService,
            SystemStatusController systemStatusController,
            App app
    ) {
        this.webEngine = webEngine;
        this.watchlistDao = watchlistDao;
        this.cryptoDao = cryptoDao;
        this.alertDao = alertDao;
        this.alertService = alertService;
        this.systemStatusController = systemStatusController;
        this.app = app;
    }

    /**
     * Renderuje listę kryptowalut w polu wyboru (select) w formularzu tworzenia alertu.
     *
     * @throws DatabaseException gdy odczyt watchlisty lub kryptowalut nie powiedzie się
     */
    public void renderAlertSelect() throws DatabaseException {
        Document document = requireDocument("Nie udało się załadować zawartości strony.");
        Element select = requireElement(document, "alert-crypto-select", "Nie znaleziono listy wyboru kryptowalut.");
        Object currentSelectionObj = webEngine.executeScript("document.getElementById('alert-crypto-select').value");
        String currentSelection = currentSelectionObj == null ? "" : currentSelectionObj.toString().trim();
        select.setTextContent("");
        for (Watchlist item : findActiveWatchlistItems()) {
            Crypto crypto = cryptoDao.findById(item.cryptoId());
            if (crypto == null) {
                logger.warn("Pominięto brakującą kryptowalutę crypto_id: {} przy renderowaniu selecta alertów.", item.cryptoId());
                continue;
            }
            appendCryptoOption(document, select, crypto, currentSelection);
        }
    }

    /**
     * Renderuje listę wszystkich aktywnych alertów cenowych na stronie.
     *
     * @throws DatabaseException gdy odczyt alertów lub kryptowalut nie powiedzie się
     */
    public void renderAlertsList() throws DatabaseException {
        Document document = requireDocument("Nie udało się załadować zawartości strony.");
        requireElement(document, "alerts-container", "Nie znaleziono listy alertów.");
        List<Alert> alerts = alertDao.findActiveAlerts();
        if (alerts.isEmpty()) {
            setInnerHtml("alerts-container",
                    "<p style=\"color:var(--muted);font-size:13px;text-align:center;padding:10px;\">Brak skonfigurowanych alertów.</p>");
            return;
        }
        StringBuilder html = new StringBuilder();
        for (Alert alert : alerts) {
            html.append(buildAlertItemHtml(alert));
        }
        setInnerHtml("alerts-container", html.toString());
    }

    /**
     * Obsługuje utworzenie nowego alertu cenowego na podstawie danych z formularza.
     */
    public void handleCreateAlert() {
        try {
            Object cryptoObj = webEngine.executeScript("document.getElementById('alert-crypto-select').value;");
            Object directionObj = webEngine.executeScript("document.getElementById('alert-direction-select').value;");
            Object priceObj = webEngine.executeScript("document.getElementById('alert-price-input').value;");
            if (cryptoObj == null || priceObj == null || priceObj.toString().isEmpty()) {
                logger.warn("Formularz alertu zawiera puste pola.");
                return;
            }
            int cryptoId = Integer.parseInt(cryptoObj.toString());
            Direction direction = Direction.valueOf(directionObj.toString());
            String priceStr = priceObj.toString().trim();
            if (priceStr.isEmpty()) {
                logger.warn("Pusta cena alertu");
                return;
            }
            try {
                BigDecimal targetPrice = new BigDecimal(priceStr);
                Alert newAlert = new Alert(0, cryptoId, targetPrice, direction, false, LocalDateTime.now(), null);
                alertService.createAlert(newAlert);
                webEngine.executeScript("document.getElementById('alert-price-input').value = '';");
                app.refreshAllUI();
                logger.info("Zapisano nowy alert cenowy dla krypto ID: {}", cryptoId);
            } catch (NumberFormatException e) {
                logger.warn("Zła cena: {}", priceStr);
                return;
            }
        } catch (AlertValidationException e) {
            logger.warn("Walidacja alertu nie powiodła się: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Błąd walidacji danych alertu: {}", e.getMessage());
        } catch (DatabaseException e) {
            logger.error("Błąd bazy danych podczas tworzenia alertu.", e);
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, "BŁĄD ZAPISU ALERTU");
        }
    }

    /**
     * Usuwa alert o podanym identyfikatorze.
     *
     * @param id identyfikator alertu do usunięcia
     */
    public void deleteAlert(int id) {
        try {
            alertService.deleteAlert(id);
            app.refreshAllUI();
        } catch (DatabaseException e) {
            logger.error("Błąd podczas usuwania alertu o ID: {}.", id, e);
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, "BŁĄD USUWANIA ALERTU");
        }
    }

    /**
     * Dodaje nową pozycję do elementu listy w ustawieniach alertu
     *
     * @param document         dokument bierzącej strony
     * @param select           element HTML
     * @param crypto           obiekt kryptowaluty
     * @param currentSelection identyfikator aktualnie wybranej kryptowaluty
     */
    private void appendCryptoOption(Document document, Element select, Crypto crypto, String currentSelection) {
        Element option = document.createElement("option");
        String id = String.valueOf(crypto.id());
        option.setAttribute("value", id);
        option.setTextContent(crypto.name());
        if (id.equals(currentSelection)) {
            option.setAttribute("selected", "selected");
        }
        select.appendChild(option);
    }

    /**
     * Tworzy element listy alertu HTML
     * @param alert obiekt alertu
     * @return kod HTML
     * @throws DatabaseException gdy błąd SQL
     */
    private String buildAlertItemHtml(Alert alert) throws DatabaseException {
        Crypto crypto = cryptoDao.findById(alert.cryptoId());
        if (crypto == null) {
            logger.warn("Pominięto alert ID: {} — brak kryptowaluty crypto_id: {}.", alert.id(), alert.cryptoId());
            return "";
        }
        String pillClass = alert.direction() == Direction.ABOVE ? "up" : "down";
        String pillLabel = alert.direction() == Direction.ABOVE ? "Wzrośnie do" : "Spadnie do";
        return """
                <div class="alert-item"> <div class="alert-info">
                        <strong>%s</strong>
                        <span>Cena docelowa</span>
                    </div>
                    <div class="alert-condition">
                        <span class="pill %s">%s</span>
                    </div>
                    <div class="alert-target">
                        <strong>%s</strong>
                    </div>
                    <div class="alert-actions">
                        <button class="icon-btn alert-delete-btn" onclick="alertController.deleteAlert(%d)">🗑</button>
                    </div>
                </div>
                """.formatted(crypto.name(), pillClass, pillLabel, alert.targetPrice(), alert.id());
    }

    /**
     * Zwraca listę aktywnych kryptowalut
     *
     * @return lista aktywnych kryptowalut
     * @throws DatabaseException gdy błąd bazy danych
     */
    private List<Watchlist> findActiveWatchlistItems() throws DatabaseException {
        List<Watchlist> watchlists = watchlistDao.findAll();
        List<Watchlist> activeWatchlists = new ArrayList<>();
        for (Watchlist w : watchlists) {
            if (w.isActive()) {
                activeWatchlists.add(w);
            }
        }
        return activeWatchlists;
    }

    /**
     * Zwraca dokument (Strukturę drzewa DOM)
     *
     * @param errorMessage wiadomość, jeśli się nie powiedzie
     * @return dokument aktualnie załadowanej strony
     */
    private Document requireDocument(String errorMessage) {
        Document document = webEngine.getDocument();
        if (document == null) {
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        return document;
    }

    /**
     * Zwraca żądany element
     *
     * @param document     dokument załadowanej strony
     * @param elementId    id elementu nadany w HTML
     * @param errorMessage wiadomość, jeśli się nie powiedzie
     * @return Element
     */
    private Element requireElement(Document document, String elementId, String errorMessage) {
        Element element = document.getElementById(elementId);
        if (element == null) {
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        return element;
    }

    /**
     * Bezpiecznie i dynamicznie zmienia zawartość kodu HTML
     *
     * @param elementId id elementu do zmiany
     * @param html      kod HTML
     */
    private void setInnerHtml(String elementId, String html) {
        webEngine.executeScript(
                "document.getElementById('" + elementId + "').innerHTML = `" + html.replace("`", "\\`") + "`;");
    }
}
