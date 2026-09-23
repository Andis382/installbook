package io.github.andis382.installbook.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import io.github.andis382.installbook.config.AppProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reads structured data out of a photo or a transcript with Claude. Always an assist:
 * callers show the result for the person to confirm, and fall back to manual entry when
 * AI is not configured or the reading fails.
 */
@Service
public class AiExtractor {

    private static final Logger log = LoggerFactory.getLogger(AiExtractor.class);

    private final AppProperties props;
    private volatile AnthropicClient client;

    public AiExtractor(AppProperties props) {
        this.props = props;
    }

    public boolean enabled() {
        return !props.getAi().getApiKey().isBlank();
    }

    public <T> Optional<T> fromImage(byte[] image, String mediaType, String instruction, Class<T> type) {
        if (!enabled()) {
            return Optional.empty();
        }
        ImageBlockParam img = ImageBlockParam.builder()
            .source(Base64ImageSource.builder()
                .data(Base64.getEncoder().encodeToString(image))
                .mediaType(Base64ImageSource.MediaType.of(mediaType))
                .build())
            .build();
        List<ContentBlockParam> blocks = new ArrayList<>();
        blocks.add(ContentBlockParam.ofImage(img));
        blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(instruction).build()));
        return run(blocks, type);
    }

    public <T> Optional<T> fromText(String text, String instruction, Class<T> type) {
        if (!enabled()) {
            return Optional.empty();
        }
        List<ContentBlockParam> blocks = List.of(
            ContentBlockParam.ofText(TextBlockParam.builder().text(instruction + "\n\n<input>\n" + text + "\n</input>").build()));
        return run(blocks, type);
    }

    private <T> Optional<T> run(List<ContentBlockParam> blocks, Class<T> type) {
        try {
            StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(props.getAi().getModel())
                .maxTokens(8000L)
                .outputConfig(type)
                .addUserMessageOfBlockParams(blocks)
                .build();
            StructuredMessage<T> message = client().messages().create(params);
            return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .findFirst();
        } catch (RuntimeException e) {
            log.warn("AI extraction failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private AnthropicClient client() {
        AnthropicClient c = client;
        if (c == null) {
            synchronized (this) {
                c = client;
                if (c == null) {
                    c = AnthropicOkHttpClient.builder()
                        .apiKey(props.getAi().getApiKey())
                        .timeout(Duration.ofSeconds(90))
                        .maxRetries(1)
                        .build();
                    client = c;
                }
            }
        }
        return c;
    }
}
