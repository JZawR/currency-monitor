package zawr.currencymonitor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class CurrencyMonitorApplicationTests {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    static {
        mongo.start();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("app.telegram.bot-token", () -> "test_token");
        registry.add("app.telegram.chat-id", () -> "123456");
    }

    @Test
    void contextLoads() {

    }
}