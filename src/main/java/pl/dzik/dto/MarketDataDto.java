package pl.dzik.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/**
 * Klasa DTO przechowująca szczegółowe, bieżące dane rynkowe kryptowalut
 *
 * @param id           unikalny identyfikator
 * @param symbol       symbol kryptowaluty
 * @param name         nazwa kryptowaluty
 * @param image        ikona kryptowaluty
 * @param currentPrice obecna cena
 * @param totalVolume  wolumen
 * @param marketCap    kapitalizacja rynkowa
 * @param change24h    zmiana 24-godzinna
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public record MarketDataDto(
        String id,
        String symbol,
        String name,
        String image,
        @SerializedName("current_price")
        BigDecimal currentPrice,
        @SerializedName("total_volume")
        BigDecimal totalVolume,
        @SerializedName("market_cap")
        BigDecimal marketCap,
        @SerializedName("price_change_percentage_24h")
        BigDecimal change24h
) {
}
