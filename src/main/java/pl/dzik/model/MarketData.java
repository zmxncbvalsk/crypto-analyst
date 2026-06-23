package pl.dzik.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Reprezentuje historyczną próbkę danych rynkowych pobraną z API.
 *
 * @param id         klucz główny
 * @param cryptoId   klucz obcy kryptowaluty
 * @param price      cena
 * @param volume     wolumen
 * @param marketCap  kapitalizacja rynkowa
 * @param change24h  zmiana 24-godzinna ceny
 * @param recordedAt data utworzenia wpisu
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public record MarketData(
        int id,
        int cryptoId,
        BigDecimal price,
        BigDecimal volume,
        BigDecimal marketCap,
        BigDecimal change24h,
        LocalDateTime recordedAt
) {
}
