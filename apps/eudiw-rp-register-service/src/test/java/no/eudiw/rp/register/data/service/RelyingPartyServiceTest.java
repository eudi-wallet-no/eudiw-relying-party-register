package no.eudiw.rp.register.data.service;

import no.eudiw.rp.register.api.resource.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static no.eudiw.rp.register.testdata.ResourceGenerator.generateCreateRelyingPartyResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@DisplayName("When using the Relying Party Service")
@ActiveProfiles("test")
public class RelyingPartyServiceTest {

    @Autowired
    private RelyingPartyService relyingPartyService;

    @DisplayName("When editing a relying party")
    @Nested
    class EditTests {

        @Test
        void testComplexEditRelyingParty() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            RelyingPartyResource response = relyingPartyService.createRelyingParty(resource);
            assertNotNull(response);

            RelyingPartyResource editResponse1 = relyingPartyService.updateRelyingParty(
                    response.getId(),
                    new EditRelyingPartyResource(
                            response.getName(),
                            response.isPublicSector(),
                            List.of(new RelyingPartyEntitlementResource("value")),
                            List.of(new RelyingPartyEaaResource("asd", "asd"), new RelyingPartyEaaResource("vsd", "ert")),
                            true
                    )
            );
            assertNotNull(editResponse1);
            assertEquals(2, editResponse1.getRelyingPartyEaas().size());

            RelyingPartyResource editResponse2 = relyingPartyService.updateRelyingParty(
                    response.getId(),
                    new EditRelyingPartyResource(
                            response.getName(),
                            response.isPublicSector(),
                            List.of(new RelyingPartyEntitlementResource("value")),
                            List.of(new RelyingPartyEaaResource("asd", "asd"), new RelyingPartyEaaResource("vsd", "ert")),
                            true
                    )
            );
            assertNotNull(editResponse2);
            assertEquals(2, editResponse2.getRelyingPartyEaas().size());

            RelyingPartyResource editResponse3 = relyingPartyService.updateRelyingParty(
                    response.getId(),
                    new EditRelyingPartyResource(
                            response.getName(),
                            response.isPublicSector(),
                            List.of(new RelyingPartyEntitlementResource("value")),
                            new ArrayList<>(),
                            true
                    )
            );
            assertNotNull(editResponse3);
            assertEquals(0, editResponse3.getRelyingPartyEaas().size());

            RelyingPartyResource getResult = relyingPartyService.findRelyingParty(response.getId());
            assertNotNull(getResult);
            assertEquals(0, getResult.getRelyingPartyEaas().size());
        }

        @Test
        void testSimpleEditRelyingParty() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            RelyingPartyResource response = relyingPartyService.createRelyingParty(resource);
            assertNotNull(response);

            RelyingPartyResource editResponse = relyingPartyService.updateRelyingParty(
                    response.getId(),
                    new EditRelyingPartyResource(
                            response.getName() + "new",
                            !response.isPublicSector(),
                            List.of(new RelyingPartyEntitlementResource("value")),
                            List.of(new RelyingPartyEaaResource("asd", "asd"), new RelyingPartyEaaResource("vsd", "ert")),
                            true
                    )
            );
            assertNotNull(editResponse);
            assertEquals(2, editResponse.getRelyingPartyEaas().size());
            assertEquals(response.getName() + "new", editResponse.getName());
            assertEquals(!response.isPublicSector(), editResponse.isPublicSector());

            RelyingPartyResource getResult = relyingPartyService.findRelyingParty(response.getId());
            assertNotNull(getResult);
            assertEquals(2, getResult.getRelyingPartyEaas().size());
            assertEquals(response.getName() + "new", getResult.getName());
            assertEquals(!response.isPublicSector(), getResult.isPublicSector());
        }

        @Test
        void testEaaEditRelyingParty() {
            CreateRelyingPartyResource resource = generateCreateRelyingPartyResource();

            RelyingPartyResource response = relyingPartyService.createRelyingParty(resource);
            assertNotNull(response);

            RelyingPartyResource editResponse1 = relyingPartyService.updateRelyingParty(
                    response.getId(),
                    new EditRelyingPartyResource(
                            response.getName(),
                            response.isPublicSector(),
                            List.of(new RelyingPartyEntitlementResource("value")),
                            List.of(new RelyingPartyEaaResource("asd", "asd"), new RelyingPartyEaaResource("vsd", "ert")),
                            true
                    )
            );
            assertNotNull(editResponse1);
            assertEquals(2, editResponse1.getRelyingPartyEaas().size());

            RelyingPartyResource getResult1 = relyingPartyService.findRelyingParty(response.getId());
            assertNotNull(getResult1);
            assertEquals(2, getResult1.getRelyingPartyEaas().size());

            RelyingPartyResource editResponse2 = relyingPartyService.updateRelyingParty(
                    response.getId(),
                    new EditRelyingPartyResource(
                            response.getName(),
                            response.isPublicSector(),
                            List.of(new RelyingPartyEntitlementResource("value")),
                            List.of(new RelyingPartyEaaResource("asd", "asd"), new RelyingPartyEaaResource("vsd", "ert")),
                            true
                    )
            );
            assertNotNull(editResponse2);
            assertEquals(2, editResponse2.getRelyingPartyEaas().size());

            RelyingPartyResource getResult2 = relyingPartyService.findRelyingParty(response.getId());
            assertNotNull(getResult2);
            assertEquals(2, getResult2.getRelyingPartyEaas().size());

            RelyingPartyResource editResponse3 = relyingPartyService.updateRelyingParty(
                    response.getId(),
                    new EditRelyingPartyResource(
                            response.getName(),
                            response.isPublicSector(),
                            List.of(new RelyingPartyEntitlementResource("value")),
                            new ArrayList<>(),
                            true
                    )
            );
            assertNotNull(editResponse3);
            assertEquals(0, editResponse3.getRelyingPartyEaas().size());

            RelyingPartyResource getResult3 = relyingPartyService.findRelyingParty(response.getId());
            assertNotNull(getResult3);
            assertEquals(0, getResult3.getRelyingPartyEaas().size());

            RelyingPartyResource editResponse4 = relyingPartyService.updateRelyingParty(
                    response.getId(),
                    new EditRelyingPartyResource(
                            response.getName(),
                            response.isPublicSector(),
                            List.of(new RelyingPartyEntitlementResource("value")),
                            List.of(new RelyingPartyEaaResource("asd", "asd"), new RelyingPartyEaaResource("vsd", "ert")),
                            true
                    )
            );
            assertNotNull(editResponse4);
            assertEquals(2, editResponse4.getRelyingPartyEaas().size());

            RelyingPartyResource getResult4 = relyingPartyService.findRelyingParty(response.getId());
            assertNotNull(getResult4);
            assertEquals(2, getResult4.getRelyingPartyEaas().size());
        }
    }
}
