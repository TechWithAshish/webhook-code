package gcfv2;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class WebhookResponse {
    public FulfillmentResponse fulfillmentResponse;
    public SessionInfo sessionInfo;
}