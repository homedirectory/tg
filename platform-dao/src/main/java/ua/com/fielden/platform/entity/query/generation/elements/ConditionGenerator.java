package ua.com.fielden.platform.entity.query.generation.elements;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.query.metadata.DomainMetadata;
import ua.com.fielden.platform.entity.query.metadata.DomainMetadataAnalyser;
import ua.com.fielden.platform.entity.query.metadata.PropertyMetadata;
import ua.com.fielden.platform.entity.query.model.EntityResultQueryModel;
import ua.com.fielden.platform.entity.query.model.QueryModel;
import ua.com.fielden.platform.utils.EntityUtils;
import ua.com.fielden.platform.utils.Pair;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.select;

public class ConditionGenerator {
    private final DomainMetadataAnalyser domainMetadataAnalyser;

    public ConditionGenerator(final DomainMetadata domainMetadata) {
        this.domainMetadataAnalyser = new DomainMetadataAnalyser(domainMetadata);
    }

    /**
     * Genereates subquery with nested subqueries for given dot.notated property of given type with final query model containing filtering condition.
     * 
     * @param entityType
     * @param propString
     * @param mainTypeAlias
     * @param finalModel
     * @return
     */
    public <T extends AbstractEntity<?>> EntityResultQueryModel generateSubquery(final Class<T> entityType, final String propString, final String mainTypeAlias, final QueryModel finalModel) {
        final Pair<String, String> firstPropAndItsSubprop = EntityUtils.splitPropByFirstDot(propString);
        final PropertyMetadata info = domainMetadataAnalyser.getPropPersistenceInfoExplicitly(entityType, firstPropAndItsSubprop.getKey());
        final String bindingPropName = (mainTypeAlias == null ? "" : mainTypeAlias + ".") + firstPropAndItsSubprop.getKey();
        final Class<? extends AbstractEntity<?>> propType = info.getJavaType();
        final QueryModel subQuery = firstPropAndItsSubprop.getValue() == null ? finalModel : generateSubquery(propType, firstPropAndItsSubprop.getValue(), null, finalModel);
        return select(propType).where().prop("id").eq().extProp(bindingPropName).and().exists(subQuery).model();
    }
}