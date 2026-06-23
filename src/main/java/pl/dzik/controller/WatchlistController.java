package pl.dzik.controller;

import javafx.scene.web.WebEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pl.dzik.App;
import pl.dzik.dao.CryptoDao;
import pl.dzik.dao.WatchlistDao;
import pl.dzik.enums.SystemStatus;
import pl.dzik.exception.ApiException;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Crypto;
import pl.dzik.model.Watchlist;
import pl.dzik.service.MarketService;

import java.util.List;

/**
 * Kontroler odpowiedzialny za zarządzanie listą obserwowanych kryptowalut (watchlist).
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class WatchlistController {
    private static final Logger logger = LogManager.getLogger(WatchlistController.class);
    private final WebEngine webEngine;
    private final WatchlistDao watchlistDao;
    private final CryptoDao cryptoDao;
    private final MarketService marketService;
    private final SystemStatusController systemStatusController;
    private final App app;

    /**
     * Tworzy kontroler watchlisty powiązany z silnikiem WebView i serwisami danych.
     *
     * @param webEngine              silnik renderujący HTML
     * @param watchlistDao           DAO listy obserwowanych
     * @param cryptoDao              DAO kryptowalut
     * @param marketService          serwis rynkowy
     * @param systemStatusController kontroler statusu systemu
     * @param app                    główna aplikacja JavaFX
     */
    public WatchlistController(
            WebEngine webEngine,
            WatchlistDao watchlistDao,
            CryptoDao cryptoDao,
            MarketService marketService,
            SystemStatusController systemStatusController,
            App app
    ) {
        this.webEngine = webEngine;
        this.watchlistDao = watchlistDao;
        this.cryptoDao = cryptoDao;
        this.marketService = marketService;
        this.systemStatusController = systemStatusController;
        this.app = app;
    }

    /**
     * Renderuje listę wszystkich kryptowalut z watchlisty wraz z przyciskami akcji.
     *
     * @throws DatabaseException gdy odczyt watchlisty lub kryptowalut nie powiedzie się
     */
    public void renderWatchlist() throws DatabaseException {
        Document document = requireDocument("Nie udało się załadować zawartości strony.");
        requireElement(document, "watchlist-container", "Nie znaleziono listy obserwowanych.");
        List<Watchlist> watchlists = watchlistDao.findAll();
        StringBuilder html = new StringBuilder();
        for (Watchlist item : watchlists) {
            Crypto crypto = cryptoDao.findById(item.cryptoId());
            if (crypto == null) {
                logger.warn("Pominięto wpis watchlisty — brak kryptowaluty crypto_id: {}.", item.cryptoId());
                continue;
            }
            html.append(buildWatchlistItemHtml(item, crypto));
        }
        setInnerHtml("watchlist-container", html.toString());
    }

    /**
     * Obsługuje wyszukiwanie kryptowalut na podstawie frazy wpisanej w polu wyszukiwania.
     */
    public void handleSearch() {
        Document document = webEngine.getDocument();
        if (document == null) {
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, "Nie udało się załadować zawartości strony.");
            return;
        }
        if (document.getElementById("search-results-list") == null) {
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, "Nie znaleziono listy podpowiedzi.");
            return;
        }
        Object phraseObj = webEngine.executeScript("document.getElementById('search-input').value");
        String phrase = (phraseObj == null) ? "" : phraseObj.toString().trim();
        if (phrase.isEmpty()) {
            webEngine.executeScript("document.getElementById('search-results-list').innerHTML = '';");
            return;
        }
        try {
            List<Crypto> cryptos = cryptoDao.searchByPhrase(phrase);
            if (cryptos.isEmpty()) {
                webEngine.executeScript(
                        "document.getElementById('search-results-list').innerHTML = '';"
                                + "document.getElementById('search-results-list').style.display = 'none';");
                return;
            }
            setInnerHtml("search-results-list", buildSearchResultsHtml(cryptos));
            webEngine.executeScript("document.getElementById('search-results-list').style.display = 'block';");
        } catch (DatabaseException e) {
            logger.error("Błąd podczas wyszukiwania kryptowaluty.", e);
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, "Błąd podczas przetwarzania wyszukiwania.");
        }
    }

    /**
     * Dodaje kryptowalutę do watchlisty na podstawie aktualnej wartości pola wyszukiwania.
     */
    public void handleAddFromSearch() {
        try {
            Object inputObj = webEngine.executeScript("document.getElementById('search-input').value;");
            if (inputObj == null) {
                return;
            }
            String input = inputObj.toString().trim();
            if (input.isEmpty()) {
                return;
            }
            List<Crypto> cryptos = cryptoDao.searchByPhrase(input);
            if (cryptos.isEmpty()) {
                logger.warn("Brak wyników wyszukiwania dla frazy: {}", input);
                return;
            }
            Crypto crypto = cryptos.getFirst();
            watchlistDao.add(crypto.id());
            marketService.refreshMarketData();
            logger.info("Dodano do obserwowanych: {}", crypto.name());
            webEngine.executeScript("document.getElementById('search-input').value = '';");
            app.refreshAllUI();
        } catch (DatabaseException e) {
            logger.error("Błąd bazy danych podczas dodawania waluty z wyszukiwarki.", e);
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, "BŁĄD DODAWANIA DO WATCHLISTY");
        } catch (ApiException e) {
            logger.warn("Nie udało się odświeżyć danych po dodaniu waluty: {}", e.getMessage());
            app.refreshAllUI();
        }
    }

    /**
     * Usuwa kryptowalutę z watchlisty na podstawie jej ID.
     *
     * @param id identyfikator kryptowaluty w watchliście
     */
    public void removeFromWatchlist(int id) {
        try {
            watchlistDao.removeByCryptoId(id);
            app.refreshAllUI();
            logger.info("Usunięto z listy obserwowanych kryptowalutę o ID: {}", id);
        } catch (DatabaseException e) {
            logger.error("Błąd usuwania z listy obserwowanych.", e);
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, "BŁĄD USUWANIA Z WATCHLISTY");
        }
    }

    /**
     * Przełącza status aktywności (włącz/wyłącz) kryptowaluty w watchliście.
     *
     * @param id identyfikator kryptowaluty
     */
    public void toggleWatchlist(int id) {
        try {
            watchlistDao.toggleStatus(id);
            app.refreshAllUI();
            logger.info("Zmieniono status aktywności dla krypto ID: {}", id);
        } catch (DatabaseException e) {
            logger.error("Błąd podczas przełączania statusu obserwacji.", e);
            systemStatusController.changeSystemStatus(SystemStatus.ERROR, "BŁĄD PRZEŁĄCZANIA WATCHLISTY");
        }
    }

    /**
     * Buduje kod HTML dla poszczególnego elementu watchlist
     * @param item obiekt watchlist
     * @param crypto obiekt kryptowaluty
     * @return kod HTML
     */
    private String buildWatchlistItemHtml(Watchlist item, Crypto crypto) {
        return """
                    <div class="watch-item">
                        <div class="coin-icon">
                            <img src="%s" alt="%s" width="24" height="24" style="border-radius:50%%"/>
                        </div>
                        <div class="watch-meta">
                            <strong>%s</strong>
                        </div>
                        <div class="watch-actions">
                            <div class="toggle %s"
                                 onclick="watchlistController.toggleWatchlist(%d)"></div>
                            <button class="icon-btn"
                                    onclick="watchlistController.removeFromWatchlist(%d)">🗑</button>
                        </div>
                
                    </div>
                """.formatted(
                crypto.image(), crypto.symbol(), crypto.symbol().toUpperCase(),
                item.isActive() ? "on" : "", item.cryptoId(), item.cryptoId());
    }

    /**
     * Buduje kod HTML dla listy autocomplete
     * @param cryptos lista kryptowalut (TOP 250)
     * @return kod HTML
     */
    private String buildSearchResultsHtml(List<Crypto> cryptos) {
        StringBuilder html = new StringBuilder();
        int limit = Math.min(cryptos.size(), 7);
        for (int i = 0; i < limit; i++) {
            Crypto crypto = cryptos.get(i);
            html.append("""
                    <li onclick="document.getElementById('search-input').value = '%s'; document.getElementById('search-results-list').style.display = 'none';"
                        style="padding:8px; cursor:pointer; border-bottom:1px solid rgba(255,255,255,0.05); font-size:13px; color:var(--text);">
                        <span>%s (<b>%s</b>)</span>
                    </li>
                    """.formatted(crypto.name().replace("'", "\\'"), crypto.name(), crypto.symbol().toUpperCase()));
        }
        return html.toString();
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
