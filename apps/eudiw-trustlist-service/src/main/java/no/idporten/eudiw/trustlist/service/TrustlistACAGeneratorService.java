package no.idporten.eudiw.trustlist.service;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.config.TrustlistACAProperties;
import no.idporten.eudiw.trustlist.etsi119602.pojo.LoTE;
import no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntity;
import no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntityInformation;
import no.idporten.eudiw.trustlist.etsi119602.pojo.TrustedEntityService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.trustlist.service.Common602Converter.*;


@Service
public class TrustlistACAGeneratorService {

    private final TrustlistACAProperties acaProperties;
    private final DigdirProperties digdirProperties;

    public TrustlistACAGeneratorService(TrustlistACAProperties acaProperties, DigdirProperties digdirProperties) {
        this.acaProperties = acaProperties;
        this.digdirProperties = digdirProperties;
    }

    public LoTE generateTrustlistACA() {
        LoTE lote = new LoTE();

        lote.setListAndSchemeInformation(createListAndSchemeInformation(acaProperties.schemeInformation(), digdirProperties));
        lote.setTrustedEntitiesList(createListOfTrustedEntity(acaProperties.trustedEntities(), digdirProperties));

        return lote;
    }


    private static List<TrustedEntity> createListOfTrustedEntity(@Valid @NotNull Map<String, no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity> trustedEntityMap, DigdirProperties digdirProperties) {

        List<TrustedEntity> finishedList = new ArrayList<>();
        for (no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity entity : trustedEntityMap.values()) {
            TrustedEntity trustedEntity = new TrustedEntity();
            TrustedEntityInformation trustedEntityInformation = populateTrustedEntityInformation(digdirProperties, entity.trustedEntityInformation());
            trustedEntity.setTrustedEntityInformation(trustedEntityInformation);

            for (no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService service : entity.trustedEntityServices()) {
                TrustedEntityService trustedEntityService = populateTrustedEntityService(service);
                trustedEntity.getTrustedEntityServices().add(trustedEntityService);
            }
            finishedList.add(trustedEntity);
        }
        return finishedList;
    }


}
