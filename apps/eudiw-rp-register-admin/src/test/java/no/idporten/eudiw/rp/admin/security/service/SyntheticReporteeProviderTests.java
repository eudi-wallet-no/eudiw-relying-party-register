package no.idporten.eudiw.rp.admin.security.service;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.syntheticreportees.SyntheticReporteeProvider;
import no.idporten.eudiw.rp.admin.testdata.TestDataGenerator;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using synthetic reportee providers ...")
public class SyntheticReporteeProviderTests {

    @Nested
    @DisplayName("in non-prod environments ...")
    class NonProdEnvironmentTests {

        @BeforeAll
        public static void assertProdEnvironmentNotEnabled(@Autowired Environment env) {
            assertFalse(env.matchesProfiles("prod"));
        }

        @Autowired
        private SyntheticReporteeProvider syntheticReporteeProvider;

        @Test
        @DisplayName("then synthetic reportees with distinct orgnos are generated for distinct IDs")
        public void testReporteesForDistinctIdsAllHaveDistinctOrgnos() {
            int n = 1000;
            Set<String> distinctRandomIds =
                IntStream.range(0, n)
                    .mapToObj(_ -> TestDataGenerator.generateName())
                    .collect(Collectors.toSet());

            Set<String> distinctReporteeAuthorityOrgnos =
                distinctRandomIds.stream()
                                 .map(syntheticReporteeProvider::getSyntheticReporteeAuthority)
                                 .map(ReporteeAuthority::orgno)
                                 .collect(Collectors.toSet());

            assertEquals(distinctRandomIds.size(), distinctReporteeAuthorityOrgnos.size());
        }

        @Test
        @DisplayName("then repeated queries for the same ID yield the same synthetic reportee")
        public void testReporteesEqualWhenEqualIds() {
            String id = TestDataGenerator.generateName();

            ReporteeAuthority reportee =
                syntheticReporteeProvider.getSyntheticReporteeAuthority(id);

            int n = 10;
            Set<ReporteeAuthority> reportees =
                IntStream.range(0, n)
                    .mapToObj(_ -> syntheticReporteeProvider.getSyntheticReporteeAuthority(id))
                    .collect(Collectors.toSet());

            assertAll(
                () -> assertNotNull(reportee),
                () -> assertNotNull(reportees),
                () -> assertEquals(1, reportees.size()),
                () -> assertEquals(reportee, reportees.iterator().next())
            );
        }
    }

    @Nested
    @ActiveProfiles({"prod"})
    @DisplayName("in prod environments ...")
    class ProdEnvironmentTests {

        @BeforeAll
        public static void assertProdEnvironmentEnabled(@Autowired Environment env) {
            assertTrue(env.matchesProfiles("prod"));
        }

        @Autowired
        private SyntheticReporteeProvider syntheticReporteeProvider;

        @Test
        public void assertSyntheticReporteeProviderExplodesIfInvokedInProdEnvironment() {
            String id = TestDataGenerator.generateName();
            assertThrows(
                AdminServiceException.class,
                () -> syntheticReporteeProvider.getSyntheticReporteeAuthority(id)
            );
        }
    }
}

