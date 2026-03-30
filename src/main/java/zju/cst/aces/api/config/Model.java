package zju.cst.aces.api.config;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Model {
    GPT_3_5_TURBO("gpt-3.5-turbo", new ModelConfig.Builder()
            .withModelName("gpt-3.5-turbo")
            .withUrl("https://api.gptsapi.net/v1/chat/completions")
            .withContextLength(4096)
            .withTemperature(0.5)
            .withFrequencyPenalty(0)
            .withPresencePenalty(0)
            .build()),
    GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106", new ModelConfig.Builder()
            .withModelName("gpt-3.5-turbo-1106")
            .withUrl("https://api.gptsapi.net/v1/chat/completions")
            .withContextLength(16385)
            .withTemperature(0.5)
            .withFrequencyPenalty(0)
            .withPresencePenalty(0)
            .build()),
    GPT_4O("gpt-4o", new ModelConfig.Builder()
            .withModelName("gpt-4o")
            .withUrl("https://api.openai.com/v1/responses")
            .withContextLength(122880) //120k
            .withTemperature(0.5)
            .withFrequencyPenalty(0)
            .withPresencePenalty(0)
            .withApiType("RESPONSES")
            .build()),
    GPT_4O_MINI("gpt-4o-mini", new ModelConfig.Builder()
            .withModelName("gpt-4o-mini")
            .withUrl("https://api.openai.com/v1/responses")
            .withContextLength(122880) //120k
            .withTemperature(0.5)
            .withFrequencyPenalty(0)
            .withPresencePenalty(0)
            .withApiType("RESPONSES")
            .build()),
    GPT_4O_MINI_0718("gpt-4o-mini-2024-07-18", new ModelConfig.Builder()
            .withModelName("gpt-4o-mini-2024-07-18")
            .withUrl("https://api.openai.com/v1/responses")
            .withContextLength(10922)
            .withTemperature(0.5)
            .withFrequencyPenalty(0)
            .withPresencePenalty(0)
            .withApiType("RESPONSES")
            .build()),
    GPT_5_4("gpt-5.4", new ModelConfig.Builder()
            .withModelName("gpt-5.4")
            .withUrl("https://api.openai.com/v1/responses")
            .withContextLength(122880) //120k
            .withTemperature(0.5)
            .withFrequencyPenalty(0)
            .withPresencePenalty(0)
            .withApiType("RESPONSES")
            .build()),
    CLAUDE_HAIKU_4_5("claude-haiku-4-5-20251001", new ModelConfig.Builder()
            .withModelName("claude-haiku-4-5-20251001")
            .withUrl("https://api.anthropic.com/v1/messages")
            .withContextLength(200000)
            .withTemperature(0.5)
            .withApiType("CLAUDE")
            .build()),
    CLAUDE_SONNET_4_6("claude-sonnet-4-6", new ModelConfig.Builder()
            .withModelName("claude-sonnet-4-6")
            .withUrl("https://api.anthropic.com/v1/messages")
            .withContextLength(200000)
            .withTemperature(0.5)
            .withApiType("CLAUDE")
            .build()),
    CLAUDE_OPUS_4_6("claude-opus-4-6", new ModelConfig.Builder()
            .withModelName("claude-opus-4-6")
            .withUrl("https://api.anthropic.com/v1/messages")
            .withContextLength(200000)
            .withTemperature(0.5)
            .withApiType("CLAUDE")
            .build()),
    CODE_LLAMA("code-llama", new ModelConfig.Builder()
            .withModelName("code-llama")
            .withUrl(null)
            .withContextLength(16385)
            .withTemperature(0.5)
            .withFrequencyPenalty(0)
            .withPresencePenalty(0)
            .build()),
    // 添加更多模型
    QWEN3_14B_INSTRUCT("OpenPipe/Qwen3-14B-Instruct", new ModelConfig.Builder()
            .withModelName("OpenPipe/Qwen3-14B-Instruct")
            .withUrl("http://127.0.0.1:8000/v1/chat/completions")
            .withContextLength(122880)
            .withTemperature(0.5)
            .withFrequencyPenalty(0)
            .withPresencePenalty(0)
            .build()),
    QWEN2_5_14B_INSTRUCT("Qwen/Qwen2.5-14B-Instruct", new ModelConfig.Builder()
            .withModelName("Qwen/Qwen2.5-14B-Instruct")
            .withUrl("http://127.0.0.1:8000/v1/chat/completions")
            .withContextLength(32768)
            .withTemperature(0.5)
            .withFrequencyPenalty(0)
            .withPresencePenalty(0)
            .build());
    

    private final String modelName;
    private final ModelConfig defaultConfig;

    Model(String modelName, ModelConfig defaultConfig) {
        this.modelName = modelName;
        this.defaultConfig = defaultConfig;
    }

    public String getModelName() {
        return modelName;
    }

    public ModelConfig getDefaultConfig() {
        return defaultConfig;
    }

    public static Model fromString(String modelName) {
        for (Model model : Model.values()) {
            if (model.getModelName().equalsIgnoreCase(modelName)) {
                return model;
            }
        }
        throw new IllegalArgumentException("No Model with name " + modelName +
                "\nSupport models: " + Arrays.stream(Model.values()).map(Model::getModelName).collect(Collectors.joining(", ")));
    }
}
