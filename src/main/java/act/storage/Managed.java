package act.storage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marked a {@link org.osgl.storage.ISObject} typed field to be managed by framework.
 * <p>
 *     If a sobject field is marked as managed, then developer do not need to write code to save
 *     the sobject and create another field to store the key
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Managed {
    /**
     * Specifies how to deal with updated SObject field that is marked
     * with this {@code Managed} annotation
     *
     * @return the Update policy
     * @see UpdatePolicy
     */
    UpdatePolicy value() default UpdatePolicy.DELETE_OLD_DATA;
}
