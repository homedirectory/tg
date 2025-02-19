package ua.com.fielden.platform.entity.query.metadata;

import static java.lang.String.format;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.query.DbVersion;
import ua.com.fielden.platform.entity.query.EntityAggregates;
import ua.com.fielden.platform.entity.query.exceptions.EqlException;
import ua.com.fielden.platform.reflection.Finder;
import ua.com.fielden.platform.utils.EntityUtils;
import ua.com.fielden.platform.utils.Pair;

public class DomainMetadataAnalyser {
    private final Logger logger = Logger.getLogger(DomainMetadataAnalyser.class);
    private final Map<Class<? extends AbstractEntity<?>>, AbstractEntityMetadata> entityMetadataMap = new HashMap<>();
    private final DomainMetadata domainMetadata;

    public DomainMetadataAnalyser(final DomainMetadata domainMetadata) {
        this.domainMetadata = domainMetadata;
        entityMetadataMap.putAll(domainMetadata.getPersistedEntityMetadataMap());
        entityMetadataMap.putAll(domainMetadata.getModelledEntityMetadataMap());
        entityMetadataMap.putAll(domainMetadata.getPureEntityMetadataMap());
    }

    public <ET extends AbstractEntity<?>> PersistedEntityMetadata<ET> getPersistedEntityMetadata(final Class<ET> entityType) {
        return (PersistedEntityMetadata) entityMetadataMap.get(entityType);
    }

    public <ET extends AbstractEntity<?>> AbstractEntityMetadata<ET> getEntityMetadata(final Class<ET> entityType) {
        if (entityType == null || !AbstractEntity.class.isAssignableFrom(entityType) || EntityAggregates.class.equals(entityType)) {
            return null;
        }

        final AbstractEntityMetadata<ET> existing = entityMetadataMap.get(entityType);

        if (existing != null) {
            return existing;
        } else {
            try {
                final AbstractEntityMetadata<ET> newOne;
                final EntityTypeInfo<ET> parentInfo = new EntityTypeInfo<>(entityType);
                switch (parentInfo.category) {
                case PERSISTENT:
                    newOne = domainMetadata.generatePersistedEntityMetadata(parentInfo);
                    break;
                case QUERY_BASED:
                    newOne = domainMetadata.generateModelledEntityMetadata(parentInfo);
                    break;
                case UNION:
                    newOne = domainMetadata.generateUnionedEntityMetadata(parentInfo);
                    break;
                default:
                    newOne = domainMetadata.generatePureEntityMetadata(parentInfo);
                }

                entityMetadataMap.put(entityType, newOne);

                return newOne;
            } catch (final Exception ex) {
                final String msg = format("Error while building metadata for type [%s].", entityType.getName());
                logger.error(msg, ex);
                throw new EqlException(msg, ex);
            }
        }
    }

    /**
     * Retrieves persistence info for entity property, which is explicitly persisted within this entity type.
     *
     * @param entityType
     * @param propName
     * @return
     */
    public <ET extends AbstractEntity<?>> PropertyMetadata getPropPersistenceInfoExplicitly(final Class<ET> entityType, final String propName) {
        final AbstractEntityMetadata<ET> map = getEntityMetadata(entityType);
        return map != null ? map.getProps().get(propName) : null;
    }

    /**
     * Retrieves persistence info for entity property or its nested subproperty.
     *
     * @param entityType
     * @param propName
     * @return
     */
    public PropertyMetadata getInfoForDotNotatedProp(final Class<? extends AbstractEntity<?>> entityType, final String dotNotatedPropName) {
        final PropertyMetadata simplePropInfo = getPropPersistenceInfoExplicitly(entityType, dotNotatedPropName);
        if (simplePropInfo != null) {
            return simplePropInfo;
        } else {
            final Pair<String, String> propSplit = EntityUtils.splitPropByFirstDot(dotNotatedPropName);
            final PropertyMetadata firstPropInfo = getPropPersistenceInfoExplicitly(entityType, propSplit.getKey());
            if (firstPropInfo != null && firstPropInfo.getJavaType() != null) {
                return getInfoForDotNotatedProp(firstPropInfo.getJavaType(), propSplit.getValue());
            } else {
                return null;
            }
        }
    }

    public boolean isNullable(final Class<? extends AbstractEntity<?>> entityType, final String dotNotatedPropName) {
        final PropertyMetadata simplePropInfo = getPropPersistenceInfoExplicitly(entityType, dotNotatedPropName);
        if (simplePropInfo != null) {
            return simplePropInfo.isNullable();
        } else {
            final Pair<String, String> propSplit = EntityUtils.splitPropByFirstDot(dotNotatedPropName);
            final PropertyMetadata firstPropInfo = getPropPersistenceInfoExplicitly(entityType, propSplit.getKey());
            if (firstPropInfo != null && firstPropInfo.getJavaType() != null) {
                return isNullable(firstPropInfo.getJavaType(), propSplit.getValue()) || firstPropInfo.isNullable();
            } else {
                throw new IllegalArgumentException("Couldn't determine nullability for prop [" + dotNotatedPropName + "] in type [" + entityType + "]");
            }
        }
    }

    public Collection<PropertyMetadata> getPropertyMetadatasForEntity(final Class<? extends AbstractEntity<?>> entityType) {
        final AbstractEntityMetadata epm = getEntityMetadata(entityType);
        if (epm == null) {
            throw new IllegalStateException("Missing ppi map for entity type: " + entityType);
        }
        return epm.getProps().values();
    }

    public DbVersion getDbVersion() {
        return domainMetadata.dbVersion;
    }

    public Map<Class<?>, Object> getHibTypesDefaults() {
        return domainMetadata.getHibTypesDefaults();
    }

    public Object getBooleanValue(final boolean value) {
        return domainMetadata.getBooleanValue(value);
    }

    public Set<String> getLeafPropsFromFirstLevelProps(final String parentProp, final Class<? extends AbstractEntity<?>> entityType, final Set<String> firstLevelProps) {
        final Set<String> result = new HashSet<>();

        for (final String prop : firstLevelProps) {
            final PropertyMetadata propMetadata = getPropPersistenceInfoExplicitly(entityType, prop);
            if (propMetadata.isEntityOfPersistedType()) {
                final Set<String> keyProps = new HashSet<>(Finder.getFieldNames(Finder.getKeyMembers(propMetadata.getJavaType())));
                if (EntityUtils.isCompositeEntity(propMetadata.getJavaType())) {
                    result.addAll(getLeafPropsFromFirstLevelProps(prop, propMetadata.getJavaType(), keyProps));
                } else {
                    result.add((parentProp != null ? (parentProp + ".") : "") + prop);
                }
            } else {
                result.add((parentProp != null ? (parentProp + ".") : "") + prop);
            }
        }

        return result;
    }
}