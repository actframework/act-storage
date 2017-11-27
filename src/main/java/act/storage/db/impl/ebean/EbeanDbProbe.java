package act.storage.db.impl.ebean;

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

import act.Act;
import act.storage.db.DbProbe;

/**
 * Check if Ebean plugin is loaded
 */
public class EbeanDbProbe extends DbProbe {
    @Override
    public boolean exists() {
        return null != Act.dbManager().plugin("act.db.ebean.EbeanPlugin");
    }

    @Override
    public String dbHookerClass() {
        return "act.storage.db.impl.ebean.EbeanDbHooker";
    }
}
