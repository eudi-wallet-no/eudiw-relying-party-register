package no.eudiw.rp.register.data.entitlement;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EntitlementAttributeConverter
    implements AttributeConverter<Entitlement, String> {
    @Override
    public String convertToDatabaseColumn(Entitlement entitlement) {
        return entitlement.getValue();
    }
    @Override
    public Entitlement convertToEntityAttribute(String s) {
        return Entitlement.fromString(s);
    }
}
