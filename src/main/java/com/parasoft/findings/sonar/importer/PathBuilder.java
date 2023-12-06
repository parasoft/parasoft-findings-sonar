/*
 * Copyright 2018 Parasoft Corporation
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

package com.parasoft.findings.sonar.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.utils.common.IStringConstants;
import com.parasoft.findings.utils.common.util.CollectionUtil;
import com.parasoft.findings.utils.common.util.StringUtil;
import com.parasoft.findings.utils.results.testableinput.IFileTestableInput;
import com.parasoft.findings.utils.results.violations.*;
import com.parasoft.findings.utils.results.testableinput.ITestableInput;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

/**
 * Creates complete path information for a violation
 */
public class PathBuilder
{
    private final SensorContext _context;
    private final NewIssue _issue;

    public PathBuilder(SensorContext context, NewIssue issue)
    {
        _context = context;
        _issue = issue;
    }

    public void setPath(IRuleViolation violation)
    {
        IPathElement[] elements = new IPathElement[0];
        if (violation instanceof IFlowAnalysisViolation) {
            elements = ((IFlowAnalysisViolation)violation).getPathElements();
        } else if (violation instanceof DupCodeViolation) {
            elements = ((DupCodeViolation)violation).getPathElements();
        }

        if ((elements == null) || (elements.length == 0)) {
            return;
        }

        var paths = getPath(elements);
        if (paths.isEmpty()) {
            return;
        }
        Collections.reverse(paths);

        if (violation instanceof IFlowAnalysisViolation) {
            _issue.addFlow(paths);
        } else if (violation instanceof DupCodeViolation) {
            for (var path : paths) {
                _issue.addLocation(path);
            }
        }
    }

    private List<NewIssueLocation> getPath(IPathElement[] descriptors)
    {
        List<NewIssueLocation> result = new ArrayList<>();
        boolean useAnnotations = useAnnotations(descriptors);
        for (var descriptor : descriptors) {
            NewIssueLocation descriptorLocation = createElement(descriptor, useAnnotations);
            if (descriptorLocation != null) {
                result.add(descriptorLocation);
            }
            if (descriptor instanceof IFlowAnalysisPathElement) {
                result.addAll(getChildren((IFlowAnalysisPathElement)descriptor));
            }
        }
        return result;
    }

    private NewIssueLocation createElement(IPathElement descriptor, boolean useAnnotations)
    {
        ResultLocation location = descriptor.getLocation();
        if (location == null) {
            return null;
        }
        InputFile inputFile = findInputFile(location, _context);
        if (inputFile == null) {
            return null;
        }
        String message = getMessage(descriptor, false, useAnnotations);
        if (message.isEmpty()) {
            return null;
        }
        SourceRange sr = location.getSourceRange();
        return _issue.newLocation().on(inputFile)
            .at(inputFile.newRange(sr.getStartLine(), sr.getStartLineOffset(), sr.getEndLine(), sr.getEndLineOffset())).message(message);
    }

    private static InputFile findInputFile(ResultLocation location, SensorContext context)
    {
        ITestableInput testableInput = location.getTestableInput();
        if (!(testableInput instanceof IFileTestableInput)) {
            return null;
        }
        var fs = context.fileSystem();
        Iterable<InputFile> javaFiles = fs
            .inputFiles(fs.predicates().hasAbsolutePath(((IFileTestableInput)testableInput).getFileLocation().getAbsolutePath()));

        List<InputFile> files = StreamSupport.stream(javaFiles.spliterator(), false).collect(Collectors.<InputFile>toList());
        if (files.size() >= 1) {
            return files.get(0);
        }
        return null;
    }

    private List<NewIssueLocation> getChildren(IFlowAnalysisPathElement descriptor)
    {
        return getPath(descriptor.getChildren());
    }

    private String getMessage(IPathElement element, boolean bFullDescription, boolean useAnnotation)
    {
        if (element == null) {
            return IStringConstants.EMPTY;
        } else if (!(element instanceof IFlowAnalysisPathElement)) {
            return StringUtil.getNotNull(element.getDescription());
        }

        IFlowAnalysisPathElement descriptor = (IFlowAnalysisPathElement)element;
        if ((descriptor.getType() == null) || (descriptor.getType().getIdentifier() == null)) {
            return IStringConstants.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        if (useAnnotation) {
            addAnnotations(sb, descriptor, bFullDescription);
        } else {
            addNodeMessage(sb, descriptor);
        }

        return sb.toString();
    }

    private String getSourceText(SourceRange range, InputFile file)
    {
        try {
            String contents = file.contents();
            String[] lines = contents.lines().toArray(String[]::new);
            String line = lines[range.getStartLine()-1];
            int startOffset = range.getStartLineOffset();
            return startOffset >= 0 && startOffset < line.length() ?
                    line.substring(startOffset) : line;
        } catch (IOException e) {
            Logger.getLogger().error(e);
        }
        return null;
    }

    private static void addNormalMessages(StringBuilder sb, List<PathElementAnnotation> annotations)
    {
        for (PathElementAnnotation annotation : annotations) {
            addMessage(sb, annotation.getMessage());
        }
    }

    private static void addMessage(StringBuilder sb, String message)
    {
        if (message != null) {
            sb.append(message).append(" ");
        }
    }

    private void addAnnotations(StringBuilder sb, IFlowAnalysisPathElement descriptor, boolean bFullDescription)
    {
        if (descriptor.getAnnotations() == null) {
            return;
        }

        List<PathElementAnnotation> annotations = new ArrayList<>(descriptor.getAnnotations());
        if (annotations.isEmpty()) {
            addNodeMessage(sb, descriptor);
            sb.append(" ");
        } else {
            addImportantMessages(sb, annotations, bFullDescription, descriptor.getType().getIdentifier());
            addNormalMessages(sb, annotations);
        }
    }

    private void addNodeMessage(StringBuilder sb, IFlowAnalysisPathElement descriptor)
    {
        ResultLocation location = descriptor.getLocation();
        if (location == null) {
            return;
        }
        InputFile inputFile = findInputFile(location, _context);
        SourceRange sr = location.getSourceRange();
        String message = inputFile != null && sr != null ? getSourceText(sr, inputFile).trim() : null;
        if (message != null) {
            addMessage(sb, message);
        }
    }

    private static void addImportantMessages(StringBuilder sb, List<PathElementAnnotation> annotations, boolean bFullDescription,
        String descriptorIdentifier)
    {
        for (Iterator<PathElementAnnotation> it = annotations.iterator(); it.hasNext();) {
            PathElementAnnotation annotation = it.next();
            String kind = annotation.getKind();
            if (kind == null) {
                it.remove();
            } else if (excludedFromMessages.contains(kind)) {
                if (!bFullDescription) {
                    addMessage(sb, annotation.getMessage());
                }
                it.remove();
            } else if (descriptorIdentifier.contains(String.valueOf(IFlowAnalysisPathElement.RULE)) && kind.equals(ANNOTATION_KIND_INFO)) {
                addMessage(sb, annotation.getMessage());
                it.remove();
            } else if (kind.equals(ANNOTATION_KIND_EXCEPTION)) {
                addMessage(sb, annotation.getMessage());
                it.remove();
            }
        }
    }

    private static boolean useAnnotations(IPathElement[] descriptors)
    {
        for (var descriptor : descriptors) {
            if (descriptor instanceof IFlowAnalysisPathElement
                    && CollectionUtil.isNonEmpty(((IFlowAnalysisPathElement)descriptor).getAnnotations())) {
                return true;
            }
        }
        return false;
    }

    private static final String ANNOTATION_KIND_POINT = "point"; //$NON-NLS-1$

    private static final String ANNOTATION_KIND_CAUSE = "cause"; //$NON-NLS-1$

    private static final String ANNOTATION_KIND_EXCEPTION = "except"; //$NON-NLS-1$

    private static final String ANNOTATION_KIND_INFO = "info"; //$NON-NLS-1$

    private static List<String> excludedFromMessages = Arrays.asList(ANNOTATION_KIND_POINT, ANNOTATION_KIND_CAUSE);

}
