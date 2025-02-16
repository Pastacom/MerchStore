package org.pastacom.MerchStore.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pastacom.MerchStore.controller.GlobalExceptionHandler;
import org.pastacom.MerchStore.dto.SendCoinRequest;
import org.pastacom.MerchStore.model.User;
import org.pastacom.MerchStore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.github.cdimascio.dotenv.Dotenv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@Sql(scripts = {"classpath:sql/cleanup.sql", "classpath:sql/transfer_coins_setup.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class TransferCoinsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CacheManager cacheManager;
    private ObjectMapper objectMapper;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis").withExposedPorts(6379);

    @BeforeAll
    public static void setup() {
        Dotenv dotenv = Dotenv.load();
        System.setProperty("JWT_SECRET_KEY", dotenv.get("JWT_SECRET_KEY"));
        System.setProperty("JDBC_URL_FROM_CONTAINER", postgres.getJdbcUrl());
        System.setProperty("spring.data.redis.host",  redis.getHost());
        System.setProperty("spring.data.redis.port", redis.getFirstMappedPort().toString());
        postgres.start();
        redis.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
        redis.stop();
    }

    @BeforeEach
    public void setObjectMapper() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(username = "senderUser")
    public void testTransferCoins() throws Exception {
        User sender = userRepository.findByUsername("senderUser");
        User receiver = userRepository.findByUsername("receiverUser");

        int initialSenderCoins = sender.getBalance();
        int initialReceiverCoins = receiver.getBalance();
        int transferAmount = 200;

        Cache senderCache = cacheManager.getCache("users");
        Cache receiverCache = cacheManager.getCache("users");

        assertThat(senderCache.get(sender.getUsername())).isNotNull();
        assertThat(receiverCache.get(receiver.getUsername())).isNotNull();

        SendCoinRequest request = new SendCoinRequest("receiverUser", transferAmount);

        mockMvc.perform(post("/api/sendCoin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

        senderCache = cacheManager.getCache("users");
        receiverCache = cacheManager.getCache("users");

        assertThat(senderCache.get(sender.getUsername())).isNull();
        assertThat(receiverCache.get(receiver.getUsername())).isNull();

        User updatedSender = userRepository.findByUsername("senderUser");
        User updatedReceiver = userRepository.findByUsername("receiverUser");

        assertThat(updatedSender.getBalance()).isEqualTo(initialSenderCoins - transferAmount);
        assertThat(updatedReceiver.getBalance()).isEqualTo(initialReceiverCoins + transferAmount);

        mockMvc.perform(get("/api/info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(updatedSender.getBalance()))
                .andExpect(jsonPath("$.coinHistory.sent[0].toUser").value(receiver.getUsername()))
                .andExpect(jsonPath("$.coinHistory.sent[0].amount").value(transferAmount));

        Cache transactionsCache = cacheManager.getCache("sentTransactions");
        assertThat(transactionsCache.get(sender.getId())).isNotNull();
    }
}
