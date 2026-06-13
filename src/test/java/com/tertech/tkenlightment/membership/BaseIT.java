package com.tertech.tkenlightment.membership;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestTokens.class})
@ActiveProfiles("test")
public abstract class BaseIT {

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected TestTokens tokens;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected JavaMailSender mailSender;

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected <T> T readBody(MvcTestResult result, Class<T> type) {
        try {
            return objectMapper.readValue(result.getResponse().getContentAsString(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse response body", e);
        }
    }
}
