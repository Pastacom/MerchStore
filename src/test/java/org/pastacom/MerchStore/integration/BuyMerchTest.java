package org.pastacom.MerchStore.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pastacom.MerchStore.controller.GlobalExceptionHandler;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@Sql(scripts = {"classpath:sql/cleanup.sql", "classpath:sql/buy_merch_setup.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BuyMerchTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CacheManager cacheManager;

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

    @Test
    @WithMockUser(username = "testUser")
    public void testBuyMerch() throws Exception {
        String itemName = "hoody";
        int itemPrice = 300;
        User userBefore = userRepository.findByUsername("testUser");

        Cache cache = cacheManager.getCache("users");
        assertThat(cache.get(userBefore.getUsername())).isNotNull();

        int initialCoins = userBefore.getBalance();

        mockMvc.perform(get("/api/buy/" + itemName)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());

        cache = cacheManager.getCache("users");
        assertThat(cache.get(userBefore.getUsername())).isNull();

        cache = cacheManager.getCache("products");
        assertThat(cache.get(itemName)).isNotNull();

        User userAfter = userRepository.findByUsername("testUser");

        assertThat(userAfter.getBalance()).isEqualTo(initialCoins - itemPrice);
        assertThat(userRepository.getInventory(userAfter)).anyMatch(item -> item.getType().equals("hoody"));

        mockMvc.perform(get("/api/info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(userAfter.getBalance()))
                .andExpect(jsonPath("$.inventory[0].type").value("hoody"));

        cache = cacheManager.getCache("inventory");
        assertThat(cache.get(userBefore.getId())).isNotNull();
    }
}
