package gcfv2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class WebhookRequest {
    public String detectIntentResponseId;
    public String languageCode;
    public FulfillmentInfo fulfillmentInfo;
    //public IntentInfo intentInfo;
    //public PageInfo pageInfo;
    public SessionInfo sessionInfo;
    //public Object messages;
    //public Object payload;
    //public Object sentimentAnalysisResult;
    //public Object languageInfo;
}
