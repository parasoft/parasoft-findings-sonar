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
package com.parasoft.findings.sonar.importer.xunitdata;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.staxmate.in.ElementFilter;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.ParsingUtils;

import javax.xml.stream.XMLStreamException;
import java.text.ParseException;
import java.util.Locale;

public class XUnitStaxHandler {

  private final XUnitTestIndex index;

  public XUnitStaxHandler(XUnitTestIndex index) {
    this.index = index;
  }

  public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
    SMInputCursor testSuite = rootCursor.constructDescendantCursor(new ElementFilter("testsuite"));
    SMEvent testSuiteEvent;
    for (testSuiteEvent = testSuite.getNext(); testSuiteEvent != null; testSuiteEvent = testSuite.getNext()) {
      if (testSuiteEvent.compareTo(SMEvent.START_ELEMENT) == 0) {
        String testSuiteClassName = testSuite.getAttrValue("name");
        parseTestCase(testSuiteClassName, testSuite.childCursor(new ElementFilter("testcase")));
      }
    }
  }

  private void parseTestCase(String testSuiteClassName, SMInputCursor testCase) throws XMLStreamException {
    for (SMEvent event = testCase.getNext(); event != null; event = testCase.getNext()) {
      if (event.compareTo(SMEvent.START_ELEMENT) == 0) {
        String testFilePath = testCase.getAttrValue("file");
        XUnitTestClassReport classReport = index.index(testFilePath);
        parseTestCase(testCase, testSuiteClassName, classReport);
      }
    }
  }

  private static void parseTestCase(SMInputCursor testCaseCursor, String testSuiteClassName, XUnitTestClassReport report) throws XMLStreamException {
    report.add(parseTestResult(testCaseCursor, testSuiteClassName));
  }

  private static void setStackAndMessage(XUnitTestResult result, SMInputCursor stackAndMessageCursor) throws XMLStreamException {
    result.setMessage(stackAndMessageCursor.getAttrValue("message"));
    String stack = stackAndMessageCursor.collectDescendantText();
    result.setStackTrace(stack);
  }

  private static XUnitTestResult parseTestResult(SMInputCursor testCaseCursor, String testSuiteClassName) throws XMLStreamException {
    XUnitTestResult detail = new XUnitTestResult();
    String name = getTestCaseName(testCaseCursor);
    detail.setName(name);
    detail.setTestSuiteClassName(testSuiteClassName);

    String status = XUnitTestResult.STATUS_OK;
    String time = testCaseCursor.getAttrValue("time");
    Long duration = null;

    SMInputCursor childNode = testCaseCursor.descendantElementCursor();
    if (childNode.getNext() != null) {
      String elementName = childNode.getLocalName();
      if ("skipped".equals(elementName)) {
        status = XUnitTestResult.STATUS_SKIPPED;
        // bug with xUnit reporting wrong time for skipped tests
        duration = 0L;

      } else if ("failure".equals(elementName)) {
        status = XUnitTestResult.STATUS_FAILURE;
        setStackAndMessage(detail, childNode);

      } else if ("error".equals(elementName)) {
        status = XUnitTestResult.STATUS_ERROR;
        setStackAndMessage(detail, childNode);
      }
    }
    while (childNode.getNext() != null) {
      // make sure we loop till the end of the elements cursor
    }
    if (duration == null) {
      duration = getTimeAttributeInMS(time);
    }
    detail.setDurationMilliseconds(duration);
    detail.setStatus(status);
    return detail;
  }

  private static long getTimeAttributeInMS(String value) throws XMLStreamException {
    // hardcoded to Locale.ENGLISH see http://jira.codehaus.org/browse/SONAR-602
    try {
      Double time = ParsingUtils.parseNumber(value, Locale.ENGLISH);
      return !Double.isNaN(time) ? (long) ParsingUtils.scaleValue(time * 1000, 3) : 0L; // parasoft-suppress PB.NUM.CLP-2 "expected cast to lower precision"
    } catch (ParseException e) {
      throw new XMLStreamException(e);
    }
  }

  private static String getTestCaseName(SMInputCursor testCaseCursor) throws XMLStreamException {
    String classname = testCaseCursor.getAttrValue("classname");
    String name = testCaseCursor.getAttrValue("name");
    if (StringUtils.contains(classname, "$")) {
      return StringUtils.substringAfter(classname, "$") + "/" + name;
    }
    return name;
  }

}
