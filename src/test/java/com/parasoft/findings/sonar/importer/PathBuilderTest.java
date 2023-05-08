/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.importer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import com.parasoft.xtest.common.api.IFileTestableInput;
import com.parasoft.xtest.common.api.ISourceRange;
import com.parasoft.xtest.results.api.IDupCodeViolation;
import com.parasoft.xtest.results.api.IFlowAnalysisPathElement;
import com.parasoft.xtest.results.api.IFlowAnalysisPathElement.Type;
import com.parasoft.xtest.results.api.IFlowAnalysisViolation;
import com.parasoft.xtest.results.api.IPathElement;
import com.parasoft.xtest.results.api.IPathElementAnnotation;
import com.parasoft.xtest.results.api.IResultLocation;

class PathBuilderTest
{
    private NewIssueLocation _issueLoc;

    @Test
    void testSetPathDupCode() throws Throwable
    {
        var context = mockContext();
        var issue = mockIssue();

        var violation = mock(IDupCodeViolation.class);
        var elements = new IPathElement[] { setupElement(mock(IPathElement.class)) };

        when(violation.getPathElements()).thenReturn(elements);

        var builder = new PathBuilder(context, issue);

        builder.setPath(violation);

        verify(issue).newLocation();
    }

    @Test
    void testSetPathFlowAnalysis() throws Throwable
    {
        var context = mockContext();
        var issue = mockIssue();

        var violation = mock(IFlowAnalysisViolation.class);
        var elements = new IFlowAnalysisPathElement[] { setupElement(mock(IFlowAnalysisPathElement.class)) };
        var type = mock(Type.class);
        List<IPathElementAnnotation> listAnno = new ArrayList<>();

        when(type.getIdentifier()).thenReturn("an Identifier");
        when(elements[0].getChildren()).thenReturn(new IFlowAnalysisPathElement[0]);
        when(elements[0].getAnnotations()).thenReturn(listAnno);
        when(elements[0].getType()).thenReturn(type);
        when(violation.getPathElements()).thenReturn(elements);

        var builder = new PathBuilder(context, issue);

        builder.setPath(violation);

        verify(_issueLoc).message("HelloWorld ");
    }

    @Test
    void testSetPathFlowAnalysis2() throws Throwable
    {
        var context = mockContext();
        var issue = mockIssue();

        var violation = mock(IFlowAnalysisViolation.class);
        var elements = new IFlowAnalysisPathElement[] { setupElement(mock(IFlowAnalysisPathElement.class)) };
        var type = mock(Type.class);
        List<IPathElementAnnotation> listAnno = new ArrayList<>();

        var anno = mock(IPathElementAnnotation.class);
        when(anno.getKind()).thenReturn("except");
        when(anno.getMessage()).thenReturn("Exception here");
        listAnno.add(anno);
        anno = mock(IPathElementAnnotation.class);
        when(anno.getKind()).thenReturn("point");
        when(anno.getMessage()).thenReturn("What is");
        listAnno.add(anno);
        anno = mock(IPathElementAnnotation.class);
        when(anno.getKind()).thenReturn("info");
        when(anno.getMessage()).thenReturn("The info");
        listAnno.add(anno);
        anno = mock(IPathElementAnnotation.class);
        when(anno.getKind()).thenReturn("other");
        when(anno.getMessage()).thenReturn("foo");
        listAnno.add(anno);

        when(type.getIdentifier()).thenReturn("an Identifier");
        when(elements[0].getChildren()).thenReturn(new IFlowAnalysisPathElement[0]);
        when(elements[0].getAnnotations()).thenReturn(listAnno);
        when(elements[0].getType()).thenReturn(type);
        when(violation.getPathElements()).thenReturn(elements);

        var builder = new PathBuilder(context, issue);

        builder.setPath(violation);
        
        verify(issue).newLocation();
        verify(_issueLoc).message("Exception here What is The info foo ");
    }

    SensorContext mockContext() throws IOException
    {
        var context = mock(SensorContext.class);
        var fs = mock(FileSystem.class);
        var predicates = mock(FilePredicates.class);
        var inputFile = mock(InputFile.class);

        List<InputFile> fileList = new ArrayList<>();
        fileList.add(inputFile);

        when(fs.inputFiles(any())).thenReturn(fileList);
        when(fs.predicates()).thenReturn(predicates);
        when(context.fileSystem()).thenReturn(fs);
        when(inputFile.contents()).thenReturn(" A code snippet\r\nHelloWorld");

        return context;
    }

    NewIssue mockIssue()
    {
        _issueLoc = mock(NewIssueLocation.class);
        var issue = mock(NewIssue.class);

        when(_issueLoc.at(any())).thenReturn(_issueLoc);
        when(_issueLoc.on(any())).thenReturn(_issueLoc);
        when(_issueLoc.message(anyString())).thenReturn(_issueLoc);
        when(issue.newLocation()).thenReturn(_issueLoc);

        return issue;
    }

    <T extends IPathElement> T setupElement(T element)
    {
        var location = mock(IResultLocation.class);
        var range = mock(ISourceRange.class);
        var input = mock(IFileTestableInput.class);
        var file = mock(File.class);

        when(input.getFileLocation()).thenReturn(file);
        when(location.getSourceRange()).thenReturn(range);
        when(location.getTestableInput()).thenReturn(input);
        when(element.getLocation()).thenReturn(location);
        when(element.getLocation()).thenReturn(location);
        when(element.getDescription()).thenReturn("This is a description");
        when(range.getStartLine()).thenReturn(2);
        when(range.getStartLineOffset()).thenReturn(0);
        when(range.getEndLine()).thenReturn(2);
        when(range.getEndLineOffset()).thenReturn(10);

        return element;
    }
}
