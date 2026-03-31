package zju.cst.aces.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.api.config.ModelConfig;
import zju.cst.aces.dto.ChatChoice;
import zju.cst.aces.dto.ChatMessage;
import zju.cst.aces.dto.ChatResponse;
import zju.cst.aces.dto.ChatUsage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AskGPT {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public Config config;

    public AskGPT(Config config) {
        this.config = config;
    }

    public ChatResponse askChatGPT(List<ChatMessage> chatMessages) {
        String apiKey = config.getRandomKey();
        int maxTry = 5;
        while (maxTry > 0) {
            Response response = null;
            try {
                ModelConfig modelConfig = config.getModel().getDefaultConfig();
                String apiType = modelConfig.getApiType() != null ? modelConfig.getApiType() : "CHAT_COMPLETIONS";

                String jsonPayload;
                if ("CLAUDE".equals(apiType)) {
                    jsonPayload = buildClaudePayload(chatMessages, modelConfig);
                } else if ("RESPONSES".equals(apiType)) {
                    jsonPayload = buildResponsesPayload(chatMessages, modelConfig);
                } else {
                    jsonPayload = buildChatCompletionsPayload(chatMessages, modelConfig);
                }

                Request.Builder requestBuilder = new Request.Builder()
                        .url(modelConfig.getUrl())
                        .post(RequestBody.create(MEDIA_TYPE, jsonPayload))
                        .addHeader("Content-Type", "application/json");

                if ("CLAUDE".equals(apiType)) {
                    requestBuilder.addHeader("x-api-key", apiKey)
                                  .addHeader("anthropic-version", "2023-06-01");
                } else {
                    requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
                }

                response = config.getClient().newCall(requestBuilder.build()).execute();
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "(no body)";
                    response.close();
                    throw new IOException("Unexpected code " + response + " | body: " + errorBody);
                }
                try {
                    Thread.sleep(config.sleepTime);
                } catch (InterruptedException ie) {
                    throw new RuntimeException("In AskGPT.askChatGPT: " + ie);
                }
                if (response.body() == null) throw new IOException("Response body is null.");
                String responseBody = response.body().string();
                response.close();

                ChatResponse chatResponse;
                if ("CLAUDE".equals(apiType)) {
                    chatResponse = parseClaudeResponse(responseBody);
                } else if ("RESPONSES".equals(apiType)) {
                    chatResponse = parseResponsesApiResponse(responseBody);
                } else {
                    chatResponse = GSON.fromJson(responseBody, ChatResponse.class);
                }
                return chatResponse;
            } catch (IOException e) {
                if (response != null) {
                    response.close();
                }
                config.getLogger().error("In AskGPT.askChatGPT: " + e);
                maxTry--;
            }
        }
        config.getLogger().debug("AskGPT: Failed to get response\n");
        return null;
    }

    // --- Request builders ---

    private String buildChatCompletionsPayload(List<ChatMessage> chatMessages, ModelConfig modelConfig) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messages", chatMessages);
        payload.put("model", modelConfig.getModelName());
        payload.put("temperature", config.getTemperature());
        payload.put("frequency_penalty", config.getFrequencyPenalty());
        payload.put("presence_penalty", config.getPresencePenalty());
        payload.put("max_completion_tokens", config.getMaxResponseTokens());
        return GSON.toJson(payload);
    }

    private String buildResponsesPayload(List<ChatMessage> chatMessages, ModelConfig modelConfig) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("input", chatMessages);
        payload.put("model", modelConfig.getModelName());
        payload.put("temperature", config.getTemperature());
        payload.put("max_output_tokens", config.getMaxResponseTokens());
        return GSON.toJson(payload);
    }

    private String buildClaudePayload(List<ChatMessage> chatMessages, ModelConfig modelConfig) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", modelConfig.getModelName());
        payload.put("max_tokens", config.getMaxResponseTokens());

        // Extract system message (Claude places it at top level)
        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage msg : chatMessages) {
            if ("system".equals(msg.getRole())) {
                payload.put("system", msg.getContent());
            } else {
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                messages.add(m);
            }
        }
        payload.put("messages", messages);
        payload.put("temperature", config.getTemperature());
        return GSON.toJson(payload);
    }

    // --- Response parsers ---

    /**
     * Parses OpenAI Responses API response:
     * { "output": [{ "content": [{ "type": "output_text", "text": "..." }] }],
     *   "usage": { "input_tokens": N, "output_tokens": M, "total_tokens": T } }
     */
    private ChatResponse parseResponsesApiResponse(String responseBody) {
        JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
        ChatResponse chatResponse = new ChatResponse();
        if (json.has("id")) chatResponse.setId(json.get("id").getAsString());
        if (json.has("model")) chatResponse.setModel(json.get("model").getAsString());

        String text = "";
        if (json.has("output")) {
            JsonArray output = json.getAsJsonArray("output");
            if (output.size() > 0) {
                JsonObject firstOutput = output.get(0).getAsJsonObject();
                if (firstOutput.has("content")) {
                    JsonArray content = firstOutput.getAsJsonArray("content");
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement el : content) {
                        JsonObject contentItem = el.getAsJsonObject();
                        if (contentItem.has("text")) {
                            sb.append(contentItem.get("text").getAsString());
                        }
                    }
                    text = sb.toString();
                }
            }
        }

        ChatMessage assistantMessage = ChatMessage.ofAssistant(text);
        ChatChoice choice = new ChatChoice();
        choice.setIndex(0);
        choice.setMessage(assistantMessage);
        chatResponse.setChoices(Collections.singletonList(choice));

        if (json.has("usage")) {
            chatResponse.setUsage(parseResponsesUsage(json.getAsJsonObject("usage")));
        }
        return chatResponse;
    }

    /**
     * Parses Anthropic Claude API response:
     * { "content": [{ "type": "text", "text": "..." }],
     *   "usage": { "input_tokens": N, "output_tokens": M } }
     */
    private ChatResponse parseClaudeResponse(String responseBody) {
        JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
        ChatResponse chatResponse = new ChatResponse();
        if (json.has("id")) chatResponse.setId(json.get("id").getAsString());
        if (json.has("model")) chatResponse.setModel(json.get("model").getAsString());

        String text = "";
        if (json.has("content")) {
            JsonArray content = json.getAsJsonArray("content");
            StringBuilder sb = new StringBuilder();
            for (JsonElement el : content) {
                JsonObject contentItem = el.getAsJsonObject();
                if (contentItem.has("text")) {
                    sb.append(contentItem.get("text").getAsString());
                }
            }
            text = sb.toString();
        }

        ChatMessage assistantMessage = ChatMessage.ofAssistant(text);
        ChatChoice choice = new ChatChoice();
        choice.setIndex(0);
        choice.setMessage(assistantMessage);
        chatResponse.setChoices(Collections.singletonList(choice));

        if (json.has("usage")) {
            chatResponse.setUsage(parseResponsesUsage(json.getAsJsonObject("usage")));
        }
        return chatResponse;
    }

    /** Maps input_tokens/output_tokens (Responses API & Claude) to ChatUsage. */
    private ChatUsage parseResponsesUsage(JsonObject usageJson) {
        ChatUsage usage = new ChatUsage();
        if (usageJson.has("input_tokens")) {
            usage.setPromptTokens(usageJson.get("input_tokens").getAsInt());
        }
        if (usageJson.has("output_tokens")) {
            usage.setCompletionTokens(usageJson.get("output_tokens").getAsInt());
        }
        if (usageJson.has("total_tokens")) {
            usage.setTotalTokens(usageJson.get("total_tokens").getAsInt());
        } else if (usage.getPromptTokens() != null && usage.getCompletionTokens() != null) {
            usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
        }
        return usage;
    }
}
