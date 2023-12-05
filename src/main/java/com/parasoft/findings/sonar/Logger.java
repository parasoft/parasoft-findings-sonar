/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar;

import com.parasoft.findings.utils.common.logging.FindingsLogger;

public final class Logger
{
    private final static FindingsLogger _LOGGER = FindingsLogger.getLogger(Logger.class);

    private Logger()
    {
    }

    public static FindingsLogger getLogger()
    {
        return _LOGGER;
    }
}
