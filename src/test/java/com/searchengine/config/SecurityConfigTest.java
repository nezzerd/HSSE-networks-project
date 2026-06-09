package com.searchengine.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test-secret"
})
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void search_isPublic() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "java"))
            .andExpect(status().isOk());
    }

    @Test
    void crawlStatus_withoutCredentials_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/crawl/status"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void crawlStatus_withAdminCredentials_isOk() throws Exception {
        mockMvc.perform(get("/api/crawl/status").with(httpBasic("admin", "test-secret")))
            .andExpect(status().isOk());
    }

    @Test
    void crawlStatus_withWrongPassword_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/crawl/status").with(httpBasic("admin", "wrong")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void indexStatus_withoutCredentials_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/index/status"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void indexStatus_withAdminCredentials_isOk() throws Exception {
        mockMvc.perform(get("/api/index/status").with(httpBasic("admin", "test-secret")))
            .andExpect(status().isOk());
    }
}
