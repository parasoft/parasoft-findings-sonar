/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.ParasoftProduct;
import com.parasoft.findings.sonar.importer.ParasoftDottestAndCpptestTestsParser;

public class CpptestFindingsSensor extends AbstractParasoftFindingsSensor
{
    private final ParasoftDottestAndCpptestTestsParser parasoftDottestAndCpptestTestsParser;
    public CpptestFindingsSensor(ParasoftDottestAndCpptestTestsParser parasoftDottestAndCpptestTestsParser)
    {
        super(ParasoftProduct.CPPTEST);
        this.parasoftDottestAndCpptestTestsParser = parasoftDottestAndCpptestTestsParser;
    }
}
