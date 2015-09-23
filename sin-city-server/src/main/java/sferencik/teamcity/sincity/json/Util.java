package sferencik.teamcity.sincity.json;

public class Util {
    static String nullifyEmptyString(String s) {
        return s == null || s.equals("") ? null : s;
    }
}
