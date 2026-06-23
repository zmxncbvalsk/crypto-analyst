package pl.dzik;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.dzik.client.CoinGeckoClient;
import pl.dzik.config.AppConfig;
import pl.dzik.controller.AlertController;
import pl.dzik.controller.MarketController;
import pl.dzik.controller.SystemStatusController;
import pl.dzik.controller.WatchlistController;
import pl.dzik.dao.AlertDao;
import pl.dzik.dao.CryptoDao;
import pl.dzik.dao.MarketDataDao;
import pl.dzik.dao.WatchlistDao;
import pl.dzik.database.DatabaseInitializer;
import pl.dzik.enums.SystemStatus;
import pl.dzik.exception.ApiException;
import pl.dzik.exception.ApplicationException;
import pl.dzik.exception.DatabaseException;
import pl.dzik.service.AlertService;
import pl.dzik.service.MarketService;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Główna klasa aplikacji JavaFX – punkt wejścia programu.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class App extends Application {
    private static final Logger logger = LogManager.getLogger(App.class);
    private final AlertDao alertDao = new AlertDao();
    private final WatchlistDao watchlistDao = new WatchlistDao();
    private final CryptoDao cryptoDao = new CryptoDao();
    private final MarketDataDao marketDataDao = new MarketDataDao();
    private final AlertService alertService = new AlertService(alertDao);
    private final MarketService marketService = new MarketService(new CoinGeckoClient(), cryptoDao, watchlistDao, marketDataDao, alertService);

    private SystemStatusController systemStatusController;
    private WatchlistController watchlistController;
    private MarketController marketController;
    private AlertController alertController;

    private ScheduledExecutorService service;
    private ScheduledFuture<?> currentScheduledTask;
    private int currentIntervalMinutes = 1;

    @Override
    public void start(Stage primaryStage) {
        logger.info("Uruchamianie okna głównego aplikacji...");
        try {
            AppConfig.load();
            DatabaseInitializer.init();
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            this.systemStatusController = new SystemStatusController(webEngine, this);
            this.watchlistController = new WatchlistController(webEngine, watchlistDao, cryptoDao, marketService, systemStatusController, this);
            this.marketController = new MarketController(webEngine, watchlistDao, cryptoDao, marketDataDao, systemStatusController);
            this.alertController = new AlertController(webEngine, watchlistDao, cryptoDao, alertDao, alertService, systemStatusController, this);
            registerControllers(webEngine);
            loadUserInterface(webEngine);
            currentIntervalMinutes = Integer.parseInt(AppConfig.get("app.interval", "1"));
            service = Executors.newSingleThreadScheduledExecutor();
            updateScheduleInterval(currentIntervalMinutes);
            startBackgroundCryptoSync();
            Scene scene = new Scene(webView, 1440, 900);
            primaryStage.setTitle("Crypto Analyst");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> shutdownBackgroundTasks());
            primaryStage.show();
        } catch (ApplicationException | DatabaseException e) {
            logger.error("Błąd krytyczny podczas startu aplikacji: {}", e.getMessage(), e);
        } catch (NumberFormatException e) {
            logger.error("Nieprawidłowa wartość app.interval w konfiguracji.", e);
        }
    }

    /**
     * Wymusza odświeżenie wszystkich komponentów interfejsu użytkownika.
     */
    public void refreshAllUI() {
        Platform.runLater(() -> {
            try {
                watchlistController.renderWatchlist();
                marketController.renderMarketTable();
                alertController.renderAlertsList();
                alertController.renderAlertSelect();
                logger.info("Odświeżono wszystkie komponenty interfejsu.");
            } catch (DatabaseException e) {
                logger.error("Błąd bazy danych podczas pełnego odświeżania UI.", e);
                systemStatusController.changeSystemStatus(SystemStatus.ERROR, "BŁĄD ODŚWIEŻANIA UI");
            }
        });
    }

    /**
     * Aktualizuje interwał odświżania danych rynkowych
     * @param minutes nowy interwał odświeżania w minutach
     */
    public synchronized void updateScheduleInterval(int minutes){
        this.currentIntervalMinutes = minutes;
        if(currentScheduledTask != null){
            currentScheduledTask.cancel(false);
        }
        currentScheduledTask = service.scheduleAtFixedRate(() -> {
            try{
                marketService.refreshMarketData();
                refreshAllUI();
            } catch (ApiException e) {
                logger.error("Błąd API podczas cyklu odświeżania: {}", e.getMessage());
                Platform.runLater(() -> systemStatusController.changeSystemStatus(
                        SystemStatus.DEGRADED, "BŁĄD API — LIMIT LUB SIEC"));
            } catch (DatabaseException e) {
                logger.error("Błąd bazy podczas cyklu odświeżania.", e);
                Platform.runLater(() -> systemStatusController.changeSystemStatus(
                        SystemStatus.ERROR, "BŁĄD SYNCHRONIZACJI"));
            }
        }, 0, minutes, TimeUnit.MINUTES);
    }

    /**
     * Zwraca obecny interwał
     * @return obecny interwał
     */
    public int getCurrentIntervalMinutes(){
        return currentIntervalMinutes;
    }

    /**
     * Rejestruje kontrolery w silniku strony
     * @param webEngine silnik
     */
    private void registerControllers(WebEngine webEngine){
        webEngine.getLoadWorker().stateProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue == Worker.State.SUCCEEDED){
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("systemStatusController", systemStatusController);
                window.setMember("watchlistController", watchlistController);
                window.setMember("marketController", marketController);
                window.setMember("alertController", alertController);
                logger.info("Pomyślnie zarejestrowano kontrolery w silniku JavaScript");
                refreshAllUI();
                systemStatusController.highlightIntervalButton(getCurrentIntervalMinutes());
                systemStatusController.changeSystemStatus(SystemStatus.RUNNING, "SYSTEM AKTYWNY");
            }
        }));
    }

    /**
     * Ładuje plik źródłowy interfejsu graficznego
     * @param webEngine silnik
     * @throws ApplicationException gdy brak pliku źródłowego interfejsu graficznego
     */
    private void loadUserInterface(WebEngine webEngine) throws ApplicationException{
        URL url = getClass().getResource("index.html");
        if(url == null){
            throw new ApplicationException("Krytyczny błąd aplikacji. Błąd źródłowy interfejsu graficznego.");
        }
        webEngine.load(url.toExternalForm());
    }

    /**
     * Ustawienie i uruchomienie synchronizacji słownika w tle
     */
    private void startBackgroundCryptoSync(){
        Thread syncThread = new Thread(marketService::synchronizeCryptoDictionary);
        syncThread.setDaemon(true);
        syncThread.setName("crypto-dictionary-sync");
        syncThread.start();
    }

    /**
     * Procedura zwalniania wątków w tle
     */
    private void shutdownBackgroundTasks(){
        logger.info("Zamykanie wątków w tle...");
        if(service != null){
            service.shutdown();
        }
    }

    /**
     * Punkt wejścia aplikacji konsolowej uruchamiającej JavaFX.
     *
     * @param args argumenty wiersza poleceń
     */
    public static void main(String[] args) {
        launch(args);
    }
}
