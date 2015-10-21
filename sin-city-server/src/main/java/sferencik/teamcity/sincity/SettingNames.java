package sferencik.teamcity.sincity;

public class SettingNames {
    public String getTagNameForBuildsNotTriggeredBySinCity() {
        return "sincity.tagNameForBuildsNotTriggeredBySinCity";
    }

    public String getTagNameForBuildsTriggeredBySinCity() {
        return "sincity.tagNameForBuildsTriggeredBySinCity";
    }

    public String getRbTriggerOnBuildProblem() {
        return "sincity.rbTriggerOnBuildProblem";
    }

    public String getRbTriggerOnTestFailure() {
        return "sincity.rbTriggerOnTestFailure";
    }

    public String getNoTrigger() {
        return "No";
    }

    public String getTriggerOnNew() {
        return "New";
    }

    public String getTriggerOnAll() {
        return "All";
    }

    public String getCbSetBuildProblemJsonParameter() {
        return "sincity.cbSetBuildProblemJsonParameter";
    }

    public String getCbSetTestFailureJsonParameter() {
        return "sincity.cbSetTestFailureJsonParameter";
    }
}
