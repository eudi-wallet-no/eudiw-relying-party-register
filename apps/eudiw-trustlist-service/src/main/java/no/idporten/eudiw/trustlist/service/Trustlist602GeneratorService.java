package no.idporten.eudiw.trustlist.service;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.trustlist.config.DigdirProperties;
import no.idporten.eudiw.trustlist.config.Trustlist602Properties;
import no.idporten.eudiw.trustlist.domain.etsi602.Trustlist;
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
public class Trustlist602GeneratorService {

    private final Trustlist602Properties trustlistProperties;
    private final DigdirProperties digdirProperties;

    public Trustlist602GeneratorService(Trustlist602Properties trustlistProperties, DigdirProperties digdirProperties) {
        this.trustlistProperties = trustlistProperties;
        this.digdirProperties = digdirProperties;
    }

    public LoTE generateTrustlist(String trustlist) {
        Trustlist list = trustlistProperties.tsl602().get(trustlist);// Check if the trustlist exists, if not throw exception.
        if (list == null) {
            throw new RuntimeException("Trustlist with name " + trustlist + " not found in properties: " + trustlistProperties.tsl602());
        }
        LoTE lote = new LoTE();

        lote.setListAndSchemeInformation(createListAndSchemeInformation(list.schemeInformation(), digdirProperties));
        if (list.trustedEntities() != null) {
            lote.setTrustedEntitiesList(createListOfTrustedEntity(list.trustedEntities(), digdirProperties));
        }
        return lote;
    }


    private static List<TrustedEntity> createListOfTrustedEntity(@Valid @NotNull Map<String, no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity> trustedEntityMap, DigdirProperties digdirProperties) {

        List<TrustedEntity> finishedList = new ArrayList<>();
        for (no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntity entity : trustedEntityMap.values()) {
            TrustedEntity trustedEntity = new TrustedEntity();
            TrustedEntityInformation trustedEntityInformation = populateTrustedEntityInformation(digdirProperties, entity.trustedEntityInformation());
            trustedEntity.setTrustedEntityInformation(trustedEntityInformation);

            if (entity.trustedEntityServices() != null) {
                for (no.idporten.eudiw.trustlist.domain.etsi602.TrustedEntityService service : entity.trustedEntityServices()) {
                    TrustedEntityService trustedEntityService = populateTrustedEntityService(service);
                    trustedEntity.getTrustedEntityServices().add(trustedEntityService);
                }
            }
            finishedList.add(trustedEntity);
        }
        return finishedList;
    }


}
