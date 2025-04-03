package no.idporten.eudiw.rp.register.lookup.web;

import no.idporten.eudiw.rp.register.lookup.service.LookupService;
import no.idporten.eudiw.rp.register.lookup.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.register.lookup.web.resource.RelyingPartiesResource;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchForm;
import no.idporten.eudiw.rp.register.lookup.web.resource.SearchRelyingPartyResource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("When using the lookup web interface")
@SpringBootTest
public class LookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LookupService lookupService;

    @BeforeEach
    void setupMockLookupService() {
        when(lookupService.search(any()))
            .thenAnswer(invocationOnMock -> {
                SearchRelyingPartyResource searchResource =
                    invocationOnMock.getArgument(0, SearchRelyingPartyResource.class);
                return new RelyingPartiesResource(
                    List.of(ResourceGenerator.generateRelyingPartyResource()
                                             .withOrgno(searchResource.orgno())));
            });
    }

    @Nested
    @DisplayName("when making POST requests to the search page")
    class GetTests {
        @Test
        void testModelGetsEmptySearchFormOnGet() throws Exception {
            mockMvc.perform(get("/"))
                   .andExpect(status().isOk())
                   .andExpect(view().name("search"))
                   .andExpect(model().attribute(LookupController.searchFormAttrId,
                                                SearchForm.empty()));
        }
    }

    @Nested
    @DisplayName("when making POST requests to the search page")
    class PostTests {
        @Test
        void testServiceCalledWithCorrectSearchForm() throws Exception {
            SearchForm searchForm = ResourceGenerator.generateSearchForm();
            mockMvc.perform(
                       post("/")
                           .param("orgno", searchForm.orgno())
                           .param("name", searchForm.name())
                           .param("searchSector", searchForm.searchSector().toString())
                           .param("includeInactive", Boolean.valueOf(searchForm.includeInactive()).toString()))
                   .andExpect(status().isOk())
                   .andExpect(view().name("search"))
                   .andExpect(content().string(containsString(searchForm.orgno())))
                   .andExpect(content().string(containsString(searchForm.name())));
        }
    }
}
