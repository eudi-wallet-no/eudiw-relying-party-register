package no.idporten.eudiw.rp.admin.service.syntheticreportees;

import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;

public class StatelessPersistentSyntheticReporteeService implements SyntheticReporteeProvider {

    @Override
    public ReporteeAuthority getSyntheticReporteeAuthority(String id) {
        long unsignedHash = ((long) id.hashCode()) + Integer.MAX_VALUE;

        String orgno = PseudoRandomOrgnoGenerator.generateValidOrgno(unsignedHash);
        String name = "Syntetisk organisasjon %s".formatted(orgno);

        boolean isPublicSector = unsignedHash % 2 == 0;
        return new ReporteeAuthority(orgno, name, isPublicSector);
    }

    // generates valid synthetic orgnos in the range 200000000 .. 399999999
    private static class PseudoRandomOrgnoGenerator {

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

        private static String generateValidOrgno(long seed) {
            int rawOrgno = (int) ((Math.abs(seed) % 200000000) + 200000000);

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
