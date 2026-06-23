package pl.dzik.exception;

/**
 * Wyjątek rzucany, gdy dane alertu cenowego nie przechodzą walidacji biznesowej.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class AlertValidationException extends Exception {

    /**
     * Tworzy wyjątek walidacji alertu z komunikatem opisującym naruszenie reguły.
     *
     * @param message opis błędu walidacji (np. nieprawidłowa cena docelowa)
     */
    public AlertValidationException(String message) {
        super(message);
    }
}