package no.idporten.eudiw.rp.admin.web;

import no.idporten.eudiw.rp.admin.data.RelyingPartyEntitlement;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import no.idporten.eudiw.rp.admin.testdata.ResourceGenerator;
import no.idporten.eudiw.rp.admin.testdata.TestDataGenerator;
import no.idporten.eudiw.rp.admin.web.controllers.CreateController;
import no.idporten.eudiw.rp.admin.web.form.RelyingPartyCreateForm;
import no.idporten.eudiw.rp.admin.web.resource.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local-test")
@DisplayName("When using the RP registration controller")
@AutoConfigureMockMvc
public class CreateControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private RelyingPartiesService mockRpService;

    @Nested
    @DisplayName("When GET'ing the create endpoint")
    class CreateEndpointGetTests {
        @Test
        @DisplayName("then the correct view with the expected create form is loaded")
        void testCorrectViewAndModelAttributes() throws Exception {
            RelyingPartyCreateForm createForm = new RelyingPartyCreateForm();

            mockMvc.perform(get("/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("create_form_view"))
                .andExpect(model().attribute(CreateController.createFormAttrId, createForm));
        }
    }

    @Nested
    @DisplayName("When POST'ing create forms to the /create endpoint")
    class CreateEndpointPostTests {
        @Test
        @DisplayName("then form accepted if well-formed, and correct services called, view, and model")
        void testCreateFormAcceptedIfWellFormed() throws Exception {
            RelyingPartyCreateForm createForm = new RelyingPartyCreateForm();
            createForm.setName(ResourceGenerator.generateName());
            createForm.setPublicSector(ResourceGenerator.generateBoolean());
            createForm.setOrgno(TestDataGenerator.generateValidOrgno());
            RelyingPartyEntitlement entitlement = RelyingPartyEntitlement.SERVICE_PROVIDER;
            createForm.setEntitlements(List.of(entitlement.toFormField()));

            CreateRelyingPartyResource expectedCreateResource = createForm.toResource();
            RelyingPartyResource rpResource = new RelyingPartyResource(
                UUID.randomUUID(),
                createForm.getOrgno(),
                createForm.getName(),
                createForm.isPublicSector(),
                List.of(entitlement.toResource()),
                Collections.emptyList(),
                1,
                1,
                true
            );

            when(mockRpService.create(expectedCreateResource)).thenReturn(rpResource);
            mockMvc.perform(post("/create")
                    .formField("orgno", createForm.getOrgno())
                    .formField("name", createForm.getName())
                    .formField("publicSector", Boolean.toString(createForm.isPublicSector()))
                    .formField("entitlements", entitlement.getUri()))
                .andExpect(status().is3xxRedirection());

            verify(mockRpService).create(expectedCreateResource);
        }
    }
}
