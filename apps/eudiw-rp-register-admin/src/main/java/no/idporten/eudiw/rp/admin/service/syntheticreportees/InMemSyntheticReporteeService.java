package no.idporten.eudiw.rp.admin.service.syntheticreportees;

import lombok.SneakyThrows;
import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class InMemSyntheticReporteeService implements SyntheticReporteeProvider {

    private static final Random rng = new Random();
    private final Map<String, ReporteeAuthority> knownUsers = new HashMap<>();

    private static final int UNIQUE_ORGNO_GENERATION_MAX_ATTEMPTS = 10;

    private String getValidOrgnoNotInUse() {
        Set<String> orgnosInUse =
            knownUsers.values()
                      .stream()
                      .map(ReporteeAuthority::orgno)
                      .collect(Collectors.toSet());
        for (int i = 0; i < UNIQUE_ORGNO_GENERATION_MAX_ATTEMPTS; i++) {
            String orgno = RandomOrgnoGenerator.generateValidOrgno();
            if (!orgnosInUse.contains(orgno)) {
                return orgno;
            }
        }
        throw new AdminServiceException(
            "InMemSyntheticReporteeService failed to generate unique random "
                + "orgno within a reasonable number of attempts");
    }

    private ReporteeAuthority generateReporteeAuthority() {
        String orgno = getValidOrgnoNotInUse();
        String name = "Test-brukarstad-%s".formatted(orgno);
        boolean isPublicSector = rng.nextBoolean();
        return new ReporteeAuthority(orgno, name, isPublicSector);
    }

    @SneakyThrows
    private String hashId(String id) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] sha256Digest = digest.digest(id.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sha256Digest);
    }

    public ReporteeAuthority getSyntheticReporteeAuthority(String id) {
        return knownUsers.computeIfAbsent(hashId(id), _ -> generateReporteeAuthority());
    }

    private static class RandomOrgnoGenerator {
        // orgno weights, from least to most significant digit.
        private static final int[] ORGNO_WEIGHTS = {2, 3, 4, 5, 6, 7, 2, 3};

        private static int getControlDigit(int orgno) {
            int sumOfProducts = 0;
            for (int i = 0; i < 8; i++) {
                orgno /= 10;
                int currentOrgnoDigit = orgno % 10;
                sumOfProducts += currentOrgnoDigit * ORGNO_WEIGHTS[i];
            }
            int remainder = sumOfProducts % 11;
            return remainder != 0 ? 11 - remainder : 0;
        }

        // generates valid orgnos in the range 200000000 .. 399999999
        private static String generateValidOrgno() {
            int rawOrgno = rng.nextInt(200000000, 400000000);

            int controlDigit = getControlDigit(rawOrgno);
            if (controlDigit == 10) {
                // make orgno valid by adding 1 to least significant non-control digit.
                // works because a weighted orgno digit can never have 11 as a factor
                // (given current orgno weights).
                int leastSignificantNonControlDigit = rawOrgno / 10 % 10;
                int newleastSignificantNonControlDigit = (leastSignificantNonControlDigit + 1) % 10;
                rawOrgno = rawOrgno - rawOrgno % 100 + newleastSignificantNonControlDigit * 10;
                controlDigit = getControlDigit(rawOrgno);
            }
            int orgno = rawOrgno - rawOrgno % 10 + controlDigit;
            return Integer.toString(orgno);
        }
    }
}
