package com.parasoft.findings.sonar;

import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;

public class SonarAPIVersionUtil {

    public static boolean isAPIVersionAtLeast10_1(SonarRuntime sonarRuntime) {
        return sonarRuntime
                .getApiVersion()
                .isGreaterThanOrEqual(Version.create(10, 1));
    }
}
