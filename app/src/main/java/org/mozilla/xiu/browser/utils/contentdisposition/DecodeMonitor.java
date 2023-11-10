/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.mozilla.xiu.browser.utils.contentdisposition;

/**
 * This class is used to drive how decoder/parser should deal with malformed
 * and unexpected data.
 *
 * 2 basic implementations are provided:
 * <ul>
 * <li>{@link #STRICT} return "true" on any occurrence</li>
 * <li>{@link #SILENT} ignores any problem</li>
 * </ul>
 */
public class DecodeMonitor {

    /**
     * The STRICT monitor throws an exception on every event.
     */
    public static final DecodeMonitor STRICT = new DecodeMonitor() {

        @Override
        public boolean warn(String error, String dropDesc) {
            return true;
        }

        @Override
        public boolean isListening() {
            return true;
        }
    };

    /**
     * The SILENT monitor ignore requests.
     */
    public static final DecodeMonitor SILENT = new DecodeMonitor();

    public boolean warn(String error, String dropDesc) {
        return false;
    }

    public boolean isListening() {
        return false;
    }

}
