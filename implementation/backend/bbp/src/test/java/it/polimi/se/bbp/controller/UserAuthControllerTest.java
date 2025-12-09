package it.polimi.se.bbp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.se.bbp.dto.request.UserLoginRequest;
import it.polimi.se.bbp.dto.request.UserRegisterRequest;
import it.polimi.se.bbp.dto.response.UserAuthResponse;
import it.polimi.se.bbp.service.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserAuthService userAuthService;

    @Test
    void shouldRegisterUser() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .name("Test")
                .surname("User")
                .username("testuser")
                .email("test@test.com")
                .password("password123")
                .build();

        UserAuthResponse response = new UserAuthResponse("jwt-token", 1L);

        when(userAuthService.register(any(UserRegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void shouldLoginUser() throws Exception {
        UserLoginRequest request = new UserLoginRequest("test@test.com", "password123");
        UserAuthResponse response = new UserAuthResponse("jwt-token", 1L);

        when(userAuthService.login(any(UserLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void shouldReturnBadRequestForInvalidRegister() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("invalid-email")
                .password("short")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}