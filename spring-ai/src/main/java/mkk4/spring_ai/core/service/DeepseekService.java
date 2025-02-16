package mkk4.spring_ai.core.service;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DeepseekService {
    @Autowired
    private OpenAiChatModel chatModel;

    @Value("classpath:/prompts/system-message.st")
    private Resource systemResource;

    @PostConstruct
    public void test() {
        String responseMessage = this.chatModel.call("Tell me your name");
        System.out.println("responseMessage = " + responseMessage);
    }
}
