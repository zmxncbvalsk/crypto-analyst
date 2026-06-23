package pl.dzik.model;

import pl.dzik.enums.Direction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Reprezentuje alert cenowy skonfigurowany przez użytkownika.
 *
 * @param id          klucz główny
 * @param cryptoId    klucz obcy kryptowaluty
 * @param targetPrice cena docelowa
 * @param direction   kierunek alertu
 * @param isTriggered wartość logiczna mówiąca, czy wzbudzony
 * @param createdAt   data utworzenia
 * @param triggeredAt data wzbudzenia
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public record Alert(
        int id,
        int cryptoId,
        BigDecimal targetPrice,
        Direction direction,
        boolean isTriggered,
        LocalDateTime createdAt,
        LocalDateTime triggeredAt
) {
}
