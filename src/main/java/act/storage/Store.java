package act.storage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marked a class or field to specify the {@link org.osgl.storage.IStorageService storage service}
 * instance the class or field should be linked to
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Store {
    /**
     * Specify the StorageService.
     * <p>There are three possible ways to specify a storage service</p>
     * <ul>
     *     <li>
     *         {service-id}:{context-path}. E.g. {@code "upload:img/profile"}, where
     *         {@code upload} is service id and {@code img/profile} is the context path
     *     </li>
     *     <li>
     *         {context-path}. E.g. {@code "img/profile}. In this case the service
     *         id will be the {@link StorageServiceManager#DEFAULT}
     *     </li>
     *     <li>
     *         {service-id:}. E.g. {@code "upload:"}. In this case the context path
     *         will be {@code ""}
     *     </li>
     * </ul>
     * @return the storage service locator
     */
    String value();
}
