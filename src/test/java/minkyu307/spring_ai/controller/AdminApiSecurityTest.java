package minkyu307.spring_ai.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import minkyu307.spring_ai.config.SecurityConfig;
import minkyu307.spring_ai.config.TraceIdFilter;
import minkyu307.spring_ai.error.ApiErrorFactory;
import minkyu307.spring_ai.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = AdminApiController.class)
@Import({SecurityConfig.class, TraceIdFilter.class, ApiErrorFactory.class})
class AdminApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    void nonAdminUserCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user("member").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminUserCanAccessAdminApi() throws Exception {
        when(adminUserService.findAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());

        verify(adminUserService).findAllUsers();
    }
}
