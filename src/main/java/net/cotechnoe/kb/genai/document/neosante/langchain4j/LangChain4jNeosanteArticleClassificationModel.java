package net.cotechnoe.kb.genai.document.neosante.langchain4j;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import net.cotechnoe.kb.genai.document.neosante.ArticleExtractionException;
import net.cotechnoe.kb.genai.document.neosante.llm.NeosanteArticleClassificationModel;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/** LangChain4j adapter for the Néosanté OpenAI-compatible endpoint configured outside source control. */
public final class LangChain4jNeosanteArticleClassificationModel implements NeosanteArticleClassificationModel {

    private final ChatModel model;

    public LangChain4jNeosanteArticleClassificationModel(ChatModel model) {
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    public static LangChain4jNeosanteArticleClassificationModel from(Properties properties) {
        String endpoint = required(properties, "KB_GENAI_BUILDER_LLM_URL");
        String apiKey = required(properties, "KB_GENAI_BUILDER_LLM_API_KEY");
        URI uri = URI.create(endpoint);
        return new LangChain4jNeosanteArticleClassificationModel(AzureOpenAiChatModel.builder()
                .endpoint(uri.getScheme() + "://" + uri.getAuthority())
                .apiKey(apiKey)
                .deploymentName(deployment(uri.getPath()))
                .serviceVersion(apiVersion(uri))
                .temperature(0.0)
                .logRequestsAndResponses(false)
                .build());
    }

    @Override
    public String classify(String prompt) {
        return model.chat(prompt);
    }

    private static String required(Properties properties, String key) {
        String value = Objects.requireNonNull(properties, "properties must not be null").getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ArticleExtractionException("Missing LLM configuration property: " + key);
        }
        return value.strip();
    }

    private static String deployment(String path) {
        String marker = "/openai/deployments/";
        int start = path.indexOf(marker);
        if (start < 0) {
            throw new ArticleExtractionException("LLM URL must target an Azure OpenAI deployment");
        }
        String remaining = path.substring(start + marker.length());
        int end = remaining.indexOf('/');
        if (end < 1) {
            throw new ArticleExtractionException("LLM URL does not contain a deployment name");
        }
        return remaining.substring(0, end);
    }

    private static String apiVersion(URI uri) {
        String query = uri.getRawQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2 && "api-version".equals(URLDecoder.decode(parts[0], StandardCharsets.UTF_8))) {
                    String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8).strip();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        throw new ArticleExtractionException("LLM URL must contain the Azure OpenAI api-version query parameter");
    }
}
