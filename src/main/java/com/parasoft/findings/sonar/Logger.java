/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar;

import com.parasoft.xtest.logging.api.ParasoftLogger;

public final class Logger
{
    private final static ParasoftLogger _LOGGER = ParasoftLogger.getLogger(Logger.class);

    private Logger()
    {
    }

    public static ParasoftLogger getLogger()
    {
        return _LOGGER;
    }
}
