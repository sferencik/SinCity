package sferencik.teamcity.sincity;

/**
 * Created by ferencis on 18/09/2015.
 */
public class SettingNames {
    private static final String IS_TRIGGER_ON_ANY_TEST_FAILURE = "isTriggerOnAnyTestFailure";
    private static final String NON_SIN_CITY_TAG = "nonSinCityTag";
    private static final String SIN_CITY_TAG = "sinCityTag";

    public String getIsTriggerOnAnyTestFailure() {
        return IS_TRIGGER_ON_ANY_TEST_FAILURE;
    }

    public String getNonSinCityTag() {
        return NON_SIN_CITY_TAG;
    }

    public String getSinCityTag() {
        return SIN_CITY_TAG;
    }
}
