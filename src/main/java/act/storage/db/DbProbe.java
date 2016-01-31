package act.storage.db;

/**
 * The implementation of this interface probe
 * if a specific {@link act.db.DbPlugin database layer}
 * is presented in the current application class loader
 */
public interface DbProbe {
    /**
     * Check if the database layer exists
     * @return {@code true if the database layer exists}
     */
    boolean exists();

    /**
     * Returns the class name of {@link act.storage.db.DbHooker}
     * implementation
     * @return the class name
     */
    String dbHookerClass();
}
