package pl.dzik.controller;

import javafx.scene.web.WebEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pl.dzik.dao.CryptoDao;
import pl.dzik.dao.MarketDataDao;
import pl.dzik.dao.WatchlistDao;
import pl.dzik.enums.SystemStatus;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Crypto;
import pl.dzik.model.MarketData;
import pl.dzik.model.Watchlist;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MarketController {
    private static final Logger logger = LogManager.getLogger(MarketController.class);
    private final WebEngine webEngine;
    private final WatchlistDao watchlistDao;
    private final CryptoDao cryptoDao;
    private final MarketDataDao marketDataDao;
    private final SystemStatusController systemStatusController;

    public MarketController(
            WebEngine webEngine,
            WatchlistDao watchlistDao,
            CryptoDao cryptoDao,
            MarketDataDao marketDataDao,
            SystemStatusController systemStatusController
    ){
        this.webEngine = webEngine;
        this.watchlistDao = watchlistDao;
        this.cryptoDao = cryptoDao;
        this.marketDataDao = marketDataDao;
        this.systemStatusController = systemStatusController;
    }

    public void renderMarketTable() throws DatabaseException{
        Document document = requireDocument("Nie udało się załadować zawartości strony.");
        requireElement(document, "market-table-body", "Nie znaleziono tabeli rynkowej.");
        List<Watchlist> watchlists = findActiveWatchlistItems();
        if (watchlists.isEmpty()) {
            setInnerHtml("market-table-body",
                    "<tr><td colspan=\"7\" style=\"text-align:center;color:var(--muted);padding:20px;\">"
                            + "Brak aktywnych walut. Włącz obserwację w panelu bocznym.</td></tr>");
            return;
        }
        StringBuilder html = new StringBuilder();
        for(Watchlist row : watchlists){
            String rowHtml = buildMarketRowHtml(row);
            if(!rowHtml.isEmpty()){
                html.append(rowHtml);
            }
        }
        setInnerHtml("market-table-body", html.toString());
    }

    /**
     * Zwraca kod HTML dla poszczególnego wiersza obserwowanej kryptowaluty
     * @param item obserwowana kryptowaluta
     * @return kod HTML
     * @throws DatabaseException gdy błąd zapytania SQL
     */
    private String buildMarketRowHtml(Watchlist item) throws DatabaseException {
        Crypto crypto = cryptoDao.findById(item.cryptoId());
        if (crypto == null) {
            logger.warn("Pominięto wiersz tabeli — brak kryptowaluty crypto_id: {}.", item.cryptoId());
            return "";
        }
        MarketData marketData = marketDataDao.findLatestByCryptoId(crypto.id());
        if (marketData == null) {
            logger.debug("Brak danych rynkowych dla {} — pomijam wiersz tabeli.", crypto.name());
            return "";
        }
        boolean isPositive = marketData.change24h().compareTo(BigDecimal.ZERO) >= 0;
        String changeClass = isPositive ? "positive" : "negative";
        return """
                <tr>
                    <td><img src="%s" width="24" height="24" style="border-radius:50%%"/></td>
                    <td>
                        <div class="coin-cell">
                            <div class="coin-name">
                                <strong>%s</strong>
                                <span>%s</span>
                            </div>
                        </div>
                    </td>
                    <td class="market">%s</td>
                    <td class="%s">%s</td>
                    <td class="market">%s</td>
                    <td class="market">%s</td>
                </tr>
                """.formatted(
                crypto.image(), crypto.name(), crypto.symbol().toUpperCase(),
                marketData.price(), changeClass, marketData.change24h(),
                marketData.volume(), marketData.marketCap());
    }

    /**
     * Zwraca listę aktywnych kryptowalut
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
     * @param document dokument załadowanej strony
     * @param elementId id elementu nadany w HTML
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
     * @param elementId id elementu do zmiany
     * @param html kod HTML
     */
    private void setInnerHtml(String elementId, String html) {
        webEngine.executeScript(
                "document.getElementById('" + elementId + "').innerHTML = `" + html.replace("`", "\\`") + "`;");
    }
}
