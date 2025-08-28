package no.idporten.eudiw.rp.register.lookup.web;

import no.idporten.eudiw.rp.register.lookup.service.RelyingPartiesService;
import no.idporten.eudiw.rp.register.lookup.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.register.lookup.web.controllers.SearchController;
import no.idporten.eudiw.rp.register.lookup.web.form.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("When using the lookup web interface")
@SpringBootTest
public class LookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused") // since its use in when() is not recognized as a use.
    @MockitoBean
    private RelyingPartiesService lookupService;

    @BeforeEach
    void setupMockLookupService() {
        when(lookupService.search(any()))
            .thenAnswer(invocationOnMock -> {
                SearchRelyingPartyResource searchResource =
                    invocationOnMock.getArgument(0, SearchRelyingPartyResource.class);
                return new RelyingPartiesResource(
                    List.of(ResourceGenerator.generateRelyingPartyResource()
                                             .withName(searchResource.searchTerm())));
            });
    }

    @Nested
    @DisplayName("when making GET requests to the search page")
    class GetTests {
        @Test
        void testModelGetsEmptySearchFormOnGet() throws Exception {
            mockMvc.perform(get("/"))
                   .andExpect(status().isOk())
                   .andExpect(view().name("search_view"))
                   .andExpect(model().attribute(SearchController.searchFormAttrId,
                                                SearchForm.empty()));
        }
    }
}
