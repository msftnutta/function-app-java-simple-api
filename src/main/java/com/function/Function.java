package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        try {
            // Parse query parameter
            final String query = request.getQueryParameters().get("name");
            final String name = request.getBody().orElse(query);

            if (name == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Please pass a name on the query string or in the request body");
                errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(errorResponse))
                    .build();
            } else {
                // Build comprehensive response
                Map<String, Object> response = new HashMap<>();
                
                // Basic response
                response.put("message", "Hello, " + name);
                response.put("name", name);
                
                // Current date and time
                response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                response.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                response.put("time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
                
                // Client IP address
                String clientIp = getClientIpAddress(request);
                response.put("clientIp", clientIp);
                
                // Browser and client information
                Map<String, Object> clientInfo = getClientInfo(request);
                response.put("clientInfo", clientInfo);
                
                // Request headers
                Map<String, String> headers = new HashMap<>();
                request.getHeaders().forEach(headers::put);
                response.put("headers", headers);
                
                // Request method and URI
                response.put("method", request.getHttpMethod().toString());
                response.put("uri", request.getUri().toString());
                
                return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(response))
                    .build();
            }
        } catch (JsonProcessingException e) {
            context.getLogger().severe("Error serializing response to JSON: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error")
                .build();
        }
    }
    
    private String getClientIpAddress(HttpRequestMessage<Optional<String>> request) {
        // Try to get IP from various headers that might contain the real client IP
        String xForwardedFor = request.getHeaders().get("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, the first one is usually the client
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().get("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        String xClientIp = request.getHeaders().get("X-Client-IP");
        if (xClientIp != null && !xClientIp.isEmpty()) {
            return xClientIp;
        }
        
        // Fallback - this might not be the actual client IP in Azure Functions
        return request.getHeaders().getOrDefault("Remote-Addr", "Unknown");
    }
    
    private Map<String, Object> getClientInfo(HttpRequestMessage<Optional<String>> request) {
        Map<String, Object> clientInfo = new HashMap<>();
        
        String userAgent = request.getHeaders().get("User-Agent");
        clientInfo.put("userAgent", userAgent != null ? userAgent : "Unknown");
        
        // Parse browser information from User-Agent
        if (userAgent != null) {
            String browser = parseBrowserFromUserAgent(userAgent);
            String os = parseOSFromUserAgent(userAgent);
            
            clientInfo.put("browser", browser);
            clientInfo.put("operatingSystem", os);
        } else {
            clientInfo.put("browser", "Unknown");
            clientInfo.put("operatingSystem", "Unknown");
        }
        
        // Other useful client information
        clientInfo.put("acceptLanguage", request.getHeaders().get("Accept-Language"));
        clientInfo.put("acceptEncoding", request.getHeaders().get("Accept-Encoding"));
        clientInfo.put("accept", request.getHeaders().get("Accept"));
        
        return clientInfo;
    }
    
    private String parseBrowserFromUserAgent(String userAgent) {
        userAgent = userAgent.toLowerCase();
        
        if (userAgent.contains("edg/")) {
            return "Microsoft Edge";
        } else if (userAgent.contains("chrome/") && !userAgent.contains("edg/")) {
            return "Google Chrome";
        } else if (userAgent.contains("firefox/")) {
            return "Mozilla Firefox";
        } else if (userAgent.contains("safari/") && !userAgent.contains("chrome/")) {
            return "Apple Safari";
        } else if (userAgent.contains("opera/") || userAgent.contains("opr/")) {
            return "Opera";
        } else if (userAgent.contains("msie") || userAgent.contains("trident/")) {
            return "Internet Explorer";
        } else {
            return "Unknown Browser";
        }
    }
    
    private String parseOSFromUserAgent(String userAgent) {
        userAgent = userAgent.toLowerCase();
        
        if (userAgent.contains("windows nt")) {
            return "Windows";
        } else if (userAgent.contains("mac os x") || userAgent.contains("macintosh")) {
            return "macOS";
        } else if (userAgent.contains("linux")) {
            return "Linux";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return "iOS";
        } else {
            return "Unknown OS";
        }
    }
}
