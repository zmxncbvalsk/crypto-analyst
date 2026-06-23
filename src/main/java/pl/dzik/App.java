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
public class App extends Application
{
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
    public void start(Stage primaryStage){
        logger.info("Uruchamianie okna głównego aplikacji...");
        try{
            AppConfig.load();
            DatabaseInitializer.init();
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            this.systemStatusController = new SystemStatusController(webEngine, this);
            this.marketController = new MarketController(webEngine, watchlistDao, cryptoDao, marketDataDao, systemStatusController);

        }
    }

    public static void main( String[] args )
    {
        launch(args);
    }
}
