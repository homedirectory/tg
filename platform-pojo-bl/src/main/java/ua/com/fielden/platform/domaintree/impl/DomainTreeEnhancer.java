package ua.com.fielden.platform.domaintree.impl;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static ua.com.fielden.platform.reflection.asm.impl.DynamicEntityClassLoader.isGenerated;
import static ua.com.fielden.platform.reflection.asm.impl.DynamicTypeNamingService.APPENDIX;
import static ua.com.fielden.platform.reflection.asm.impl.DynamicTypeNamingService.nextTypeName;
import static ua.com.fielden.platform.types.tuples.T2.t2;
import static ua.com.fielden.platform.utils.CollectionUtil.linkedMapOf;
import static ua.com.fielden.platform.utils.Pair.pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ua.com.fielden.platform.domaintree.ICalculatedProperty;
import ua.com.fielden.platform.domaintree.ICalculatedProperty.CalculatedPropertyAttribute;
import ua.com.fielden.platform.domaintree.IDomainTreeEnhancer;
import ua.com.fielden.platform.domaintree.IDomainTreeEnhancerCache;
import ua.com.fielden.platform.domaintree.IProperty;
import ua.com.fielden.platform.domaintree.exceptions.DomainTreeException;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.annotation.Calculated;
import ua.com.fielden.platform.entity.annotation.IsProperty;
import ua.com.fielden.platform.entity.annotation.Title;
import ua.com.fielden.platform.entity.annotation.factory.CalculatedAnnotation;
import ua.com.fielden.platform.entity.annotation.factory.CustomPropAnnotation;
import ua.com.fielden.platform.entity.annotation.factory.IsPropertyAnnotation;
import ua.com.fielden.platform.entity.factory.EntityFactory;
import ua.com.fielden.platform.reflection.AnnotationReflector;
import ua.com.fielden.platform.reflection.Finder;
import ua.com.fielden.platform.reflection.PropertyTypeDeterminator;
import ua.com.fielden.platform.reflection.Reflector;
import ua.com.fielden.platform.reflection.asm.api.NewProperty;
import ua.com.fielden.platform.reflection.asm.impl.DynamicEntityClassLoader;
import ua.com.fielden.platform.ui.menu.MiTypeAnnotation;
import ua.com.fielden.platform.ui.menu.MiWithConfigurationSupport;
import ua.com.fielden.platform.utils.EntityUtils;
import ua.com.fielden.platform.utils.Pair;

/**
 * A domain manager implementation with all sufficient logic for domain modification / loading. <br>
 * <br>
 *
 * <b>Implementation notes:</b><br>
 * 1. After the modifications have been applied manager consists of a map of (entityType -> real enhanced entityType). To play correctly with any type information with enhanced
 * domain you need to use ({@link #getManagedType(Class)} of entityType; dotNotationName) instead of (entityType; dotNotationName).<br>
 * 2. The current version of manager after some modifications (calcProperty has been added/removed/changed) holds a full list of calculated properties for all types. This list
 * should be applied or discarded using {@link #apply()} or {@link #discard()} interface methods.<br>
 * 3.
 *
 * @author TG Team
 *
 */
public final class DomainTreeEnhancer extends AbstractDomainTree implements IDomainTreeEnhancer {
    private static final Logger logger = Logger.getLogger(DomainTreeEnhancer.class);

    /**
     * Holds byte arrays & <b>enhanced</b> types mapped to their original root types. Contains pairs of [original -> real & arrays] or [original -> original & emptyArrays] (in case
     * of not enhanced type).
     */
    private final Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> originalAndEnhancedRootTypesAndArrays;

    /** Holds current domain differences from "standard" domain (all calculated properties for all root types). */
    private final Map<Class<?>, List<CalculatedProperty>> calculatedProperties;

    /** Holds current domain differences from "standard" domain (all custom properties for all root types). */
    private final Map<Class<?>, List<CustomProperty>> customProperties;
    
    private final transient List<Annotation> rootAnnotations = new ArrayList<>();
    private transient Optional<String> rootNameSuffix = empty();
    
    public static class ByteArray {
        private final byte[] array;

        protected ByteArray() {
            array = null;
        }

        public ByteArray(final byte[] array) {
            this.array = array;
        }

        public byte[] getArray() {
            return array;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(array);
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ByteArray other = (ByteArray) obj;
            return Arrays.equals(array, other.array);
        }
    }

    /**
     * Constructs a new instance of domain enhancer with clean, not enhanced, domain.
     * <p>
     * However, no calculated properties have been added -- the resultant types will be enhanced. They will use a marker property (for more information see
     * {@link #generateHierarchy(Set, Map)} method).
     *
     * @param rootTypes -- root types
     *
     */
    public DomainTreeEnhancer(final EntityFactory entityFactory, final Set<Class<?>> rootTypes) {
        this(entityFactory, rootTypes, createEmptyCalculatedPropsFromRootTypes(rootTypes), createEmptyCustomPropsFromRootTypes(rootTypes));
    }

    /**
     * Constructs a new instance of domain enhancer with full information about containing root types (<b>enhanced</b> or not). This primary constructor should be used for
     * serialisation and copying. Please also note that calculated property changes, that were not applied, will be disappeared! So every enhancer should be carefully applied (or
     * discarded) before serialisation.
     *
     */
    private DomainTreeEnhancer(final EntityFactory entityFactory, final Set<Class<?>> rootTypes, final Map<Class<?>, Set<CalculatedPropertyInfo>> calculatedPropertiesInfo, final Map<Class<?>, List<CustomProperty>> customProperties) {
        super(entityFactory);

        this.originalAndEnhancedRootTypesAndArrays = new LinkedHashMap<>();
        // init a map with NOT enhanced types and empty byte arrays.
        this.originalAndEnhancedRootTypesAndArrays.putAll(createOriginalAndEnhancedRootTypesAndArraysFromRootTypes(rootTypes));

        this.customProperties = new LinkedHashMap<>();
        this.customProperties.putAll(customProperties);

        this.calculatedProperties = new LinkedHashMap<>();
        this.calculatedProperties.putAll(createCalculatedPropertiesFrom(this, calculatedPropertiesInfo));

        apply();

        this.customProperties.clear();
        this.customProperties.putAll(customProperties);

        this.calculatedProperties.clear();
        this.calculatedProperties.putAll(extractAll(this, true));
        
        // Perform some post-creation validation.
        validateEnhancer();
    }
    
    /**
     * Copy constructor for {@link DomainTreeEnhancer} taking benefit from shared inner resources (like 'ast's of corresponding CalculatedProperty'es).
     * <p>
     * This is performance-friendly version of copying function without unnecessary parsing of {@link CalculatedProperty#getContextualExpression()},
     * which is costly operation.
     * 
     * @param enhancer
     */
    private DomainTreeEnhancer(final DomainTreeEnhancer enhancer) {
        super(enhancer.getFactory());
        
        this.rootAnnotations.addAll(enhancer.rootAnnotations);
        this.rootNameSuffix = enhancer.rootNameSuffix;
        
        // Perform copying of originalAndEnhancedRootTypesAndArrays. ByteArray class is immutable so it is safe to use the same shared instances.
        this.originalAndEnhancedRootTypesAndArrays = new LinkedHashMap<>();
        for (final Entry<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> entry: enhancer.originalAndEnhancedRootTypesAndArrays.entrySet()) {
            final Map<String, ByteArray> byteArraysCopy = new LinkedHashMap<>(entry.getValue().getValue());
            this.originalAndEnhancedRootTypesAndArrays.put(entry.getKey(), pair(entry.getValue().getKey(), byteArraysCopy));
        }
        
        // CustomProperty is fully immutable and it is also safe to use the same shared instances of that type across all DomainTreeEnhancer copies.
        this.customProperties = new LinkedHashMap<>();
        this.customProperties.putAll(enhancer.customProperties);
        
        // CalculatedProperty instances will be copied through CalculatedProperty.copy method which shares inner 'ast's and other derived information across all CalculatedProperty copies.
        this.calculatedProperties = new LinkedHashMap<>();
        for (final Entry<Class<?>, List<CalculatedProperty>> entry: enhancer.calculatedProperties.entrySet()) {
            this.calculatedProperties.put(entry.getKey(), entry.getValue().stream().map(cp -> cp.copy(this)).collect(toCollection(ArrayList::new)));
        }
        
        // Perform some post-creation validation.
        validateEnhancer();
    }
    
    /**
     * Validates this instance on subject of conformity of resultant managed type and presence of calculated / custom properties.
     */
    private void validateEnhancer() {
        for (final Class<?> rootType : rootTypes()) {
            // check whether the type WITH calculated properties IS enhanced 
            if (!hasNoAdditionalProperties(rootType) && !isGenerated(getManagedType(rootType))) {
                throw new IllegalStateException(format("The type [%s] should be enhanced -- it has %s properties.", rootType.getSimpleName(), additionalPropDefinitionsAsString(rootType)));
            }
            // check whether the type WITHOUT calculated properties IS NOT enhanced 
            if (hasNoAdditionalProperties(rootType) && isGenerated(getManagedType(rootType))) {
                throw new IllegalStateException(format("The type [%s] should be NOT enhanced -- it has no additional properties.", rootType.getSimpleName()));
            }
        }
    }
    
    /**
     * A constructor <b>strictly</b> for version maintenance.
     *
     * @param entityFactory
     * @param originalAndEnhancedRootTypesAndArrays
     * @param calculatedProperties
     */
    public DomainTreeEnhancer(final EntityFactory entityFactory, final Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> originalAndEnhancedRootTypesAndArrays, final Map<Class<?>, List<CalculatedProperty>> calculatedProperties, final Map<Class<?>, List<CustomProperty>> customProperties) {
        super(entityFactory);
        this.originalAndEnhancedRootTypesAndArrays = originalAndEnhancedRootTypesAndArrays;
        this.calculatedProperties = calculatedProperties;
        this.customProperties = customProperties;
    }
    
    /**
     * Creates {@link DomainTreeEnhancer} instance from <code>rootTypes</code> that are used for calculated/custom properties mapped to each root type.
     * <p>
     * Please note that this method works with {@link DomainTreeEnhancer} cache. At this stage, instances of {@link DomainTreeEnhancer} are mutable, 
     * but in practice we do not use their mutability and we can safely cache every instance of {@link DomainTreeEnhancer}.
     * <p>
     * Mutability is not an issue due to the following:<br>
     * 1. EntityCentre.createDefaultCentre method applies calculated/custom properties and after that {@link DomainTreeEnhancer} remains in the same state.<br>
     * 2. Instances, that were deserialised after retrieval from database or during copying, are not mutated afterwards.<br>
     * We need to consider {@link DomainTreeEnhancer} cache management when implementing ability to add calculated properties from UI.
     * 
     * @param serialiser
     * @param domainTreeEnhancerCache
     * @param rootTypes
     * @param calculatedPropertiesInfo
     * @param customProperties
     * @param miType -- menu item type to which this enhancer will be related; please note that only enhancers that are related to some miType will be cached -- annotation MiType must be generated there
     * @return
     */
    public static DomainTreeEnhancer createFrom(
            final EntityFactory entityFactory,
            final IDomainTreeEnhancerCache domainTreeEnhancerCache,
            final Set<Class<?>> rootTypes,
            final Map<Class<?>, Set<CalculatedPropertyInfo>> calculatedPropertiesInfo,
            final Map<Class<?>, List<CustomProperty>> customProperties,
            final Optional<Class<? extends MiWithConfigurationSupport<?>>> miType) {
        final DomainTreeEnhancer cachedInstance = domainTreeEnhancerCache.getDomainTreeEnhancerFor(rootTypes, calculatedPropertiesInfo, customProperties);
        if (cachedInstance != null) {
            return new DomainTreeEnhancer(cachedInstance);
        } else {
            final DomainTreeEnhancer newInstance = new DomainTreeEnhancer(entityFactory, rootTypes, calculatedPropertiesInfo, customProperties);
            
            if (miType.isPresent()) {
                for (final Class<?> root: rootTypes) {
                    if (isGenerated(newInstance.getManagedType(root))) {
                        newInstance.adjustManagedTypeAnnotations(root, new MiTypeAnnotation().newInstance(miType.get()));
                    }
                }
                domainTreeEnhancerCache.putDomainTreeEnhancerFor(rootTypes, calculatedPropertiesInfo, customProperties, newInstance);
            }
            return newInstance;
        }
    }
    
    private boolean hasNoAdditionalProperties(final Class<?> rootType) {
        return this.calculatedProperties.get(rootType) == null && (this.customProperties.get(rootType) == null || this.customProperties.get(rootType).isEmpty());
    }
    
    private String additionalPropDefinitionsAsString(final Class<?> rootType) {
        final StringBuilder sb = new StringBuilder();
        if (this.calculatedProperties.get(rootType) != null) {
            sb.append(this.calculatedProperties.get(rootType).size() + " calculated ");
        }
        if (this.customProperties.get(rootType) != null && !this.customProperties.get(rootType).isEmpty()) {
            sb.append(this.customProperties.get(rootType).size() + " custom ");
        }
        return StringUtils.isEmpty(sb.toString()) ? " no " : sb.toString();
    }

    /**
     * Creates an empty map of calc props for <code>rootTypes</code>.
     *
     * @param rootTypes
     * @return
     */
    private static final Map<Class<?>, Set<CalculatedPropertyInfo>> createEmptyCalculatedPropsFromRootTypes(final Set<Class<?>> rootTypes) {
        final Map<Class<?>, Set<CalculatedPropertyInfo>> map = new LinkedHashMap<>();
        for (final Class<?> rootType : rootTypes) {
            map.put(rootType, new HashSet<CalculatedPropertyInfo>());
        }
        return map;
    }

    /**
     * Creates an empty map of custom props for <code>rootTypes</code>.
     *
     * @param rootTypes
     * @return
     */
    private static final Map<Class<?>, List<CustomProperty>> createEmptyCustomPropsFromRootTypes(final Set<Class<?>> rootTypes) {
        final Map<Class<?>, List<CustomProperty>> map = new LinkedHashMap<>();
        for (final Class<?> rootType : rootTypes) {
            map.put(rootType, new ArrayList<>());
        }
        return map;
    }

    /**
     * Creates a map of [original -> original & emptyArrays] for provided <code>rootTypes</code>.
     *
     * @param rootTypes
     * @return
     */
    private static Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> createOriginalAndEnhancedRootTypesAndArraysFromRootTypes(final Set<Class<?>> rootTypes) {
        final Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> originalAndEnhancedRootTypesAndArrays = new LinkedHashMap<>();
        for (final Class<?> rootType : rootTypes) {
            originalAndEnhancedRootTypesAndArrays.put(rootType, new Pair<Class<?>, Map<String, ByteArray>>(rootType, new LinkedHashMap<String, ByteArray>()));
        }
        return originalAndEnhancedRootTypesAndArrays;
    }

    /**
     * Groups calc props into the map by its domain paths.
     *
     * @param calculatedProperties
     * @return
     */
    private static Map<Class<?>, Map<String, Map<String, IProperty>>> groupByPaths(final Map<Class<?>, List<CalculatedProperty>> calculatedProperties, final Map<Class<?>, List<CustomProperty>> customProperties, final Set<Class<?>> rootTypes) {
        final Map<Class<?>, Map<String, Map<String, IProperty>>> grouped = new LinkedHashMap<>();
        for (final Entry<Class<?>, List<CalculatedProperty>> entry : calculatedProperties.entrySet()) {
            final Class<?> root = entry.getKey();
            final List<CalculatedProperty> props = entry.getValue();
            if (props != null && !props.isEmpty()) {
                if (!grouped.containsKey(root)) {
                    grouped.put(root, new LinkedHashMap<String, Map<String, IProperty>>());
                }
                for (final CalculatedProperty prop : props) {
                    final String path = prop.path();
                    if (!grouped.get(root).containsKey(path)) {
                        grouped.get(root).put(path, new LinkedHashMap<String, IProperty>());
                    }
                    grouped.get(root).get(path).put(prop.name(), prop);
                }
            } else {
                grouped.put(root, null);
            }
        }
        for (final Entry<Class<?>, List<CustomProperty>> entry : customProperties.entrySet()) {
            final Class<?> root = entry.getKey();
            final List<CustomProperty> props = entry.getValue();
            if (props != null && !props.isEmpty()) {
                if (grouped.get(root) == null) {
                    grouped.put(root, new LinkedHashMap<String, Map<String, IProperty>>());
                }
                for (final CustomProperty prop : props) {
                    final String path = prop.path();
                    if (!grouped.get(root).containsKey(path)) {
                        grouped.get(root).put(path, new LinkedHashMap<String, IProperty>());
                    }
                    grouped.get(root).get(path).put(prop.name(), prop);
                }
            } else {
                if (!grouped.containsKey(root)) {
                    grouped.put(root, null);
                }
            }
        }
        // add the types, not enhanced with any calc prop
        for (final Class<?> originalRoot : rootTypes) {
            if (!grouped.containsKey(originalRoot)) {
                grouped.put(originalRoot, null);
            }
        }
        return grouped;
    }

    @Override
    public Class<?> getManagedType(final Class<?> type) {
        CalculatedProperty.validateRootWithoutRootTypeEnforcement(this, type);
        return originalAndEnhancedRootTypesAndArrays.get(type) == null ? type : originalAndEnhancedRootTypesAndArrays.get(type).getKey();
    }

    @Override
    public List<ByteArray> getManagedTypeArrays(final Class<?> type) {
        CalculatedProperty.validateRoot(this, type);
        final Map<String, ByteArray> byteArrays = originalAndEnhancedRootTypesAndArrays.get(type).getValue();
        return new ArrayList<>(byteArrays.values());
    }

    @Override
    public void apply() {
        //////////// Performs migration [calculatedProperties => originalAndEnhancedRootTypes] ////////////
        final Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> freshOriginalAndEnhancedRootTypesAndArrays = generateHierarchy(originalAndEnhancedRootTypesAndArrays.keySet(), calculatedProperties, customProperties, rootAnnotations, rootNameSuffix);
        originalAndEnhancedRootTypesAndArrays.clear();
        originalAndEnhancedRootTypesAndArrays.putAll(freshOriginalAndEnhancedRootTypesAndArrays);
    }

    /**
     * Fully generates a new hierarchy of "originalAndEnhancedRootTypes" that conform to "calculatedProperties".
     * <p>
     * Note, that if no calculated properties specified for some rootType -- a marker calc property will be used to ensure that the resultant rootType will be enhanced.
     *
     * @param rootTypes
     * @param calculatedProperties
     * @param customProperties
     * @return
     */
    protected static Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> generateHierarchy(final Set<Class<?>> rootTypes, final Map<Class<?>, List<CalculatedProperty>> calculatedProperties, final Map<Class<?>, List<CustomProperty>> customProperties, final List<Annotation> rootAnnotations, final Optional<String> rootNameSuffix) {
        // single classLoader instance is needed for single "apply" transaction
        final DynamicEntityClassLoader classLoader = DynamicEntityClassLoader.getInstance(ClassLoader.getSystemClassLoader());
        final Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> originalAndEnhancedRootTypes = createOriginalAndEnhancedRootTypesAndArraysFromRootTypes(rootTypes);
        final Map<Class<?>, Map<String, Map<String, IProperty>>> groupedCalculatedProperties = groupByPaths(calculatedProperties, customProperties, rootTypes);

        // iterate through calculated property places (e.g. Vehicle.class+"" or WorkOrder.class+"veh.status") with no care about order
        for (final Entry<Class<?>, Map<String, Map<String, IProperty>>> entry : groupedCalculatedProperties.entrySet()) {
            final Class<?> originalRoot = entry.getKey();
            // generate predefined root type name for all calculated properties
            final String predefinedRootTypeName = rootNameSuffix.isPresent() ? originalRoot.getName() + APPENDIX + "_" + rootNameSuffix.get() : nextTypeName(originalRoot.getName());
            if (entry.getValue() == null) {
                final ByteArray newByteArray = new ByteArray(classLoader.getCachedByteArray(originalRoot.getName()));
                originalAndEnhancedRootTypes.put(originalRoot, new Pair<Class<?>, Map<String, ByteArray>>(originalRoot,  linkedMapOf(t2("", newByteArray))));
            } else {
                for (final Entry<String, Map<String, IProperty>> placeAndProps : entry.getValue().entrySet()) {
                    final Map<String, IProperty> props = placeAndProps.getValue();
                    if (props != null && !props.isEmpty()) {
                        final Class<?> realRoot = originalAndEnhancedRootTypes.get(originalRoot).getKey();
                        // a path to calculated properties
                        final String path = placeAndProps.getKey();

                        final NewProperty[] newProperties = new NewProperty[props.size()];
                        int i = 0;
                        for (final Entry<String, IProperty> nameWithProp : props.entrySet()) {
                            final IProperty iProp = nameWithProp.getValue();
                            if (iProp instanceof CalculatedProperty) {
                                final CalculatedProperty prop = (CalculatedProperty) iProp;
                                final String originationProperty = prop.getOriginationProperty() == null ? "" : prop.getOriginationProperty();
                                final Annotation calcAnnotation = new CalculatedAnnotation().contextualExpression(prop.getContextualExpression()).rootTypeName(predefinedRootTypeName).contextPath(prop.getContextPath()).origination(originationProperty).attribute(prop.getAttribute()).category(prop.category()).newInstance();
                                final IsProperty isPropAnnot = prop.getPrecision() != null && prop.getScale() != null ? new IsPropertyAnnotation(prop.getPrecision(), prop.getScale()).newInstance() : NewProperty.DEFAULT_IS_PROPERTY_ANNOTATION;
                                newProperties[i++] = new NewProperty(nameWithProp.getKey(), prop.resultType(), false, prop.getTitle(), prop.getDesc(),
                                                                     isPropAnnot,
                                                                     calcAnnotation);
                            } else { // this should be CustomProperty!
                                final CustomProperty prop = (CustomProperty) iProp;
                                newProperties[i++] = new NewProperty(nameWithProp.getKey(), prop.resultType(), false, prop.getTitle(), prop.getDesc(), new CustomPropAnnotation().newInstance());
                            }
                        }
                        // determine a "real" parent type:
                        final Class<?> realParentToBeEnhanced = StringUtils.isEmpty(path) ? realRoot : PropertyTypeDeterminator.determinePropertyType(realRoot, path);
                        try {
                            final Map<String, ByteArray> existingByteArrays = new LinkedHashMap<>(originalAndEnhancedRootTypes.get(originalRoot).getValue());

                            // generate & load new type enhanced by calculated properties
                            final Class<?> realParentEnhanced = classLoader.startModification(realParentToBeEnhanced).addProperties(newProperties)./* TODO modifySupertypeName(realParentToBeEnhanced.getName()).*/endModification();
                            // propagate enhanced type to root
                            final Pair<Class<?>, Map<String, ByteArray>> rootAfterPropagationAndAdditionalByteArrays = propagateEnhancedTypeToRoot(realParentEnhanced, realRoot, path, classLoader);
                            final Class<?> rootAfterPropagation = rootAfterPropagationAndAdditionalByteArrays.getKey();
                            // insert new byte arrays into beginning (the first item is an array of root type)
                            existingByteArrays.putAll(rootAfterPropagationAndAdditionalByteArrays.getValue());
                            // replace relevant root type in cache
                            originalAndEnhancedRootTypes.put(originalRoot, new Pair<Class<?>, Map<String, ByteArray>>(rootAfterPropagation, existingByteArrays));
                        } catch (final ClassNotFoundException e) {
                            logger.error(e);
                            throw new IllegalStateException(e);
                        }
                    }
                }
            }
            try {
                // modify root type name with predefinedRootTypeName
                final Pair<Class<?>, Map<String, ByteArray>> current = originalAndEnhancedRootTypes.get(originalRoot);
                final Class<?> enhancedRoot = current.getKey();
                if (originalRoot != enhancedRoot) { // calculated properties exist -- root type should be enhanced
                    final Class<?> rootWithPredefinedName = classLoader.startModification(enhancedRoot).addClassAnnotations(rootAnnotations.toArray(new Annotation[rootAnnotations.size()])).modifyTypeName(predefinedRootTypeName)./* TODO modifySupertypeName(originalRoot.getName()).*/endModification();
                    final Map<String, ByteArray> byteArraysWithRenamedRoot = new LinkedHashMap<>();

                    byteArraysWithRenamedRoot.putAll(current.getValue());
                    byteArraysWithRenamedRoot.put("", new ByteArray(classLoader.getCachedByteArray(rootWithPredefinedName.getName())));
                    final Pair<Class<?>, Map<String, ByteArray>> neww = pair(rootWithPredefinedName, byteArraysWithRenamedRoot);
                    originalAndEnhancedRootTypes.put(originalRoot, neww);
                }
            } catch (final ClassNotFoundException e) {
                logger.error(e);
                throw new IllegalStateException(e);
            }
        }
        return originalAndEnhancedRootTypes;
    }
    
    @Override
    public Class<?> adjustManagedTypeName(final Class<?> root, final String clientGeneratedTypeNameSuffix) {
        final Class<?> managedType = getManagedType(root);
        if (!DynamicEntityClassLoader.isGenerated(managedType)) {
            throw new DomainTreeException(format("The type for root [%s] is not generated. But it should be, because the same type on client application is generated and its suffix is [%s].", root.getSimpleName(), clientGeneratedTypeNameSuffix));
        }
        rootNameSuffix = of(clientGeneratedTypeNameSuffix);
        apply();
        return getManagedType(root);
    }
    
    @Override
    public Class<?> adjustManagedTypeAnnotations(final Class<?> root, final Annotation... additionalAnnotations) {
        validateManagedType(root);
        final Class<?> managedType = getManagedType(root);
        if (additionalAnnotations.length == 0) {
            logger.warn(format("\t\t\t\tEnded to adjustManagedTypeAnnotations for root [%s]. No annotations have been specified, root's managed type was not changed.", root.getSimpleName()));
            return managedType;
        }
        rootAnnotations.addAll(asList(additionalAnnotations));
        apply();
        return getManagedType(root);
    }
    
    /**
     * Replaces inner data with new data derived from <code>newManagedType</code>.
     * 
     * @param root
     * @param newManagedType
     * @return
     */
    private Class<?> adjustManagedType(final Class<?> root, final Class<?> newManagedType) {
        final DynamicEntityClassLoader classLoader = DynamicEntityClassLoader.getInstance(ClassLoader.getSystemClassLoader());
        
        final Map<String, ByteArray> byteArraysWithRenamedRoot = new LinkedHashMap<>();
        final Pair<Class<?>, Map<String, ByteArray>> currentByteArrays = originalAndEnhancedRootTypesAndArrays.get(root);
        byteArraysWithRenamedRoot.putAll(currentByteArrays.getValue());
        byteArraysWithRenamedRoot.put("", new ByteArray(classLoader.getCachedByteArray(newManagedType.getName())));
        originalAndEnhancedRootTypesAndArrays.put(root, new Pair<>(newManagedType, byteArraysWithRenamedRoot));
        
        return getManagedType(root);
    }
    
    @Override
    public Class<?> replaceManagedTypeBy(final Class<?> root, final Class<?> newManagedType) {
        validateManagedType(root);
        return adjustManagedType(root, newManagedType);
    }
    
    /**
     * Validates managed type in context of whether it can be adjusted with additional annotations.
     * 
     * @param root
     */
    private void validateManagedType(final Class<?> root) {
        if (!isGenerated(getManagedType(root))) {
            throw new DomainTreeException(format("The type for root [%s] is not generated. It is prohibited to generate additional annotations inside that type.", root.getSimpleName()));
        }
    }
    
    /**
     * Propagates recursively the <code>enhancedType</code> from place [root; path] to place [root; ""].
     *
     * @param enhancedType
     *            -- the type to replace the current type of property "path" in "root" type
     * @param root
     * @param path
     * @param classLoader
     * @return
     */
    private static Pair<Class<?>, Map<String, ByteArray>> propagateEnhancedTypeToRoot(final Class<?> enhancedType, final Class<?> root, final String path, final DynamicEntityClassLoader classLoader) {
        final Map<String, ByteArray> additionalByteArrays = new LinkedHashMap<>();
        // add a byte array corresponding to "enhancedType"
        additionalByteArrays.put(path, new ByteArray(classLoader.getCachedByteArray(enhancedType.getName())));

        if (StringUtils.isEmpty(path)) { // replace current root type with new one
            return pair(enhancedType, additionalByteArrays);
        }
        final Pair<Class<?>, String> transformed = PropertyTypeDeterminator.transform(root, path);

        final Class<?> typeToAdapt = transformed.getKey();
        final String nameOfThePropertyToAdapt = transformed.getValue();
        try {
            // change type if simple field and change signature in case of collectional field
            final boolean isCollectional = Collection.class.isAssignableFrom(PropertyTypeDeterminator.determineClass(transformed.getKey(), transformed.getValue(), true, false));
            final NewProperty propertyToBeModified = !isCollectional ? NewProperty.changeType(nameOfThePropertyToAdapt, enhancedType)
                    : NewProperty.changeTypeSignature(nameOfThePropertyToAdapt, enhancedType);
            final Class<?> nextEnhancedType = classLoader.startModification(typeToAdapt).modifyProperties(propertyToBeModified)./* TODO modifySupertypeName(nameOfTheTypeToAdapt).*/endModification();
            final String nextProp = PropertyTypeDeterminator.isDotNotation(path) ? PropertyTypeDeterminator.penultAndLast(path).getKey() : "";
            final Pair<Class<?>, Map<String, ByteArray>> lastTypeThatIsRootAndPropagatedArrays = propagateEnhancedTypeToRoot(nextEnhancedType, root, nextProp, classLoader);
            additionalByteArrays.putAll(lastTypeThatIsRootAndPropagatedArrays.getValue());

            return pair(lastTypeThatIsRootAndPropagatedArrays.getKey(), additionalByteArrays);
        } catch (final ClassNotFoundException e) {
            logger.error(e);
            throw new DomainTreeException("Could not propagate enahced type to root.", e);
        }
    }

    @Override
    public void discard() {
        //////////// Performs migration [originalAndEnhancedRootTypes => calculatedProperties] ////////////
        calculatedProperties.clear();
        calculatedProperties.putAll(extractAll(this, true));
        
        customProperties.clear(); // FIXME this piece of logic is not properly workable -- this must be considered when discard action will be used anywhere. Low priority for now.
    }

    /**
     * Extracts all calculated properties from enhanced root types.
     *
     * @param dte
     * @return
     */
    protected static Map<Class<?>, List<CalculatedProperty>> extractAll(final IDomainTreeEnhancer dte, final boolean validateTitleContextOfExtractedProperties) {
        final Map<Class<?>, List<CalculatedProperty>> newCalculatedProperties = new LinkedHashMap<>();
        for (final Entry<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> originalAndEnhancedAndArrays : dte.originalAndEnhancedRootTypesAndArrays().entrySet()) {
            final List<CalculatedProperty> calc = reload(originalAndEnhancedAndArrays.getValue().getKey(), originalAndEnhancedAndArrays.getKey(), "", dte, validateTitleContextOfExtractedProperties);
            for (final CalculatedProperty calculatedProperty : calc) {
                addCalculatedProperty(calculatedProperty, newCalculatedProperties);
            }
        }
        return newCalculatedProperties;
    }

    /**
     * Extracts recursively contextual <code>calculatedProperties</code> from enhanced domain <code>type</code>.
     *
     * @param type
     *            -- enhanced type to load properties
     * @param root
     *            -- not enhanced root type
     * @param path
     *            -- the path to loaded calculated props
     * @param dte
     */
    private static List<CalculatedProperty> reload(final Class<?> type, final Class<?> root, final String path, final IDomainTreeEnhancer dte, final boolean validateTitleContextOfExtractedProperties) {
        final List<CalculatedProperty> newCalcProperties = new ArrayList<>();
        if (!DynamicEntityClassLoader.isGenerated(type)) {
            return newCalcProperties;
        } else {
            // add all first level calculated properties if any exist
            for (final Field calculatedField : Finder.findRealProperties((Class<? extends AbstractEntity<?>>) type, Calculated.class)) {
                final Calculated calcAnnotation = AnnotationReflector.getAnnotation(calculatedField, Calculated.class);
                if (calcAnnotation != null && !StringUtils.isEmpty(calcAnnotation.value()) && AnnotationReflector.isContextual(calcAnnotation)) {
                    final Title titleAnnotation = AnnotationReflector.getAnnotation(calculatedField, Title.class);
                    final String title = titleAnnotation == null ? "" : titleAnnotation.value();
                    final String desc = titleAnnotation == null ? "" : titleAnnotation.desc();
                    final IsProperty isPropertyAnnotation = AnnotationReflector.getAnnotation(calculatedField, IsProperty.class);
                    final CalculatedProperty calculatedProperty = CalculatedProperty.createCorrect(dte.getFactory(), root, calcAnnotation.contextPath(), calcAnnotation.value(), title, desc, calcAnnotation.attribute(), "".equals(calcAnnotation.origination()) ? null
                            : calcAnnotation.origination(), isPropertyAnnotation.precision(), isPropertyAnnotation.scale(), dte, validateTitleContextOfExtractedProperties);

                    // TODO tricky setting!
                    if (!EntityUtils.equalsEx(calculatedField.getName(), calculatedProperty.name())) {
                        calculatedProperty.provideCustomPropertyName(calculatedField.getName());
                    }
                    calculatedProperty.setNameVeryTricky(calculatedField.getName());

                    newCalcProperties.add(calculatedProperty);
                }
            }
            // reload all "entity-typed" and "collectional entity-typed" sub-properties if they are enhanced
            for (final Field prop : Finder.findProperties(type)) {
                if (EntityUtils.isEntityType(prop.getType()) || EntityUtils.isCollectional(prop.getType())) {
                    final Class<?> propType = PropertyTypeDeterminator.determinePropertyType(type, prop.getName());
                    final String newPath = StringUtils.isEmpty(path) ? prop.getName() : (path + "." + prop.getName());
                    newCalcProperties.addAll(reload(propType, root, newPath, dte, validateTitleContextOfExtractedProperties));
                }
            }
            return newCalcProperties;
        }
    }

    /**
     * Checks whether calculated property with the suggested name exists (if it does not exist throws {@link IncorrectCalcPropertyException}) and return it.
     *
     * @param root
     * @param pathAndName
     * @return
     */
    protected final CalculatedProperty calculatedPropertyWhichShouldExist(final Class<?> root, final String pathAndName) {
        final CalculatedProperty calculatedProperty = calculatedProperty(root, pathAndName);
        if (calculatedProperty == null) {
            throw new IncorrectCalcPropertyException("The calculated property with path & name [" + pathAndName + "] does not exist in type [" + root + "].");
        }
        return calculatedProperty;
    }

    /**
     * Iterates through the set of calculated properties to find appropriate calc property.
     *
     * @param root
     * @param pathAndName
     * @return
     */
    protected final CalculatedProperty calculatedProperty(final Class<?> root, final String pathAndName) {
        return calculatedProperty(calculatedProperties.get(DynamicEntityClassLoader.getOriginalType(root)), pathAndName);
    }

    /**
     * Iterates through the set of calculated properties to find appropriate calc property.
     *
     * @param root
     * @param pathAndName
     * @return
     */
    protected static final CalculatedProperty calculatedProperty(final List<CalculatedProperty> calcProperties, final String pathAndName) {
        if (calcProperties != null) {
            for (final CalculatedProperty prop : calcProperties) {
                if (prop.pathAndName().equals(pathAndName)) {
                    return prop;
                }
            }
        }
        return null;
    }

    @Override
    public IDomainTreeEnhancer addCustomProperty(final Class<?> root, final String contextPath, final String name, final String title, final String desc, final Class<?> type, final Integer precision, final Integer scale) {
        addCustomProperty(new CustomProperty(root, getManagedType(root), contextPath, name, title, desc, type, precision, scale), customProperties);
        return this;
    }

    /**
     * Validates and adds custom property to a customProperties.
     *
     * @param customProperty
     * @param customProperties
     */
    private static void addCustomProperty(final CustomProperty customProperty, final Map<Class<?>, List<CustomProperty>> customProperties) {
        final Class<?> root = customProperty.getRoot();
        if (!customProperties.containsKey(root)) {
            customProperties.put(root, new ArrayList<CustomProperty>());
        }
        customProperties.get(root).add(customProperty);
    }

    /**
     * Validates and adds calc property to a calculatedProperties.
     *
     * @param calculatedProperty
     * @param calculatedProperties
     */
    private static ICalculatedProperty addCalculatedProperty(final CalculatedProperty calculatedProperty, final Map<Class<?>, List<CalculatedProperty>> calculatedProperties) {
        final Class<?> root = calculatedProperty.getRoot();
        if (!calculatedProperty.isValid().isSuccessful()) {
            throw calculatedProperty.isValid();
        }
        if (!calculatedProperties.containsKey(root)) {
            calculatedProperties.put(root, new ArrayList<CalculatedProperty>());
        }
        calculatedProperties.get(root).add(calculatedProperty);
        return /*calculatedProperty*/calculatedProperties.get(root).get(calculatedProperties.get(root).size() - 1);
    }

    @Override
    public ICalculatedProperty addCalculatedProperty(final ICalculatedProperty calculatedProperty) {
        return addCalculatedProperty((CalculatedProperty) calculatedProperty, calculatedProperties);
    }

    @Override
    public ICalculatedProperty addCalculatedProperty(final Class<?> root, final String contextPath, final String contextualExpression, final String title, final String desc, final CalculatedPropertyAttribute attribute, final String originationProperty, final Integer precision, final Integer scale) {
        return addCalculatedProperty(CalculatedProperty.createCorrect(getFactory(), root, contextPath, contextualExpression, title, desc, attribute, originationProperty, precision, scale, this));
    }

    @Override
    public ICalculatedProperty addCalculatedProperty(final Class<?> root, final String contextPath, final String customPropertyName, final String contextualExpression, final String title, final String desc, final CalculatedPropertyAttribute attribute, final String originationProperty, final Integer precision, final Integer scale) {
        return addCalculatedProperty(CalculatedProperty.createCorrect(getFactory(), root, contextPath, customPropertyName, contextualExpression, title, desc, attribute, originationProperty, precision, scale, this));
    }

    @Override
    public ICalculatedProperty getCalculatedProperty(final Class<?> rootType, final String calculatedPropertyName) {
        return calculatedPropertyWhichShouldExist(rootType, calculatedPropertyName);
    }

    @Override
    public ICalculatedProperty copyCalculatedProperty(final Class<?> rootType, final String calculatedPropertyName) {
        return calculatedPropertyWhichShouldExist(rootType, calculatedPropertyName).copy();
    }

    @Override
    public void removeCalculatedProperty(final Class<?> rootType, final String calculatedPropertyName) {
        final CalculatedProperty calculatedProperty = calculatedPropertyWhichShouldExist(rootType, calculatedPropertyName);

        /////////////////
        final List<CalculatedProperty> calcs = calculatedProperties.get(rootType);
        boolean existsInOtherExpressionsAsOriginationProperty = false;
        String containingExpression = null;
        for (final CalculatedProperty calc : calcs) {
            if (StringUtils.equals(calc.getOriginationProperty(), Reflector.fromAbsotule2RelativePath(calc.getContextPath(), calculatedPropertyName))) {
                existsInOtherExpressionsAsOriginationProperty = true;
                containingExpression = calc.pathAndName();
                break;
            }
        }
        if (existsInOtherExpressionsAsOriginationProperty) {
            throw new DomainTreeException("Cannot remove a property that exists in other expressions as 'origination' property. See property [" + containingExpression + "].");
        }
        /////////////////

        final boolean removed = calculatedProperties.get(rootType).remove(calculatedProperty);

        if (!removed) {
            throw new IllegalStateException("The property [" + calculatedPropertyName + "] has been validated but can not be removed.");
        }
        if (calculatedProperties.get(rootType).isEmpty()) {
            calculatedProperties.remove(rootType);
        }
    }

    @Override
    public Set<Class<?>> rootTypes() {
        return new LinkedHashSet<>(originalAndEnhancedRootTypesAndArrays.keySet());
    }

    //    /**
    //     * Extracts only <b>enhanced</b> type's arrays mapped to original types.
    //     *
    //     * @return
    //     */
    //    private static Map<Class<?>, Map<String, ByteArray>> originalTypesAndEnhancedArrays(final Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> originalAndEnhancedRootTypesAndArrays) {
    //	final Map<Class<?>, Map<String, ByteArray>> originalTypesAndEnhancedArrays = new LinkedHashMap<Class<?>, Map<String, ByteArray>>();
    //	for (final Entry<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> entry : originalAndEnhancedRootTypesAndArrays.entrySet()) {
    //	    if (!entry.getValue().getValue().isEmpty()) {
    //		originalTypesAndEnhancedArrays.put(entry.getKey(), new LinkedHashMap<String, ByteArray>(entry.getValue().getValue()));
    //	    }
    //	}
    //	return originalTypesAndEnhancedArrays;
    //    }
    //
    //    /**
    //     * Extracts only <b>enhanced</b> type's arrays mapped to original types.
    //     *
    //     * @return
    //     */
    //    private Map<Class<?>, Map<String, ByteArray>> originalTypesAndEnhancedArrays() {
    //	return originalTypesAndEnhancedArrays(originalAndEnhancedRootTypesAndArrays);
    //    }

    /**
     * A current snapshot of calculated properties, possibly not applied.
     *
     * @return
     */
    @Override
    public Map<Class<?>, List<CalculatedProperty>> calculatedProperties() {
        return calculatedProperties;
    }

    /**
     * Extracts {@link CalculatedPropertyInfo} instances from current snapshot of {@link #calculatedProperties()}.
     * <p>
     * Warning: it is necessary to have applied all changes.
     *
     * @return
     */
    protected Map<Class<?>, Set<CalculatedPropertyInfo>> calculatedPropertiesInfo() {
        final Map<Class<?>, Set<CalculatedPropertyInfo>> map = new LinkedHashMap<>();
        for (final Class<?> root: rootTypes()) {
            map.put(root, new LinkedHashSet<>());
        }
        for (final Entry<Class<?>, List<CalculatedProperty>> entry : calculatedProperties.entrySet()) {
            final Set<CalculatedPropertyInfo> set = new HashSet<>();
            for (final CalculatedProperty cp : entry.getValue()) {
                set.add(new CalculatedPropertyInfo(cp.getRoot(), cp.getContextPath(), cp.getCustomPropertyName(), cp.getContextualExpression(), cp.getTitle(), cp.getAttribute(), cp.getOriginationProperty(), cp.getDesc(), cp.getPrecision(), cp.getScale()));
            }
            map.put(entry.getKey(), set);
        }
        return map;
    }

    /**
     * Creates calculated properties from raw {@link CalculatedPropertyInfo} instances.
     * <p>
     * Created calculated properties are fully dependent on "dte" {@link DomainTreeEnhancer}. Also validation performs to be sure that all is okay with deserialised (or created
     * from scratch) "dte" {@link DomainTreeEnhancer}.
     *
     * @return
     */
    private static Map<Class<?>, List<CalculatedProperty>> createCalculatedPropertiesFrom(final DomainTreeEnhancer dte, final Map<Class<?>, Set<CalculatedPropertyInfo>> calculatedPropertiesInfo) {
        final Map<Class<?>, List<CalculatedProperty>> map = new LinkedHashMap<>();
        for (final Entry<Class<?>, Set<CalculatedPropertyInfo>> entry : calculatedPropertiesInfo.entrySet()) {
            final List<CalculatedProperty> list = new ArrayList<>();
            for (final CalculatedPropertyInfo cpInfo : entry.getValue()) {
                list.add(CalculatedProperty.createCorrect(dte.getFactory(), cpInfo.getRoot(), cpInfo.getContextPath(), cpInfo.getCustomPropertyName(), cpInfo.getContextualExpression(), cpInfo.getTitle(), cpInfo.getDesc(), cpInfo.getAttribute(), cpInfo.getOriginationProperty(), cpInfo.getPrecision(), cpInfo.getScale(), dte, true));
            }
            map.put(entry.getKey(), list);
        }
        return map;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        // IMPORTANT : rootTypes() and calculatedPropertiesInfo() are the mirror for "calculatedProperties".
        // So they should be used for serialisation, comparison and hashCode() implementation.
        result = prime * result + rootTypes().hashCode();
        result = prime * result + calculatedPropertiesInfo().hashCode();
        result = prime * result + customProperties().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DomainTreeEnhancer other = (DomainTreeEnhancer) obj;
        // IMPORTANT : rootTypes() and calculatedPropertiesInfo() are the mirror for "calculatedProperties".
        // So they should be used for serialisation, comparison and hashCode() implementation.
        return rootTypes().equals(other.rootTypes()) && calculatedPropertiesInfo().equals(other.calculatedPropertiesInfo()) && customProperties().equals(other.customProperties());
    }

    @Override
    public Map<Class<?>, Pair<Class<?>, Map<String, ByteArray>>> originalAndEnhancedRootTypesAndArrays() {
        return originalAndEnhancedRootTypesAndArrays;
    }

    /**
     * Returns an entity factory that is essential for inner {@link AbstractEntity} instances (e.g. calculated properties) creation.
     *
     * @return
     */
    @Override
    public EntityFactory getFactory() {
        return super.getFactory();
    }

    @Override
    public Map<Class<?>, List<CustomProperty>> customProperties() {
        return customProperties;
    }
}
