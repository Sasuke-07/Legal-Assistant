package com.portal.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service class that provides legal advice by processing user queries
 * and generating appropriate responses.
 */
public class LegalAPIService {
    // Store console logs for browser display
    private static final List<LogEntry> consoleLogs = new LinkedList<>();
    private static final int MAX_LOGS = 100; // Maximum number of logs to keep
    
    // API configuration
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final String API_KEY = "AIzaSyBvNBKiyAFAsWmjRXbM1lpr4p1WmbgME6o"; // Google AI Studio API Key
    private static final boolean USE_API = true; // Set to true to use actual Google AI API
    
    /**
     * Log entry class to store log information
     */
    public static class LogEntry {
        private final String level;
        private final String message;
        private final long timestamp;
        
        public LogEntry(String level, String message) {
            this.level = level;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getLevel() {
            return level;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "[" + level + "] " + message;
        }
        
        /**
         * Convert the log entry to JavaScript console log code
         */
        public String toJavaScript() {
            if ("INFO".equals(level)) {
                return "console.log('" + escapeJsString(message) + "');";
            } else if ("WARNING".equals(level)) {
                return "console.warn('" + escapeJsString(message) + "');";
            } else if ("ERROR".equals(level) || "SEVERE".equals(level)) {
                return "console.error('" + escapeJsString(message) + "');";
            } else {
                return "console.info('" + escapeJsString(message) + "');";
            }
        }
        
        private String escapeJsString(String input) {
            return input.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }
    
    /**
     * Get legal advice based on the user's query
     * 
     * @param query The legal question or situation from the user
     * @return Map containing the legal advice details
     */
    public static Map<String, Object> getLegalAdvice(String query) {
        if (query == null || query.trim().isEmpty()) {
            log("WARNING", "Empty query received");
            return getDefaultResponse();
        }
        
        if (USE_API) {
            try {
                return getAdviceFromAPI(query);
            } catch (Exception e) {
                log("SEVERE", "Error getting advice from API: " + e.getMessage());
                return getSimulatedResponse(query);
            }
        } else {
            return getSimulatedResponse(query);
        }
    }
    
    /**
     * Get advice by calling the OpenAI API
     * 
     * @param query The user's query
     * @return Map with legal advice details
     * @throws Exception If an error occurs during API communication
     */
    private static Map<String, Object> getAdviceFromAPI(String query) throws Exception {
        // Create legal advice prompt for Google AI
        String prompt = "Act as a legal assistant. Provide advice for this situation: " + query + 
            "\n\nFormat your response in JSON with the following structure:\n" +
            "{\n" +
            "  \"section\": \"The relevant law section name\",\n" +
            "  \"description\": \"A brief description of the applicable law\",\n" +
            "  \"steps\": [\"Step 1: First recommended step\", \"Step 2: Second recommended step\", \"Step 3: Third recommended step\"]\n" +
            "}\n\n" +
            "Make sure to include 3-5 specific steps the person should take.";
        
        // Create the request payload for Google AI
        String jsonPayload = "{\n" +
            "  \"contents\": [\n" +
            "    {\n" +
            "      \"parts\": [\n" +
            "        {\n" +
            "          \"text\": \"" + escapeJsonString(prompt) + "\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        log("INFO", "Sending request to Google AI API: " + API_URL);
        log("INFO", "Request payload: " + jsonPayload);
        
        // URL and connection setup - using java.net.URI to avoid deprecated constructor
        URL url = new URI(API_URL + "?key=" + API_KEY).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(60000); // 60 seconds timeout
        connection.setReadTimeout(60000);    // 60 seconds read timeout
        
        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Check for error response
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
            }
            log("SEVERE", "API Error Response [" + responseCode + "]: " + errorResponse.toString());
            throw new Exception("Google AI API request failed with response code: " + responseCode + 
                                " and message: " + errorResponse.toString());
        }
        
        // Read the response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        // Log the full response for debugging
        String responseStr = response.toString();
        log("INFO", "API Response: " + responseStr);
        
        // Parse Google AI response - updated to handle the specific response format from Gemini
        try {
            // First, check for the new response structure (candidates array)
            if (responseStr.contains("\"candidates\"")) {
                log("INFO", "Detected Gemini API response format with candidates array");
                
                // Extract the text content from the candidates array
                Pattern candidatesTextPattern = Pattern.compile("\"candidates\"\\s*:\\s*\\[\\s*\\{.*?\"parts\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"", Pattern.DOTALL);
                Matcher candidatesTextMatcher = candidatesTextPattern.matcher(responseStr);
                
                if (candidatesTextMatcher.find()) {
                    String content = candidatesTextMatcher.group(1)
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\");
                    
                    log("INFO", "Extracted content from candidates array: " + content);
                    
                    // Check if the content contains code blocks (```json)
                    if (content.contains("```json")) {
                        // Extract JSON from markdown code block
                        Pattern jsonBlockPattern = Pattern.compile("```json\\s*\\n(\\{.*?\\})\\s*```", Pattern.DOTALL);
                        Matcher jsonBlockMatcher = jsonBlockPattern.matcher(content);
                        
                        if (jsonBlockMatcher.find()) {
                            String jsonString = jsonBlockMatcher.group(1);
                            log("INFO", "Extracted JSON from code block: " + jsonString);
                            
                            // Parse the extracted JSON
                            Map<String, Object> result = parseJsonResponse(jsonString);
                            if (!result.isEmpty()) {
                                return result;
                            }
                        }
                    }
                    
                    // If no JSON code block found, try to extract JSON directly
                    Pattern jsonPattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
                    Matcher jsonMatcher = jsonPattern.matcher(content);
                    if (jsonMatcher.find()) {
                        String jsonString = jsonMatcher.group(0);
                        log("INFO", "Extracted JSON directly: " + jsonString);
                        
                        Map<String, Object> result = parseJsonResponse(jsonString);
                        if (!result.isEmpty()) {
                            return result;
                        }
                    }
                    
                    // If still no JSON found, try direct parsing approach
                    Map<String, Object> directResult = parseDirectResponse(content);
                    if (!directResult.isEmpty()) {
                        return directResult;
                    }
                }
            } else {
                // Try the original approach for backward compatibility
                // Extract the text content from the response
                Pattern textPattern = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
                Matcher textMatcher = textPattern.matcher(responseStr);
                
                if (textMatcher.find()) {
                    String content = textMatcher.group(1)
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"");
                    
                    log("INFO", "Extracted content: " + content);
                    
                    // Try to extract JSON from the content
                    Pattern jsonPattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
                    Matcher jsonMatcher = jsonPattern.matcher(content);
                    if (jsonMatcher.find()) {
                        String jsonString = jsonMatcher.group(0);
                        log("INFO", "Extracted JSON: " + jsonString);
                        
                        // Parse the JSON response
                        Map<String, Object> result = parseJsonResponse(jsonString);
                        if (!result.isEmpty()) {
                            return result;
                        }
                    } else {
                        // If no JSON object found, try to parse the content directly
                        Map<String, Object> result = parseDirectResponse(content);
                        if (!result.isEmpty()) {
                            return result;
                        }
                    }
                } else {
                    // Try an alternative approach
                    Pattern contentPattern = Pattern.compile("\"content\"\\s*:\\s*\\{.*?\"parts\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
                    Matcher contentMatcher = contentPattern.matcher(responseStr);
                    if (contentMatcher.find()) {
                        String content = contentMatcher.group(1)
                                        .replace("\\n", "\n")
                                        .replace("\\\"", "\"");
                        
                        log("INFO", "Extracted content (alternative method): " + content);
                        
                        // Try to extract JSON from the content
                        Pattern jsonPattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
                        Matcher jsonMatcher = jsonPattern.matcher(content);
                        if (jsonMatcher.find()) {
                            String jsonString = jsonMatcher.group(0);
                            log("INFO", "Extracted JSON: " + jsonString);
                            
                            // Parse the JSON response
                            Map<String, Object> result = parseJsonResponse(jsonString);
                            if (!result.isEmpty()) {
                                return result;
                            }
                        } else {
                            // If no JSON object found, try to parse the content directly
                            Map<String, Object> result = parseDirectResponse(content);
                            if (!result.isEmpty()) {
                                return result;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log("SEVERE", "Error parsing Google AI API response: " + e.getMessage());
            e.printStackTrace();
        }
        
        log("WARNING", "Failed to parse Google AI API response, returning default response.");
        return getDefaultResponse();
    }
    
    /**
     * Parse JSON response string into a map
     * 
     * @param jsonString The JSON string to parse
     * @return Map with parsed values
     */
    private static Map<String, Object> parseJsonResponse(String jsonString) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Extract section
            Pattern sectionPattern = Pattern.compile("\"section\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
            Matcher sectionMatcher = sectionPattern.matcher(jsonString);
            if (sectionMatcher.find()) {
                result.put("section", sectionMatcher.group(1));
            }
            
            // Extract description
            Pattern descPattern = Pattern.compile("\"description\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
            Matcher descMatcher = descPattern.matcher(jsonString);
            if (descMatcher.find()) {
                result.put("description", descMatcher.group(1));
            }
            
            // Extract steps
            Pattern stepsPattern = Pattern.compile("\"steps\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher stepsMatcher = stepsPattern.matcher(jsonString);
            if (stepsMatcher.find()) {
                String stepsJson = stepsMatcher.group(1);
                // Split by commas not inside quotes
                String[] stepsArray = stepsJson.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (int i = 0; i < stepsArray.length; i++) {
                    String step = stepsArray[i].trim().replaceAll("^\"(.*)\"$", "$1");
                    
                    // Remove markdown formatting (bold asterisks, etc.)
                    step = step.replaceAll("\\*\\*(.*?)\\*\\*", "$1"); // Remove bold formatting (**text**)
                    step = step.replaceAll("\\*(.*?)\\*", "$1");       // Remove italic formatting (*text*)
                    step = step.replaceAll("__(.*?)__", "$1");         // Remove underline formatting (__text__)
                    step = step.replaceAll("_(.*?)_", "$1");           // Remove italic formatting (_text_)
                    step = step.replaceAll("~~(.*?)~~", "$1");         // Remove strikethrough formatting (~~text~~)
                    
                    // Remove "Step X: " prefix if it exists
                    step = step.replaceAll("^Step \\d+:\\s*", "");
                    
                    stepsArray[i] = step;
                }
                result.put("steps", stepsArray);
            }
        } catch (Exception e) {
            log("WARNING", "Error parsing JSON response: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Attempt to parse a direct response that might not be in JSON format
     * 
     * @param content The response content
     * @return Map with parsed values
     */
    private static Map<String, Object> parseDirectResponse(String content) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Try to find a section title at the beginning or after newlines
            Pattern sectionPattern = Pattern.compile("(?:^|\\n)(?:Section|Law|Legal Area):\\s*(.*?)(?:\\n|$)");
            Matcher sectionMatcher = sectionPattern.matcher(content);
            if (sectionMatcher.find()) {
                result.put("section", sectionMatcher.group(1).trim());
            }
            
            // Try to find a description paragraph
            Pattern descPattern = Pattern.compile("(?:Description|Overview|Summary):\\s*(.*?)(?=\\n\\d\\.|\\n(?:Steps|Recommendations|Actions)|$)", Pattern.DOTALL);
            Matcher descMatcher = descPattern.matcher(content);
            if (descMatcher.find()) {
                result.put("description", descMatcher.group(1).trim());
            }
            
            // Try to find numbered steps
            Pattern stepsPattern = Pattern.compile("\\b(\\d+\\.\\s*.*?)(?=\\n\\d+\\.|$)", Pattern.DOTALL);
            Matcher stepsMatcher = stepsPattern.matcher(content);
            
            java.util.List<String> steps = new java.util.ArrayList<>();
            while (stepsMatcher.find()) {
                steps.add(stepsMatcher.group(1).trim());
            }
            
            if (!steps.isEmpty()) {
                result.put("steps", steps.toArray(new String[0]));
            }
        } catch (Exception e) {
            log("WARNING", "Error parsing direct response: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Provide a simulated response based on keywords in the query
     * Used when API is not available or for testing
     * 
     * @param query The user's query
     * @return Map with legal advice details
     */
    private static Map<String, Object> getSimulatedResponse(String query) {
        String queryLower = query.toLowerCase();
        Map<String, Object> response = new HashMap<>();
        
        // Check for common legal issues and provide appropriate responses
        if (queryLower.contains("car accident") || queryLower.contains("vehicle") || queryLower.contains("crash")) {
            response.put("section", "Personal Injury - Motor Vehicle Accidents");
            response.put("description", "Motor vehicle accidents fall under personal injury law, which allows injured parties to seek compensation for damages resulting from another's negligence. Most jurisdictions follow comparative or contributory negligence principles to determine liability.");
            response.put("steps", new String[] {
                "Report the accident to local police and your insurance company immediately",
                "Seek medical attention even if injuries seem minor",
                "Document the accident scene with photos and gather contact information from witnesses",
                "Keep records of all medical treatments and expenses",
                "Consult with a personal injury attorney before accepting any settlement offers"
            });
        } else if (queryLower.contains("divorce") || queryLower.contains("separation") || queryLower.contains("custody")) {
            response.put("section", "Family Law - Divorce and Custody");
            response.put("description", "Divorce laws govern the legal termination of marriage and address issues such as property division, spousal support, child custody, and child support. All states now allow no-fault divorce, though requirements vary by jurisdiction.");
            response.put("steps", new String[] {
                "Consult with a family law attorney to understand your rights and options",
                "Gather all financial documents including assets, debts, income, and expenses",
                "Consider mediation as a less adversarial approach to resolving disputes",
                "Develop a parenting plan if children are involved",
                "Protect your credit by separating financial accounts where possible"
            });
        } else if (queryLower.contains("theft") || queryLower.contains("stole") || queryLower.contains("stolen")) {
            response.put("section", "Criminal Law - Theft and Property Crimes");
            response.put("description", "Theft is defined as taking someone else's property without permission with the intent to permanently deprive them of it. Penalties vary based on the value of the property taken and can range from misdemeanors to felonies.");
            response.put("steps", new String[] {
                "Report the theft to police immediately and obtain a copy of the police report",
                "Make a detailed inventory of all stolen items with descriptions and approximate values",
                "Contact your insurance company if the items were insured",
                "Check local pawn shops or online marketplaces where stolen items might be sold",
                "Consider installing security measures to prevent future thefts"
            });
        } else if (queryLower.contains("landlord") || queryLower.contains("tenant") || queryLower.contains("rent") || queryLower.contains("lease")) {
            response.put("section", "Property Law - Landlord-Tenant Relations");
            response.put("description", "Landlord-tenant law governs the rental of commercial and residential property. It specifies the rights and duties of both landlords and tenants, including issues related to security deposits, repairs, evictions, and lease terminations.");
            response.put("steps", new String[] {
                "Review your lease agreement thoroughly to understand your rights and obligations",
                "Document all communication with your landlord in writing",
                "Research your local tenant rights laws as they vary significantly by location",
                "Address maintenance issues promptly by submitting formal written requests",
                "Consider consulting with a tenant advocacy organization or attorney if disputes escalate"
            });
        } else if (queryLower.contains("injured") || queryLower.contains("injury") || queryLower.contains("accident") || queryLower.contains("hurt")) {
            response.put("section", "Personal Injury Law");
            response.put("description", "Personal injury law allows individuals who have been harmed physically or psychologically due to another's negligence or intentional acts to seek compensation. Damages may include medical expenses, lost wages, pain and suffering, and more.");
            response.put("steps", new String[] {
                "Seek medical treatment immediately and follow all doctor recommendations",
                "Document your injuries with photographs and keep a journal of symptoms and limitations",
                "Preserve evidence from the incident that caused your injury",
                "Avoid discussing your case on social media or with insurance adjusters",
                "Consult with a personal injury attorney to evaluate your claim before any statute of limitations expires"
            });
        } else {
            // Default response for other types of queries
            return getDefaultResponse();
        }
        
        return response;
    }
    
    /**
     * Provide a default response when query cannot be processed
     * 
     * @return Map with default legal advice
     */
    private static Map<String, Object> getDefaultResponse() {
        Map<String, Object> defaultResponse = new HashMap<>();
        defaultResponse.put("section", "General Legal Guidance");
        defaultResponse.put("description", "Legal issues often require specialized advice tailored to your specific circumstances and jurisdiction. The information provided here is general in nature and not a substitute for qualified legal counsel.");
        defaultResponse.put("steps", new String[] {
            "Document all relevant information related to your legal concern",
            "Research applicable laws in your jurisdiction",
            "Consider consulting with a qualified attorney specializing in your issue",
            "Be aware of any time limitations (statutes of limitations) that may apply",
            "Maintain records of all communications and documents related to your case"
        });
        return defaultResponse;
    }
    
    /**
     * Escape special characters in the string for JSON
     * 
     * @param input The input string
     * @return Escaped string safe for JSON
     */
    private static String escapeJsonString(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * Log a message with a specific level
     * 
     * @param level The log level (INFO, WARNING, SEVERE)
     * @param message The log message
     */
    private static void log(String level, String message) {
        LogEntry logEntry = new LogEntry(level, message);
        consoleLogs.add(logEntry);
        if (consoleLogs.size() > MAX_LOGS) {
            consoleLogs.remove(0);
        }
        System.out.println(logEntry.toString());
    }
    
    /**
     * Get all console logs as a List
     * 
     * @return List of LogEntry objects
     */
    public static List<LogEntry> getConsoleLogs() {
        return new ArrayList<>(consoleLogs);
    }
    
    /**
     * Get console logs as JavaScript code to execute in browser
     * 
     * @return String of JavaScript code that outputs logs to browser console
     */
    public static String getConsoleLogsAsJavaScript() {
        StringBuilder script = new StringBuilder();
        script.append("<script>\n");
        script.append("console.clear();\n");
        script.append("console.log('=== Legal API Service Logs ===');\n");
        
        for (LogEntry entry : consoleLogs) {
            script.append(entry.toJavaScript()).append("\n");
        }
        
        script.append("</script>");
        return script.toString();
    }
    
    /**
     * Clear all console logs
     */
    public static void clearConsoleLogs() {
        consoleLogs.clear();
    }
}