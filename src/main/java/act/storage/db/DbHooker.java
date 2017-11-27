package act.storage.db;

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
