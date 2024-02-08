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

    private final static SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

    public XUnitTestsContainer parse(File xUnitXmlFile) throws ParserConfigurationException, SAXException, IOException {
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
                                 Attributes attributes) throws SAXException {
            if ("testcase".equals(qName)) {
                String name = attributes.getValue("name");
                String file = attributes.getValue("file");
                String classname = attributes.getValue("classname");
                long time = getTimeAttributeInMS(attributes.getValue("time"), 0L);

                currentTestCase = new XUnitTestCase(name, file, classname, time);
                currentText.setLength(0);
            }
        }

        public void endElement(String uri, String localName,
                               String qName) throws SAXException {
            if ("testcase".equals(qName)) {
                xUnitTestsContainer.addTestCase(currentTestCase);
            } else if (qName.equalsIgnoreCase("failure")) {
                currentTestCase.setFailure(XUnitTestCase.Status.FAILURE);
                currentTestCase.setStackTrace(currentText.toString().trim());
            } else if (qName.equalsIgnoreCase("error")) {
                currentTestCase.setFailure(XUnitTestCase.Status.ERROR);
                currentTestCase.setStackTrace(currentText.toString().trim());
            }
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            currentText.append(new String(ch, start, length));
        }

        private long getTimeAttributeInMS(String value, long defaultValue) {
            if (value == null || value.isEmpty()) {
                return defaultValue;
            }
            try {
                return (long) (Double.parseDouble(value) * 1000L);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }
}
