package act.storage;

/**
 * Defines how to deal with updating to {@link org.osgl.storage.ISObject} typed field
 */
public enum UpdatePolicy {
    /**
     * If a field is specified as {@code REJECT} and the framework
     * detect the sobject has been updated, the framework will throw
     * out an exception to reject the update when persist the entity
     */
    REJECT,

    /**
     * If a field is specified as {@code DELETE_OLD_DATA} and framework
     * detect the sobject field has been updated, the framework will
     * delete old sobject and then save the updated one when persist
     * the entity
     */
    DELETE_OLD_DATA,

    /**
     * If a field is specified as {@code KEEP_OLD_DATA} and framework
     * detect the sobject field has been updated, the framework will
     * NOT delete old sobject. However the updated one will be saved as well
     */
    KEEP_OLD_DATA
}
