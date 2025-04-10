package com.example.demo;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication
@RegisterReflectionForBinding(DogAdoptionSuggestion.class)
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    McpSyncClient mcpSyncClient() {
        var mcp = McpClient
                .sync(new HttpClientSseClientTransport("http://localhost:8081"))
                .build();
        mcp.initialize();
        return mcp;
    }
}

@RestController
class RagController {

    private final Executor executor = Executors.newFixedThreadPool(1);

    private final ChatClient ai;

    RagController(ChatClient.Builder ai, VectorStore vectorStore,
                  McpSyncClient client,
                  JdbcClient db, DogRepository repository) {

        this.ai = ai
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .defaultTools(new SyncMcpToolCallbackProvider(client))
                .build();
        this.executor.execute(() -> {

            if (db
                    .sql("select count(*) from vector_store")
                    .query(Integer.class)
                    .single()
                    .equals(0)
            ) {
                repository.findAll().forEach(d -> {
                    var dogument = new Document("id: %s, name: %s, description: %s"
                            .formatted(d.id(), d.name(), d.description()));
                    vectorStore.add(List.of(dogument));
                });
            }
        });
    }

    @GetMapping("/content")
    String ragContent(@RequestParam String question) {
        return this.responseSpec(question).content();
    }

    @GetMapping("/entity")
    DogAdoptionSuggestion ragEntity(@RequestParam String question) {
        return this.responseSpec(question).entity(DogAdoptionSuggestion.class);
    }

    private ChatClient.CallResponseSpec responseSpec(String question) {
        var system = """
                You are an AI powered assistant to help people adopt a dog from the adoption\s
                agency named Pooch Palace with locations in Antwerp, Seoul, Tokyo, Singapore, Paris,\s
                Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs available\s
                will be presented below. If there is no information, then return a polite response suggesting we\s
                don't have any dogs available.
                """;
        return this.ai
                .prompt()
                .system(system)
                .user(question)
                .call();
    }
}

record DogAdoptionSuggestion(int id, String name) {
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

record Dog(@Id int id, String name, String owner, String description) {
}