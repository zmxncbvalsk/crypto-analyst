package pl.dzik.exception;

/**
 * Wyjątek rzucany w przypadku błędów związanych z bazą danych SQLite.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class DatabaseException extends Exception {

    /**
     * Tworzy wyjątek bazy danych z komunikatem opisującym problem.
     *
     * @param message opis błędu operacji na bazie danych
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Tworzy wyjątek bazy danych z komunikatem i przyczyną pierwotną.
     *
     * @param message opis błędu
     * @param cause   pierwotny wyjątek
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}