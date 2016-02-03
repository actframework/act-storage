package act.storage.db;

import act.asm.Type;

/**
 * The implementation class of this interface provides
 * hooks to a specific {@link act.db.DbPlugin ACT Database layer}
 */
public interface DbHooker {

    /**
     * Returns the type of entity annotation
     * @return the entity annotation type
     */
    Class entityAnnotation();

    /**
     * Returns the type of transient annotation
     * @return the transient annotation type
     */
    Class transientAnnotationType();

    /**
     * plugin logic to process {@link org.osgl.storage.ISObject sobject}
     * and {@link String key} conversion during load and persist process
     */
    void hookLifecycleInterceptors();
}
