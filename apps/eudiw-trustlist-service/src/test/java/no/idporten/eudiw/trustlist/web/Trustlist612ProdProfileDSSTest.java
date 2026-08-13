package no.idporten.eudiw.trustlist.web;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("When downloading 612 trustlist in environment TEST")
@ActiveProfiles({"prod"})
@Disabled("Disabled until we have a 612 trustlist in prod environment with services - otherwise this test will fail and cause problems for the pipeline")
public class Trustlist612ProdProfileDSSTest extends Trustlist612ProfilesDSSBaseTest {
}
