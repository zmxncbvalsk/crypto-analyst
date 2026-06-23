package pl.dzik.exception;

/**
 * Wyjątek rzucany w przypadku problemów z komunikacją z API
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class ApiException extends Exception {
    /**
     * Tworzy wyjątek API z komunikatem opisującym problem.
     *
     * @param message opis błędu
     */
    public ApiException(String message) {
        super(message);
    }

    /**
     * Tworzy wyjątek API z komunikatem i przyczyną pierwotną.
     *
     * @param message opis błędu
     * @param cause   pierwotny wyjątek
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
