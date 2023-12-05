/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ScopedMock;
import org.sonar.api.utils.log.Loggers;

import com.parasoft.findings.utils.common.logging.Level;

class SonarLoggerHandlerFactoryTest
{
    Set<ScopedMock> staticMocks = new HashSet<>();

    @Test
    void testLog()
    {
        var mockLoggers = mockStatic(Loggers.class);
        staticMocks.add(mockLoggers);
        org.sonar.api.utils.log.Logger mockLogger = mock(org.sonar.api.utils.log.Logger.class);
        mockLoggers.when(() -> Loggers.get(anyString())).thenReturn(mockLogger);
        
        var factory = new SonarLoggerHandlerFactory();
        var handler = factory.getHandler();
        
        handler.log(getClass().getName(), Level.DEBUG, "My Message 1", null);
        verify(mockLogger).debug("My Message 1");
        clearInvocations(mockLogger);

        handler.log(getClass().getName(), Level.DEBUG, "My Message 2", new Exception());
        verify(mockLogger).debug(eq("My Message 2"), any(Throwable.class));
        clearInvocations(mockLogger);

        handler.log(getClass().getName(), Level.ERROR, "My Message 3", null);
        verify(mockLogger).error(eq("My Message 3"));
        clearInvocations(mockLogger);
        
        handler.log(getClass().getName(), Level.ERROR, "My Message 4", new Exception());
        verify(mockLogger).error(eq("My Message 4"), any(Throwable.class));
        clearInvocations(mockLogger);
        
        handler.log(getClass().getName(), Level.FATAL, "My Message 5", null);
        verify(mockLogger).error(eq("My Message 5"));
        clearInvocations(mockLogger);
        
        handler.log(getClass().getName(), Level.FATAL, "My Message 6", new Exception());
        verify(mockLogger).error(eq("My Message 6"), any(Throwable.class));
        clearInvocations(mockLogger);
        
        handler.log(getClass().getName(), Level.INFO, "My Message 7", null);
        verify(mockLogger).info(eq("My Message 7"));
        clearInvocations(mockLogger);
        
        handler.log(getClass().getName(), Level.INFO, "My Message 8", new Exception());
        verify(mockLogger).info(eq("My Message 8"));
        clearInvocations(mockLogger);
        
        handler.log(getClass().getName(), Level.TRACE, "My Message 9", null);
        verify(mockLogger).trace(eq("My Message 9"));
        clearInvocations(mockLogger);
        
        handler.log(getClass().getName(), Level.TRACE, "My Message 10", new Exception());
        verify(mockLogger).trace(eq("My Message 10"));
        clearInvocations(mockLogger);
        
        handler.log(getClass().getName(), Level.WARN, () -> "My Message 11", null);
        verify(mockLogger).warn(eq("My Message 11"));
        clearInvocations(mockLogger);
        
        handler.log(getClass().getName(), Level.WARN, () -> "My Message 12", new Exception());
        verify(mockLogger).warn(eq("My Message 12"), any(Throwable.class));
        clearInvocations(mockLogger);
    }
    
    @Test
    void testHandler()
    {
        var factory = new SonarLoggerHandlerFactory();

        var handler = factory.getHandler();
        assertEquals("Parasoft Sonar Logger", handler.getName());

        handler = factory.getHandler("My Name");
        assertEquals("Parasoft Sonar Logger", handler.getName());

        assertTrue(factory.isInitialized());
        
        factory.switchLoggingOff();
        factory.switchLoggingOn();
    }

    @AfterEach
    void closeMocks()
    {
        staticMocks.stream().forEach((m) -> m.close());
    }
}
