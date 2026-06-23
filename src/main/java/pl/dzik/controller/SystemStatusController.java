package pl.dzik.controller;

import javafx.scene.web.WebEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pl.dzik.App;
import pl.dzik.config.AppConfig;
import pl.dzik.enums.SystemStatus;
import pl.dzik.exception.ApplicationException;

/**
 * Kontroler zarządzający globalnym statusem aplikacji oraz interwałem odświeżania.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class SystemStatusController {
    private static final Logger logger = LogManager.getLogger(SystemStatusController.class);
    private final WebEngine webEngine;
    private final App app;

    /**
     * Tworzy kontroler statusu systemu.
     *
     * @param webEngine silnik renderujący HTML
     * @param app       główna aplikacja JavaFX
     */
    public SystemStatusController(WebEngine webEngine, App app) {
        this.webEngine = webEngine;
        this.app = app;
    }

    /**
     * Zmienia aktualny status aplikacji i aktualizuje widżet w interfejsie.
     * @param systemStatus nowy status aplikacji
     * @param message komunikat do wyświetlenia
     */
    public void changeSystemStatus(SystemStatus systemStatus, String message){
        logger.info("Żądanie zmiany statusu aplikacji na: {}", systemStatus.name());
        Document document = webEngine.getDocument();
        if(document == null){
            logger.error("Błąd podczas zmiany statusu aplikacji — dokument DOM jest niedostępny.");
            return;
        }
        Element statusLabel = document.getElementById("system-status-chip");
        if(statusLabel == null){
            logger.warn("Nie znaleziono elementu system-status-chip w DOM.");
            return;
        }
        String color = switch (systemStatus){
            case STARTING -> "#3498db";
            case RUNNING -> "#2ecc71";
            case DEGRADED -> "#f39c12";
            case ERROR -> "#ff0000";
        };
        String oldStyle = statusLabel.getAttribute("style");
        statusLabel.setAttribute("style", oldStyle + ";--green:" + color);
        statusLabel.setTextContent(message);
        logger.debug("Pomyślnie zmieniono status aplikacji na: {} ({})", systemStatus.name(), message);
    }

    /**
     * Podświetla obecny interwał w UI (synchronizacja UI z wartością z pliku konfiguracyjnego)
     *
     * @param minutes interwał do podświetlenia w minutach
     */
    public void highlightIntervalButton(int minutes) {
        logger.debug("Synchronizacja przycisku interwału odświeżania na: {} min.", minutes);
        Document document = webEngine.getDocument();
        if (document == null) {
            logger.warn("Pominięto synchronizację przycisku interwału — dokument DOM jest niedostępny.");
            return;
        }
        applyIntervalButtonStyles(document, app.getCurrentInterval(), minutes);
    }

    public void changeInterval(int minutes){
        logger.info("Żądanie zmiany interwału odświeżania na: {} min.", minutes);
        Document document = webEngine.getDocument();
        if(document == null){
            logger.error("Błąd podczas zmiany interwału odświeżania — dokument DOM jest niedostępny.");
            return;
        }
        try{
            AppConfig.set("app.interval", String.valueOf(minutes));
            applyIntervalButtonStyles(document, app.getCurrentInterval(), minutes);
            app.updateScheduleInterval(minutes);
        } catch (ApplicationException e){
            logger.error("Nie udało się zapisać interwału odświeżania w konfiguracji.", e);
        }
    }

    private void applyIntervalButtonStyles(Document document, int previousInterval, int newInterval){
        Element previousButton = document.getElementById("btn-interval-" + previousInterval);
        Element intervalButton = document.getElementById("btn-interval-" + newInterval);
        if (previousButton != null) {
            previousButton.removeAttribute("class");
        }
        if (intervalButton != null) {
            intervalButton.setAttribute("class", "active");
        }
    }
}
