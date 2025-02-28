package no.eudiw.rp.register.data.repository;

import jakarta.annotation.Resource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class RelyingPartyEntitlementRepositoryTest {

    @Resource
    private RelyingPartyEntitlementRepository rpEntitlementRepository;
}
