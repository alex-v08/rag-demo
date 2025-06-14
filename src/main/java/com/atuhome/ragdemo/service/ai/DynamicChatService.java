package com.atuhome.ragdemo.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicChatService {
    
    private final ModelManagementService modelManagementService;
    private final ChatModel chatModel;
    
    @Autowired
    private ChatClient.Builder chatClientBuilder;
    
    private ChatClient chatClient;
    
    public void updateChatClient() {
        // Por ahora, simplemente recreamos el cliente
        // En una implementación más compleja, podríamos cambiar el modelo dinámicamente
        this.chatClient = null;
    }
    
    public String chat(String prompt) {
        if (chatClient == null) {
            chatClient = chatClientBuilder.build();
        }
        
        String currentModel = modelManagementService.getCurrentChatModel();
        log.debug("Usando modelo: {} para generar respuesta", currentModel);
        
        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}