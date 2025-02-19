package ua.com.fielden.platform.eql.meta;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static ua.com.fielden.platform.entity.AbstractEntity.ID;
import static ua.com.fielden.platform.entity.AbstractUnionEntity.unionProperties;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.expr;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.select;
import static ua.com.fielden.platform.reflection.AnnotationReflector.getAnnotation;
import static ua.com.fielden.platform.reflection.AnnotationReflector.isContextual;
import static ua.com.fielden.platform.reflection.Finder.getFieldByName;
import static ua.com.fielden.platform.utils.EntityUtils.isUnionEntityType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.AbstractUnionEntity;
import ua.com.fielden.platform.entity.annotation.Calculated;
import ua.com.fielden.platform.entity.query.fluent.EntityQueryProgressiveInterfaces.ICaseWhenFunctionWhen;
import ua.com.fielden.platform.entity.query.fluent.EntityQueryProgressiveInterfaces.IFromAlias;
import ua.com.fielden.platform.entity.query.fluent.EntityQueryProgressiveInterfaces.IStandAloneExprOperationAndClose;
import ua.com.fielden.platform.entity.query.fluent.EntityQueryProgressiveInterfaces.ISubsequentCompletedAndYielded;
import ua.com.fielden.platform.entity.query.model.EntityResultQueryModel;
import ua.com.fielden.platform.entity.query.model.ExpressionModel;
import ua.com.fielden.platform.eql.exceptions.EqlMetadataGenerationException;
import ua.com.fielden.platform.expression.ExpressionText2ModelConverter;

public class DomainMetadataUtils {

    /** Private default constructor to prevent instantiation. */
    private DomainMetadataUtils() {
    }
    
    public static ExpressionModel generateUnionEntityPropertyExpression(final Class<? extends AbstractUnionEntity> entityType, final String commonPropName) {
        return generateUnionEntityPropertyContextualExpression(unionProperties(entityType).stream().map(e -> e.getName()).collect(toList()), commonPropName, null);
    }
    
    public static ExpressionModel generateUnionEntityPropertyExpression(final List<String> unionMembers, final String commonPropName) {
        return generateUnionEntityPropertyContextualExpression(unionMembers, commonPropName, null);
    }
    
    public static ExpressionModel generateUnionEntityPropertyContextualExpression(final List<String> unionMembers, final String commonSubpropName, final String contextPropName) {
        if (unionMembers.isEmpty()) {
            return expr().val(null).model();
        }
        final Iterator<String> iterator = unionMembers.iterator();
        final String firstUnionPropName = (contextPropName == null ? "" :  contextPropName + ".") + iterator.next();
        ICaseWhenFunctionWhen<IStandAloneExprOperationAndClose, AbstractEntity<?>> expressionModelInProgress = expr().caseWhen().prop(firstUnionPropName).isNotNull().then().prop(firstUnionPropName
                + "." + commonSubpropName);

        for (; iterator.hasNext();) {
            final String unionPropName = (contextPropName == null ? "" :  contextPropName + ".") + iterator.next();
            expressionModelInProgress = expressionModelInProgress.when().prop(unionPropName).isNotNull().then().prop(unionPropName + "." + commonSubpropName);
        }

        return expressionModelInProgress.end().model();
    }

    public static ExpressionModel extractExpressionModelFromCalculatedProperty(final Class<? extends AbstractEntity<?>> entityType, final Field calculatedPropfield) {
        try {
            final Calculated calcAnnotation = getAnnotation(calculatedPropfield, Calculated.class);
            if (isNotEmpty(calcAnnotation.value())) {
                return createExpressionText2ModelConverter(entityType, calcAnnotation).convert().getModel();
            } else {
                final Field exprField = getFieldByName(entityType, calculatedPropfield.getName() + "_");
                exprField.setAccessible(true);
                return (ExpressionModel) exprField.get(null);
            }
        } catch (final Exception e) {
            throw new EqlMetadataGenerationException(format("Can't extract hard-coded expression model for prop [%s] due to: [%s]", calculatedPropfield.getName(), e.getMessage()));
        }
    }

    private static ExpressionText2ModelConverter createExpressionText2ModelConverter(final Class<? extends AbstractEntity<?>> entityType, final Calculated calcAnnotation)
            throws Exception {
        if (isContextual(calcAnnotation)) {
            return new ExpressionText2ModelConverter(getRootType(calcAnnotation), calcAnnotation.contextPath(), calcAnnotation.value());
        } else {
            return new ExpressionText2ModelConverter(entityType, calcAnnotation.value());
        }
    }

    private static Class<? extends AbstractEntity<?>> getRootType(final Calculated calcAnnotation) throws ClassNotFoundException {
        return (Class<? extends AbstractEntity<?>>) ClassLoader.getSystemClassLoader().loadClass(calcAnnotation.rootTypeName());
    }
    
    public static <ET extends AbstractEntity<?>> List<EntityResultQueryModel<ET>> produceUnionEntityModels(final Class<ET> entityType) {
        final List<EntityResultQueryModel<ET>> result = new ArrayList<>();
        if (!isUnionEntityType(entityType)) {
            return result;
        }

        final List<Field> unionProps = unionProperties((Class<? extends AbstractUnionEntity>) entityType);
        for (final Field currProp : unionProps) {
            result.add(generateModelForUnionEntityProperty(unionProps, currProp).modelAsEntity(entityType));
        }
        return result;
    }
    
    private static <PT extends AbstractEntity<?>> ISubsequentCompletedAndYielded<PT> generateModelForUnionEntityProperty(final List<Field> unionProps, final Field currProp) {
        final IFromAlias<PT> startWith = select((Class<PT>) currProp.getType());
        final Field firstUnionProp = unionProps.get(0);
        final ISubsequentCompletedAndYielded<PT> initialModel = firstUnionProp.equals(currProp) ? startWith.yield().prop(ID).as(firstUnionProp.getName()) : startWith.yield().val(null).as(firstUnionProp.getName()); 
        return unionProps.stream().skip(1).reduce(initialModel, (m, f) -> f.equals(currProp) ? m.yield().prop(ID).as(f.getName()) : m.yield().val(null).as(f.getName()), (m1, m2) -> {throw new UnsupportedOperationException("Combining is not applicable here.");});
    }
    
    public static String getOriginalEntityTypeFullName(final String entityTypeFullClassName) {
        final int nameEnhancementStartIndex = entityTypeFullClassName.indexOf("$$");
        return  nameEnhancementStartIndex == -1 ? entityTypeFullClassName : entityTypeFullClassName.substring(0, nameEnhancementStartIndex);
    }
}