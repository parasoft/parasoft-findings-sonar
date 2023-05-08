/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar;

import java.util.function.Supplier;

import org.sonar.api.utils.log.Loggers;

import com.parasoft.xtest.logging.api.ILoggerHandler;
import com.parasoft.xtest.logging.api.ILoggerHandlerFactory;
import com.parasoft.xtest.logging.api.ParasoftLevel;

public class SonarLoggerHandlerFactory
    implements ILoggerHandlerFactory
{
    @Override
    public ILoggerHandler getHandler(String sName)
    {
        return new SonarLoggerHandler(sName);
    }

    @Override
    public ILoggerHandler getHandler()
    {
        return new SonarLoggerHandler(getDefaultName());
    }

    @Override
    public String getDefaultName()
    {
        return "com.parasoft.findings.sonar"; //$NON-NLS-1$
    }

    @Override
    public boolean isInitialized()
    {
        return true;
    }

    @Override
    public void switchLoggingOff()
    {
    }

    @Override
    public void switchLoggingOn()
    {
    }

    private static class SonarLoggerHandler implements ILoggerHandler
    {
        private final org.sonar.api.utils.log.Logger LOGGER;

        SonarLoggerHandler(String name)
        {
            LOGGER = Loggers.get(name);
        }

        @Override
        public String getName()
        {
            return "Parasoft Sonar Logger"; //$NON-NLS-1$
        }

        @Override
        public void log(String sWrapperClassName, ParasoftLevel level, Object object, Throwable throwable)
        {
            switch (level.getLevel()) {
            case ParasoftLevel.TRACE_INT:
                LOGGER.trace(object.toString());
                break;
            case ParasoftLevel.INFO_INT:
                LOGGER.info(object.toString());
                break;
            case ParasoftLevel.WARN_INT:
                if (throwable != null) {
                    LOGGER.warn(object.toString(), throwable);
                } else {
                    LOGGER.warn(object.toString());
                }
                break;
            case ParasoftLevel.ERROR_INT:
            case ParasoftLevel.FATAL_INT:
                if (throwable != null) {
                    LOGGER.error(object.toString(), throwable);
                } else {
                    LOGGER.error(object.toString());
                }
                break;
            case ParasoftLevel.DEBUG_INT:
            default:
                if (throwable != null) {
                    LOGGER.debug(object.toString(), throwable);
                } else {
                    LOGGER.debug(object.toString());
                }
                break;
            }
        }

        @Override
        public void log(String sWrapperClassName, ParasoftLevel level, Supplier<Object> objectSupplier, Throwable throwable)
        {
            log(sWrapperClassName, level, objectSupplier.get(), throwable);
        }
    }
}
