package sferencik.teamcity.sincity.json;

import jetbrains.buildServer.util.StringUtil;

public class Util {
    static String nullifyEmptyString(String s) {
        return StringUtil.isEmpty(s) ? null : s;
    }
}
