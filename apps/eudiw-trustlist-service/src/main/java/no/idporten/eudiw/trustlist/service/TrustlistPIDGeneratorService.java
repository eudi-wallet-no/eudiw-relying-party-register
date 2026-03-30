package no.idporten.eudiw.trustlist.service;

import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.config.TrustlistPIDProperties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntity;
import no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntityInformation;
import no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntityService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static no.idporten.eudiw.trustlist.service.Common602Converter.*;

@Service
public class TrustlistPIDGeneratorService {

    private final TrustlistPIDProperties trustListPIDProperties;
    private final DigdirProperties digdirProperties;

    public TrustlistPIDGeneratorService(TrustlistPIDProperties trustListPIDProperties, DigdirProperties digdirProperties) {
        this.trustListPIDProperties = trustListPIDProperties;
        this.digdirProperties = digdirProperties;
    }

    public LoTE generateTrustlistPID() {
        LoTE lote = new LoTE();

        lote.setListAndSchemeInformation(createListAndSchemeInformation(trustListPIDProperties.schemeInformation(), digdirProperties));
        lote.setTrustedEntitiesList(createListOfTrustedEntity(trustListPIDProperties.trustedEntities().values(), digdirProperties));

        return lote;
    }


    /**
     * For each trusted entity in the trusted entity list, it makes spec objects of our PID config objects.
     * and for each trusted entity service within the list of services in the trusted entities, it also does that.
     *
     * @param trustedEntities Collection of TrustedEntity objects from our config.
     * @return List of trusted entities, the etsi602 spec objects.
     */
    private static List<TrustedEntity> createListOfTrustedEntity(Collection<no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity> trustedEntities, DigdirProperties digdirProperties) {
        List<TrustedEntity> finishedList = new ArrayList<>();
        for (no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity entity : trustedEntities) {
            TrustedEntity trustedEntity = new TrustedEntity();
            TrustedEntityInformation trustedEntityInformation = populateTrustedEntityInformation(digdirProperties, entity.trustedEntityInformation());
            trustedEntity.setTrustedEntityInformation(trustedEntityInformation);

            List<TrustedEntityService> trustedEntityServices = new ArrayList<>();
            for (no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService service : entity.trustedEntityServices()) {
                TrustedEntityService trustedEntityService = populateTrustedEntityService(service);
                trustedEntityServices.add(trustedEntityService);
            }
            trustedEntity.setTrustedEntityServices(trustedEntityServices);
            finishedList.add(trustedEntity);
        }
        return finishedList;
    }

}