package gcfv2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerifiers;

public class HelloHttpFunction implements HttpFunction {
  ObjectMapper mapper = new ObjectMapper();

  public void service(final HttpRequest request, final HttpResponse response) throws IOException {
    if(!authValidation(request, response)){
      return;
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

  private boolean authValidation(HttpRequest request, HttpResponse response) throws IOException {
    // 1. Get the Authorization header

    Map<String, List<String>> headers = request.getHeaders();
    if(headers.get("Authorization").isEmpty()){
      System.out.println("No Authorization header is there...");
      response.setStatusCode(401);
      response.getWriter().write("Missing Authorization header");
      return false;
    }
    String authHeader = headers.get("Authorization").get(0);
    System.out.println("Token is there :- "+authHeader);
    if (!authHeader.startsWith("Bearer ")) {
      response.setStatusCode(401);
      response.getWriter().write("Invalid Authorization header");
      return false;
    }

    // 2. Extract the token string
    String token = authHeader.substring(7);

    try {
      AccessTokenVerifier jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
              .setIssuer("https://dev-jawlnaqsx3hptwx5.us.auth0.com/") // Your Auth0/Okta Issuer
              .setAudience("https://my-dialogflow-webhook")           // The Audience you set in Auth0
              .build();
      // 3. Validate the token (Signature, Exp, Issuer, and Audience)
      Jwt jwt = jwtVerifier.decode(token);

      // 4. (Optional) Check for a specific scope
      var scopes = jwt.getClaims().get("scope");
      if (scopes == null || !scopes.toString().contains("access:webhook")) {
        response.setStatusCode(403);
        response.getWriter().write("Forbidden: Missing required scope");
        System.out.println("No scope !!!!");
        return false;
      }
      System.out.println("Token validation successfully");
      return true;
    } catch (Exception e) {
      // Validation failed (token expired, wrong signature, etc.)
      response.setStatusCode(401);
      System.out.println("Token Validation failed:----");
      response.getWriter().write("Token validation failed: " + e.getMessage());
      return false;
    }
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