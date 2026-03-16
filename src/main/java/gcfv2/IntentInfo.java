package gcfv2;

import java.util.Map;

class IntentInfo{
    public String lastMatchedIntent;
    public String displayName;
    public Map<String, Object> parameters;
    public int confidence;
}