/*
 * (C) Copyright 2017-2024 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ftest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.HttpResponse;

import net.htmlparser.jericho.Source;

/**
 * HTML representation of the {@code oauth2error.jsp} page.
 *
 * @since 9.2
 */
public class OAuth2ErrorPage {

    protected static final String ELEMENT_CODE = "code";

    protected static final String ELEMENT_H1 = "h1";

    protected static final String ELEMENT_TITLE = "title";

    protected Source html;

    protected String title;

    protected String h1;

    protected String description;

    public static OAuth2ErrorPage getErrorPage(HttpClientTestRule client, String path) {
        return new OAuth2ErrorPage(
                new Source(client.buildGetRequest(path).executeAndThen(HttpResponse::getEntityString)));
    }

    public OAuth2ErrorPage(Source html) {
        this.html = html;
        title = getElementValue(ELEMENT_TITLE);
        h1 = getElementValue(ELEMENT_H1);
        description = getElementValue(ELEMENT_CODE);
    }

    public void checkTitle(String expected) {
        assertTrue(title.endsWith(expected));
    }

    public void checkH1(String expected) {
        assertEquals(expected, h1);
    }

    public void checkDescription(String expected) {
        assertEquals(expected, description);
    }

    protected String getElementValue(String element) {
        return html.getFirstElement(element).getTextExtractor().toString();
    }

}
