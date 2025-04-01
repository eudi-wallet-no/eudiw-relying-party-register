package no.eudiw.rp.register.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    // TODO: Jira EUW-23 (https://digdir.atlassian.net/browse/EUW-23)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Getter
    protected UUID id;

    // for Hibernate proxy-safe equals()
    protected static Class<?> getEffectiveClass(Object o) {
        return o instanceof HibernateProxy proxy
                   ? proxy.getHibernateLazyInitializer().getPersistentClass()
                   : o.getClass();
    }


    @Override
    public boolean equals(Object other) {
        return this == other
                   || other != null
                       && getEffectiveClass(this) == getEffectiveClass(other)
                       // always return false if either entity not yet persisted
                       && this.id != null
                       && Objects.equals(this.id, ((BaseEntity) other).id);
    }

    @Override
    public int hashCode() {
        return getEffectiveClass(this).hashCode();
    }
}
