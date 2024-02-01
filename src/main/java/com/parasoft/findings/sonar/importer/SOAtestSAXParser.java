/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.parasoft.findings.sonar.importer;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.soatest.SOAtestReport;
import com.parasoft.findings.sonar.soatest.SOAtestReportMapper;
import com.parasoft.findings.sonar.soatest.SOAtestTestCase;
import com.parasoft.findings.utils.common.nls.NLS;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.sonar.api.utils.ParsingUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

public class SOAtestSAXParser {

    private final SAXReader reader;
    private final SOAtestReportMapper reportMapper;

    public SOAtestSAXParser(SOAtestReportMapper reportMapper) {
        this.reportMapper = reportMapper;
        reader = new SAXReader();
    }

    public void parse(File report) throws DocumentException, XMLStreamException {
        Document document = reader.read(report);
        Element testsuiteRoot = document.getRootElement();
        if (!"testsuites".equals(testsuiteRoot.getName())) {
            // TODO log?
             return;
        }

        List<Element> testsuites = testsuiteRoot.elements("testsuite");
        if (testsuites == null || testsuites.isEmpty()) {
            Logger.getLogger().error(NLS.getFormatted(Messages.InvalidReportFile, report.getAbsolutePath()));
            return;
        }
        for (Element testsuite : testsuites) {
            parseTestSuite(testsuite);
        }
    }

    private void parseTestSuite(Element testsuite) throws XMLStreamException {
        String testSuiteClassName = testsuite.attributeValue("name");
        for (Element testcase : testsuite.elements("testcase")) {
            parseTestCase(testSuiteClassName, testcase);
        }
    }

    private void parseTestCase(String testSuiteClassName, Element testcase) throws XMLStreamException {
        String testClassName = getClassname(testcase, testSuiteClassName);
        SOAtestReport report = reportMapper.getOrCreateTestReport(testClassName);
        parseTestCase(testcase, testSuiteClassName, report);
    }

    private void parseTestCase(Element testcase, String testSuiteClassName, SOAtestReport report) throws XMLStreamException {
        report.addTestCaseDataFrom(parseTestResult(testcase, testSuiteClassName));
    }

    private SOAtestTestCase parseTestResult(Element testcase, String testSuiteClassName) throws XMLStreamException {
        SOAtestTestCase detail = new SOAtestTestCase();
        String name = getTestCaseName(testcase);
        detail.setName(name);
        detail.setTestSuiteName(testSuiteClassName);

        if (testcase.elements("failure") != null) {
            detail.setFailure(true);
        }
        String time = testcase.attributeValue("time");
        long duration = getTimeAttributeInMS(time);
        detail.setDurationMilliseconds(duration);
        return detail;
    }

    private static String getClassname(Element testcase, String defaultClassname) {
        String testClassName = testcase.attributeValue("classname");
        if (StringUtils.isNotBlank(testClassName) && testClassName.endsWith(")")) {
            int openParenthesisIndex = testClassName.indexOf('(');
            if (openParenthesisIndex > 0) {
                testClassName = testClassName.substring(0, openParenthesisIndex);
            }
        }
        return StringUtils.defaultIfBlank(testClassName, defaultClassname);
    }

    private static long getTimeAttributeInMS(String value) throws XMLStreamException {
        // hardcoded to Locale.ENGLISH see http://jira.codehaus.org/browse/SONAR-602
        try {
            double time = ParsingUtils.parseNumber(value, Locale.ENGLISH);
            return !Double.isNaN(time) ? (long) ParsingUtils.scaleValue(time * 1000, 3) : 0L; // parasoft-suppress PB.NUM.CLP-2 "expected cast to lower precision"
        } catch (ParseException e) {
            throw new XMLStreamException(e);
        }
    }

    private static String getTestCaseName(Element testcase) {
        String classname = testcase.attributeValue("classname");
        String name = testcase.attributeValue("name");
        if (StringUtils.contains(classname, "$")) {
            return StringUtils.substringAfter(classname, "$") + "/" + name;
        }
        return name;
    }
}
