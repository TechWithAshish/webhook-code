package gcfv2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

public class HelloHttpFunction implements HttpFunction {
  ObjectMapper mapper = new ObjectMapper();

  public void service(final HttpRequest request, final HttpResponse response) throws Exception {
    Map<String, List<String>> headers = request.getHeaders();
    for(String key : headers.keySet()){
      System.out.println(key+" "+headers.get(key).toString());
    }
    InputStream inputStream = request.getInputStream();
    WebhookRequest webhookRequest = mapper.readValue(inputStream, WebhookRequest.class);
    System.out.println("Request: " + webhookRequest);

    WebhookResponse webhookResponse;
    String tag = webhookRequest.fulfillmentInfo.tag;

    if ("confirm".equals(tag)) {
      webhookResponse = confirm(webhookRequest);
    } else {
      throw new RuntimeException("Unknown tag: " + tag);
    }

    byte[] responseBytes = mapper.writeValueAsBytes(webhookResponse);

    response.setContentType("application/json");
    response.setStatusCode(200);

    OutputStream os = response.getOutputStream();
    os.write(responseBytes);
    os.close();
  }

  private WebhookResponse confirm(WebhookRequest request) {

    String size = String.valueOf(
            request.sessionInfo.parameters.get("size"));

    String color = String.valueOf(
            request.sessionInfo.parameters.get("color"));

    String message = String.format(
            "You can pick up your order for a %s %s shirt in 5 days.",
            size, color);

    Text text = new Text();
    text.text = List.of(message);

    ResponseMessage rm = new ResponseMessage();
    rm.text = text;

    FulfillmentResponse fr = new FulfillmentResponse();
    fr.messages = List.of(rm);

    Map<String, Object> params = new HashMap<>();
    params.put("cancel-period", "2");

    SessionInfo sessionInfo = new SessionInfo();
    sessionInfo.parameters = params;

    WebhookResponse response = new WebhookResponse();
    response.fulfillmentResponse = fr;
    response.sessionInfo = sessionInfo;

    return response;
  }

  public void handleError(HttpResponse exchange, Exception e) throws IOException {

    String error = "ERROR: " + e.getMessage();
    OutputStream outputStream = exchange.getOutputStream();
    outputStream.write(error.getBytes());
    exchange.setStatusCode(500);
    outputStream.close();
  }
}