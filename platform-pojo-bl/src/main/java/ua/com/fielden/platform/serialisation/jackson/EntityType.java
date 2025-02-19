package ua.com.fielden.platform.serialisation.jackson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.annotation.CompanionObject;
import ua.com.fielden.platform.entity.annotation.IsProperty;
import ua.com.fielden.platform.entity.annotation.KeyTitle;
import ua.com.fielden.platform.entity.annotation.KeyType;
import ua.com.fielden.platform.entity.annotation.Observable;
import ua.com.fielden.platform.entity.annotation.Title;
import ua.com.fielden.platform.master.MasterInfo;

/**
 * The entity type to represent serialisable entity types for client-side handling.
 * `tg-reflector` defines how this data are interpreted.
 * <p>
 * <code>null</code> values are not serialised. This can be used to reduce resultant JSON.
 *
 * @author TG Team
 *
 */
@KeyType(String.class)
@KeyTitle(value = "Entity Type Name", desc = "Entity Type Name description")
@CompanionObject(IEntityType.class)
public class EntityType extends AbstractEntity<String> {
    @IsProperty
    @Title(value = "Identifier", desc = "Identifier of the type in context of other types for serialisation")
    private String _identifier;

    @IsProperty(String.class)
    @Title(value = "Composite Keys", desc = "Composite key property names")
    private final List<String> _compositeKeyNames = new ArrayList<>();

    @IsProperty
    @Title(value = "Composite Key Separator", desc = "Separator for composite key members (for autocompletion)")
    private String _compositeKeySeparator;

    @IsProperty
    @Title(value = "Entity Title", desc = "Entity title")
    private String _entityTitle;

    @IsProperty
    @Title(value = "Entity Desc", desc = "Entity description")
    private String _entityDesc;

    @IsProperty(EntityTypeProp.class)
    @Title(value = "Entity Type Properties", desc = "A map of entity type properties by their names")
    private final Map<String, EntityTypeProp> _props = new LinkedHashMap<>();

    @IsProperty
    @Title(value = "Is Persistent?", desc = "Indicated whether the associated entity type represents a persistent entity.")
    private Boolean _persistent;

    @IsProperty
    @Title(value = "Should Display Description?", desc = "Indicates whether editors for values of this type should display values descriptions")
    private Boolean _displayDesc;

    @IsProperty
    @Title(value = "Is Continuation?", desc = "Indicates whether the associated entity type represents a continuation entity.")
    private Boolean _continuation;

    @IsProperty
    @Title(value = "Union Common Properties", desc = "The list of common properties (can be empty) in case if the associated entity type represents union entity type; null otherwise.")
    private List<String> _unionCommonProps; // intentionally null (i.e. not serialised) to differentiate between [empty set of common properties for union entity type] and [non-union entity type]

    @IsProperty
    @Title(value = "Compound Opener Type", desc = "Represents main persistent type for this compound master opener (if it is of such kind, empty otherwise).")
    private String _compoundOpenerType;

    @IsProperty
    @Title(value = "Is Compound Menu Item?", desc = "Indicates whether the associated entity type represents menu item entity in compound master.")
    private Boolean _compoundMenuItem;

    @IsProperty
    @Title(value = "Entity Master", desc = "Entity Master Data")
    private MasterInfo _entityMaster;

    @IsProperty
    @Title(value = "New Entity Master", desc = "Entity master data for new entity action")
    private MasterInfo _newEntityMaster;

    @Observable
    public EntityType set_newEntityMaster(final MasterInfo _newEntityMaster) {
        this._newEntityMaster = _newEntityMaster;
        return this;
    }

    public MasterInfo get_newEntityMaster() {
        return _newEntityMaster;
    }

    @Observable
    public EntityType set_entityMaster(final MasterInfo _entityMaster) {
        this._entityMaster = _entityMaster;
        return this;
    }

    public MasterInfo get_entityMaster() {
        return _entityMaster;
    }

    @Observable
    public EntityType set_compoundMenuItem(final Boolean _compoundMenuItem) {
        this._compoundMenuItem = _compoundMenuItem;
        return this;
    }

    public Boolean is_compoundMenuItem() {
        return _compoundMenuItem;
    }

    @Observable
    public EntityType set_compoundOpenerType(final String value) {
        this._compoundOpenerType = value;
        return this;
    }

    public String get_compoundOpenerType() {
        return _compoundOpenerType;
    }

    @Observable
    public EntityType set_unionCommonProps(final List<String> value) {
        this._unionCommonProps = value;
        return this;
    }

    public List<String> get_unionCommonProps() {
        return _unionCommonProps;
    }

    @Observable
    public EntityType set_displayDesc(final Boolean _displayDesc) {
        this._displayDesc = _displayDesc;
        return this;
    }

    public Boolean get_displayDesc() {
        return _displayDesc;
    }

    @Observable
    public EntityType set_persistent(final Boolean _persistent) {
        this._persistent = _persistent;
        return this;
    }

    public Boolean is_persistent() {
        return _persistent;
    }

    @Observable
    public EntityType set_continuation(final Boolean _continuation) {
        this._continuation = _continuation;
        return this;
    }

    public Boolean is_continuation() {
        return _continuation;
    }

    @Observable
    protected EntityType set_props(final Map<String, EntityTypeProp> _props) {
        this._props.clear();
        this._props.putAll(_props);
        return this;
    }

    public Map<String, EntityTypeProp> get_props() {
        return Collections.unmodifiableMap(_props);
    }

    @Observable
    public EntityType set_entityDesc(final String entityDesc) {
        this._entityDesc = entityDesc;
        return this;
    }

    public String get_entityDesc() {
        return _entityDesc;
    }

    @Observable
    public EntityType set_entityTitle(final String entityTitle) {
        this._entityTitle = entityTitle;
        return this;
    }

    public String get_entityTitle() {
        return _entityTitle;
    }

    @Observable
    public EntityType set_compositeKeySeparator(final String _compositeKeySeparator) {
        this._compositeKeySeparator = _compositeKeySeparator;
        return this;
    }

    public String get_compositeKeySeparator() {
        return _compositeKeySeparator;
    }

    @Observable
    protected EntityType set_compositeKeyNames(final List<String> _compositeKeyNames) {
        this._compositeKeyNames.clear();
        this._compositeKeyNames.addAll(_compositeKeyNames);
        return this;
    }

    public List<String> get_compositeKeyNames() {
        return Collections.unmodifiableList(_compositeKeyNames);
    }

    @Observable
    public EntityType set_identifier(final String _identifier) {
        this._identifier = _identifier;
        return this;
    }

    public String get_identifier() {
        return _identifier;
    }
}