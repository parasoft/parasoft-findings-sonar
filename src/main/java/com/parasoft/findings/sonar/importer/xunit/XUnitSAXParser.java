/*
 * Copyright 2024 Parasoft Corporation
 *
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
 */
package com.parasoft.findings.sonar.importer.xunit;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.importer.xunit.data.XUnitTestCase;
import com.parasoft.findings.sonar.importer.xunit.data.XUnitTestsContainer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

public class XUnitSAXParser {

    public XUnitTestsContainer parse(File xUnitXmlFile) throws ParserConfigurationException, SAXException, IOException {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);//$NON-NLS-1$
            saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);//$NON-NLS-1$
            saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);//$NON-NLS-1$
            SAXParser saxParser = saxParserFactory.newSAXParser();
            XUnitTestsContainer xUnitTestsContainer = new XUnitTestsContainer();
            saxParser.parse(xUnitXmlFile, new XUnitSAXParserHandler(xUnitTestsContainer));
            return xUnitTestsContainer;
    }

    public static class XUnitSAXParserHandler extends DefaultHandler {

        private final XUnitTestsContainer xUnitTestsContainer;
        private final StringBuilder currentText = new StringBuilder();
        private XUnitTestCase currentTestCase;

        public XUnitSAXParserHandler(XUnitTestsContainer xUnitTestsContainer) {
            this.xUnitTestsContainer = xUnitTestsContainer;
        }

        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) {
            if ("testcase".equals(qName)) { //$NON-NLS-1$
                String name = attributes.getValue("name"); //$NON-NLS-1$
                String file = attributes.getValue("file"); //$NON-NLS-1$
                String classname = attributes.getValue("classname"); //$NON-NLS-1$
                long time = getTimeAttributeInMS(attributes.getValue("time")); //$NON-NLS-1$

                currentTestCase = new XUnitTestCase(name, file, classname, time);
                currentText.setLength(0);
            }
        }

        public void endElement(String uri, String localName,
                               String qName) {
            if ("testcase".equals(qName)) { //$NON-NLS-1$
                xUnitTestsContainer.addTestCase(currentTestCase);
            } else if (qName.equalsIgnoreCase("failure")) { //$NON-NLS-1$
                currentTestCase.setFailure(XUnitTestCase.Status.FAILURE);
                currentTestCase.setStackTrace(currentText.toString().trim());
            } else if (qName.equalsIgnoreCase("error")) { //$NON-NLS-1$
                currentTestCase.setFailure(XUnitTestCase.Status.ERROR);
                currentTestCase.setStackTrace(currentText.toString().trim());
            }
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            currentText.append(new String(ch, start, length));
        }

        private long getTimeAttributeInMS(String value) {
            if (value == null || value.isEmpty()) {
                return 0L;
            }
            try {
                return Math.round(Double.parseDouble(value) * 1000);
            } catch (Exception e) { // parasoft-suppress OWASP2021.A5.NCE "This is intentionally designed to ensure exceptions during double parsing don't cause the process to fail."
                Logger.getLogger().debug(e.getMessage());
                return 0L;
            }
        }
    }
}
