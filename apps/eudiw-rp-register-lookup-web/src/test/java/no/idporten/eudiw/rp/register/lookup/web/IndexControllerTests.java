package no.idporten.eudiw.rp.register.lookup.web;

import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.service.credentialsservice.CredentialsService;
import no.idporten.eudiw.rp.register.lookup.web.controllers.IndexController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the index controller")
@AutoConfigureMockMvc
public class IndexControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService lookupService;

    @SuppressWarnings("unused")
    @MockitoBean
    private CredentialsService mockCredentialsService;

    @Nested
    @DisplayName("when GET'ing the / endpoint")
    class IndexEndpointGetTests {

        @Test
        public void testIndexRendersWithCounts() throws Exception {
            when(lookupService.count()).thenReturn(724L);
            when(mockCredentialsService.count()).thenReturn(18L);

            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index_view"))
                .andExpect(model().attribute(IndexController.relyingPartyCountAttrId, 724L))
                .andExpect(model().attribute(IndexController.credentialCountAttrId, 18L));
        }

        @Test
        public void testIndexRendersWithoutCountsWhenServicesFail() throws Exception {
            when(lookupService.count())
                .thenThrow(new RuntimeException("register down"));
            when(mockCredentialsService.count())
                .thenThrow(new RuntimeException("credential registry down"));

            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index_view"))
                .andExpect(result -> {
                    var model = result.getModelAndView().getModel();
                    if (model.get(IndexController.relyingPartyCountAttrId) != null
                        || model.get(IndexController.credentialCountAttrId) != null) {
                        throw new AssertionError("Expected null counts when services fail");
                    }
                })
                .andExpect(content().string(not(containsString("ds-badge"))));
        }
    }
}
