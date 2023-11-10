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

import static org.mozilla.xiu.browser.utils.contentdisposition.ContentDispositionField.DISPOSITION_TYPE_ATTACHMENT;
import static org.mozilla.xiu.browser.utils.contentdisposition.ContentDispositionField.DISPOSITION_TYPE_INLINE;
import static org.mozilla.xiu.browser.utils.contentdisposition.ContentDispositionField.PARAM_FILENAME;
import static org.mozilla.xiu.browser.utils.contentdisposition.ContentDispositionField.PARAM_SIZE;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a <code>Content-Disposition</code> field.
 */
public class ContentDispositionHolder {

    private String body;

    private boolean parsed = false;

    private String dispositionType = "";
    private final Map<String, String> parameters = new HashMap<>();
    private ParseException parseException;

    public ContentDispositionHolder(String body) {
        this.body = body;
    }

    public ParseException getParseException() {
        if (!parsed)
            parse();

        return parseException;
    }

    public String getDispositionType() {
        if (!parsed)
            parse();

        return dispositionType;
    }

    public String getParameter(String name) {
        if (!parsed)
            parse();

        return parameters.get(name.toLowerCase());
    }

    public Map<String, String> getParameters() {
        if (!parsed)
            parse();

        return Collections.unmodifiableMap(parameters);
    }

    public boolean isDispositionType(String dispositionType) {
        if (!parsed)
            parse();

        return this.dispositionType.equalsIgnoreCase(dispositionType);
    }

    public boolean isInline() {
        if (!parsed)
            parse();

        return dispositionType.equals(DISPOSITION_TYPE_INLINE);
    }

    public boolean isAttachment() {
        if (!parsed)
            parse();

        return dispositionType.equals(DISPOSITION_TYPE_ATTACHMENT);
    }

    public String getFilename() {
        return getParameter(PARAM_FILENAME);
    }

    public long getSize() {
        String value = getParameter(PARAM_SIZE);
        if (value == null)
            return -1;

        try {
            long size = Long.parseLong(value);
            return size < 0 ? -1 : size;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void parse() {
        ContentDispositionParser parser = new ContentDispositionParser(
                new StringReader(body));
        try {
            parser.parseAll();
        } catch (ParseException e) {
            parseException = e;
        } catch (TokenMgrError e) {
            parseException = new ParseException(e);
        }

        final String dispositionType = parser.getDispositionType();

        if (dispositionType != null) {
            this.dispositionType = dispositionType.toLowerCase(Locale.US);
            this.parameters.putAll(parser.getParameters());
        }
        parsed = true;
    }

}
