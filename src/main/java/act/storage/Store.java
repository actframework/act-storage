package act.storage;

/*-
 * #%L
 * ACT Storage
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
    String value() default "default:";

    /**
     * Specifies how to deal with updated SObject field that is marked
     * with this {@code Managed} annotation
     *
     * @return the Update policy
     * @see UpdatePolicy
     */
    UpdatePolicy updatePolicy() default UpdatePolicy.DELETE_OLD_DATA;
}
