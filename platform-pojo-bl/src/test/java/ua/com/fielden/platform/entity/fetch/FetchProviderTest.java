package ua.com.fielden.platform.entity.fetch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ua.com.fielden.platform.entity.AbstractEntity.DESC;
import static ua.com.fielden.platform.entity.AbstractEntity.ID;
import static ua.com.fielden.platform.entity.AbstractEntity.KEY;
import static ua.com.fielden.platform.entity.AbstractEntity.VERSION;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.fetchKeyAndDescOnly;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.fetchNone;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.fetchNoneAndInstrument;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.fetchOnly;
import static ua.com.fielden.platform.utils.CollectionUtil.linkedSetOf;
import static ua.com.fielden.platform.utils.CollectionUtil.setOf;
import static ua.com.fielden.platform.utils.EntityUtils.fetchWithKeyAndDesc;

import org.junit.Test;

import ua.com.fielden.platform.entity.meta.PropertyDescriptor;
import ua.com.fielden.platform.sample.domain.TgAuthor;
import ua.com.fielden.platform.sample.domain.TgAuthorRoyalty;
import ua.com.fielden.platform.sample.domain.TgAuthorship;
import ua.com.fielden.platform.sample.domain.TgAverageFuelUsage;
import ua.com.fielden.platform.sample.domain.TgPersistentCompositeEntity;
import ua.com.fielden.platform.sample.domain.TgPersistentEntityWithProperties;
import ua.com.fielden.platform.sample.domain.TgPersonName;
import ua.com.fielden.platform.sample.domain.TgReVehicleModel;
import ua.com.fielden.platform.sample.domain.TgUnion;
import ua.com.fielden.platform.sample.domain.TgUnionCommonType;
import ua.com.fielden.platform.sample.domain.TgUnionHolder;
import ua.com.fielden.platform.sample.domain.TgUnionType1;
import ua.com.fielden.platform.sample.domain.TgUnionType2;
import ua.com.fielden.platform.security.user.User;
import ua.com.fielden.platform.utils.EntityUtils;

public class FetchProviderTest {

    @Test
    public void empty_fetch_provider_generates_empty_fetch_only_model() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
                EntityUtils.fetch(TgPersistentEntityWithProperties.class);

        assertFalse("Incorrect shouldFetch for property.", fp.shouldFetch("integerProp"));
        assertFalse("Incorrect shouldFetch for property.", fp.shouldFetch("entityProp"));

        try {
            fp.fetchFor("integerProp");
            fail("Should be not applicable for the properties of non-entity type.");
        } catch (final IllegalArgumentException e) {
        }
        try {
            fp.fetchFor("entityProp");
            fail("Should fail with exception 'should be not be fetched as defined in fetch provider'.");
        } catch (final IllegalStateException e) {
        }

        assertEquals("Incorrect allProperties list.", linkedSetOf(), fp.allProperties());
        assertEquals("Incorrect fetch model has been generated.", fetchOnly(TgPersistentEntityWithProperties.class), fp.fetchModel());
    }

    @Test
    public void keyAndDesc_fetch_provider_generates_keyAndDescOnly_fetch_model() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetchWithKeyAndDesc(TgPersistentEntityWithProperties.class);

        assertFalse("Incorrect shouldFetch for property.", fp.shouldFetch("integerProp"));
        assertFalse("Incorrect shouldFetch for property.", fp.shouldFetch("entityProp"));

        try {
            fp.fetchFor("integerProp");
            fail("Should be not applicable for the properties of non-entity type.");
        } catch (final IllegalArgumentException e) {
        }
        try {
            fp.fetchFor("entityProp");
            fail("Should fail with exception 'should be not be fetched as defined in fetch provider'.");
        } catch (final IllegalStateException e) {
        }

        assertEquals("Incorrect allProperties list.", linkedSetOf(), fp.allProperties());
        assertEquals("Incorrect fetch model has been generated.", fetchKeyAndDescOnly(TgPersistentEntityWithProperties.class), fp.fetchModel());
    }
    
    
    @Test
    public void fetch_provider_generates_instrumented_fetch_model() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
                EntityUtils.fetch(TgPersistentEntityWithProperties.class);

        assertFalse("Should not be intrumented.", fp.instrumented());
    }

    @Test
    public void fetch_provider_withKeyAndDesc_generates_instrumented_fetch_model() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
                EntityUtils.fetchWithKeyAndDesc(TgPersistentEntityWithProperties.class);

        assertFalse("Should not be instrumented.", fp.instrumented());
    }
    
    @Test
    public void fetch_provider_with_entity_typed_property_generates_uninstrumented_fetch_submodel() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
                EntityUtils.fetchWithKeyAndDesc(TgPersistentEntityWithProperties.class)
                .with("entityProp");

        assertFalse("Only the root entity should be intrumented.", fp.fetchFor("entityProp").instrumented());
        assertEquals("Incorrect property provider.", EntityUtils.fetchWithKeyAndDesc(TgPersistentEntityWithProperties.class, false), fp.fetchFor("entityProp"));
        assertEquals("Incorrect fetch model has been generated.",
                fetchKeyAndDescOnly(TgPersistentEntityWithProperties.class),
                fp.fetchFor("entityProp").fetchModel());
    }
    
    @Test
    public void fetch_provider_with_propertyDescriptor_typed_property_generates_fetch_model_with_regular_property() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
            fetchWithKeyAndDesc(TgPersistentEntityWithProperties.class)
            .with("propertyDescriptorProp");
        
        assertFalse("Only the root entity should be intrumented.", fp.fetchFor("propertyDescriptorProp").instrumented());
        assertEquals("Incorrect property provider.", EntityUtils.fetchWithKeyAndDesc(PropertyDescriptor.class, false), fp.fetchFor("propertyDescriptorProp"));
        assertEquals("Incorrect fetch model has been generated.",
            fetchKeyAndDescOnly(TgPersistentEntityWithProperties.class).with("propertyDescriptorProp"),
            fp.fetchModel());
    }
    
    @Test
    public void fetch_provider_with_entity_typed_property_concrete_provider_generates_concrete_fetch_submodel_with_concrete_instrumentation() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
                EntityUtils.fetchWithKeyAndDesc(TgPersistentEntityWithProperties.class)
                .with("entityProp", EntityUtils.fetchNotInstrumentedWithKeyAndDesc(TgPersistentEntityWithProperties.class));

        assertFalse("Should be not intrumented.", fp.fetchFor("entityProp").instrumented());
        assertEquals("Incorrect property provider.", EntityUtils.fetchNotInstrumentedWithKeyAndDesc(TgPersistentEntityWithProperties.class), fp.fetchFor("entityProp"));
        assertEquals("Incorrect fetch model has been generated.",
                fetchKeyAndDescOnly(TgPersistentEntityWithProperties.class),
                fp.fetchFor("entityProp").fetchModel());
    }

    @Test
    public void fetch_provider_with_regular_property_generates_fetch_model_with_regular_property() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
                EntityUtils.fetch(TgPersistentEntityWithProperties.class).
                        with("integerProp");

        assertTrue("Incorrect shouldFetch for property.", fp.shouldFetch("integerProp"));

        try {
            fp.fetchFor("integerProp");
            fail("Should be not applicable for the properties of non-entity type.");
        } catch (final IllegalArgumentException e) {
        }

        assertEquals("Incorrect allProperties list.", linkedSetOf("integerProp"), fp.allProperties());
        assertEquals("Incorrect fetch model has been generated.",
                fetchOnly(TgPersistentEntityWithProperties.class).with("integerProp"),
                fp.fetchModel());
    }

    @Test
    public void fetch_provider_with_entity_typed_property_generates_fetch_model_with_keyAndDesc_submodel() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
                EntityUtils.fetch(TgPersistentEntityWithProperties.class).with("entityProp");

        assertTrue("Incorrect shouldFetch for property.", fp.shouldFetch("entityProp"));

        assertEquals("Incorrect property provider.", EntityUtils.fetchWithKeyAndDesc(TgPersistentEntityWithProperties.class, false), fp.fetchFor("entityProp"));

        assertEquals("Incorrect allProperties list.", linkedSetOf("entityProp"), fp.allProperties());
        assertEquals("Incorrect fetch model has been generated.",
                fetchOnly(TgPersistentEntityWithProperties.class).
                with("entityProp", fetchKeyAndDescOnly(TgPersistentEntityWithProperties.class)),
                fp.fetchModel());
    }

    @Test
    public void fetch_provider_with_composite_entity_typed_property_generates_fetch_model_with_keyAndDesc_submodel() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
                EntityUtils.fetch(TgPersistentEntityWithProperties.class).with("compositeProp");

        assertTrue("Incorrect shouldFetch for property.", fp.shouldFetch("compositeProp"));

        assertEquals("Incorrect property provider.", EntityUtils.fetchWithKeyAndDesc(TgPersistentCompositeEntity.class, false), fp.fetchFor("compositeProp"));

        assertEquals("Incorrect allProperties list.", linkedSetOf("compositeProp"), fp.allProperties());
        assertEquals("Incorrect fetch model has been generated.",
                fetchOnly(TgPersistentEntityWithProperties.class).
                with("compositeProp", fetchKeyAndDescOnly(TgPersistentCompositeEntity.class)),
                fp.fetchModel());
    }

    @Test
    public void fetch_provider_with_concrete_property_provider_generates_fetch_model_with_concrete_submodel() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp =
                EntityUtils.fetch(TgPersistentEntityWithProperties.class).
                        with("compositeProp",
                                EntityUtils.fetch(TgPersistentCompositeEntity.class).
                                        with("key1", EntityUtils.fetch(TgPersistentEntityWithProperties.class))
                        );

        assertTrue("Incorrect shouldFetch for property.", fp.shouldFetch("compositeProp"));
        assertTrue("Incorrect shouldFetch for property.", fp.shouldFetch("compositeProp.key1"));
        assertFalse("Incorrect shouldFetch for property.", fp.shouldFetch("compositeProp.key2"));

        assertEquals("Incorrect property provider.",
                EntityUtils.fetch(TgPersistentCompositeEntity.class)
                        .with("key1", EntityUtils.fetch(TgPersistentEntityWithProperties.class)),
                fp.fetchFor("compositeProp"));

        assertEquals("Incorrect property provider.",
                EntityUtils.fetch(TgPersistentEntityWithProperties.class),
                fp.fetchFor("compositeProp.key1"));

        assertEquals("Incorrect allProperties list.", linkedSetOf("compositeProp", "compositeProp.key1"), fp.allProperties());
        assertEquals("Incorrect fetch model has been generated.",
                fetchOnly(TgPersistentEntityWithProperties.class).
                        with("compositeProp",
                                fetchOnly(TgPersistentCompositeEntity.class).
                                        with("key1", fetchOnly(TgPersistentEntityWithProperties.class))),
                fp.fetchModel());
    }

    @Test
    public void merged_fetch_providers_generates_appropriate_fetch_model() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp1 =
                EntityUtils.fetch(TgPersistentEntityWithProperties.class).
                        with("compositeProp",
                                EntityUtils.fetch(TgPersistentCompositeEntity.class).
                                        with("key1", EntityUtils.fetch(TgPersistentEntityWithProperties.class))
                        );

        final IFetchProvider<TgPersistentEntityWithProperties> fp2 =
                EntityUtils.fetch(TgPersistentEntityWithProperties.class).
                        with("compositeProp.key2");
        final IFetchProvider<TgPersistentEntityWithProperties> fp = fp1.with(fp2);

        assertTrue("Incorrect shouldFetch for property.", fp.shouldFetch("compositeProp"));
        assertTrue("Incorrect shouldFetch for property.", fp.shouldFetch("compositeProp.key1"));
        assertTrue("Incorrect shouldFetch for property.", fp.shouldFetch("compositeProp.key2"));

        // during the merge 'keyAndDesc' provider should have the priority over 'empty' provider
        assertEquals("Incorrect property provider.",
                EntityUtils.fetchWithKeyAndDesc(TgPersistentCompositeEntity.class)
                        .with("key1", EntityUtils.fetch(TgPersistentEntityWithProperties.class))
                        .with("key2"),
                fp.fetchFor("compositeProp"));

        assertEquals("Incorrect property provider.",
                EntityUtils.fetch(TgPersistentEntityWithProperties.class),
                fp.fetchFor("compositeProp.key1"));

        assertEquals("Incorrect allProperties list.", linkedSetOf("compositeProp", "compositeProp.key1", "compositeProp.key2"), fp.allProperties());
        assertEquals("Incorrect fetch model has been generated.",
                fetchOnly(TgPersistentEntityWithProperties.class).
                        with("compositeProp",
                                fetchKeyAndDescOnly(TgPersistentCompositeEntity.class).
                                        with("key1", fetchOnly(TgPersistentEntityWithProperties.class)).
                                        with("key2"))
                ,
                fp.fetchModel());
    }
    
    @Test
    public void fetchNone_provider_generates_uninstrumented_fetchNone_EQL_model() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetchNone(TgPersistentEntityWithProperties.class);
        
        assertEquals(linkedSetOf(), fp.allProperties());
        assertEquals(fetchNone(TgPersistentEntityWithProperties.class), fp.fetchModel());
    }
    
    @Test
    public void fetchNone_provider_with_instrumentation_generates_instrumented_fetchNone_EQL_model() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetchNone(TgPersistentEntityWithProperties.class, true);
        
        assertEquals(linkedSetOf(), fp.allProperties());
        assertEquals(fetchNoneAndInstrument(TgPersistentEntityWithProperties.class), fp.fetchModel());
    }
    
    @Test
    public void fetchNone_provider_generates_fetchNone_EQL_model_with_regular_property() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetchNone(TgPersistentEntityWithProperties.class).with("integerProp");
        
        assertEquals(linkedSetOf("integerProp"), fp.allProperties());
        assertEquals(fetchNone(TgPersistentEntityWithProperties.class).with("integerProp"), fp.fetchModel());
    }
    
    @Test
    public void fetchNone_provider_generates_fetchNone_EQL_model_with_concrete_subprovider() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = 
            EntityUtils.fetchNone(TgPersistentEntityWithProperties.class)
                .with("entityProp", fetchWithKeyAndDesc(TgPersistentEntityWithProperties.class));
        
        assertEquals(linkedSetOf("entityProp"), fp.allProperties());
        assertEquals(fetchNone(TgPersistentEntityWithProperties.class).with("entityProp", fetchKeyAndDescOnly(TgPersistentEntityWithProperties.class)), fp.fetchModel());
    }
    
    @Test
    public void fetchNone_provider_generates_fetchNone_EQL_model_with_default_NONE_subprovider() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetchNone(TgPersistentEntityWithProperties.class).with("entityProp");
        
        assertEquals(linkedSetOf("entityProp"), fp.allProperties());
        assertEquals(fetchNone(TgPersistentEntityWithProperties.class).with("entityProp", fetchNone(TgPersistentEntityWithProperties.class)), fp.fetchModel());
    }
    
    @Test
    public void fetchNone_provider_generates_fetchNone_EQL_model_with_default_NONE_subproviders_for_dotNotated_property_and_concrete_subprovider() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetchNone(TgPersistentEntityWithProperties.class).with("entityProp.entityProp.entityProp", EntityUtils.fetch(TgPersistentEntityWithProperties.class));
        
        assertEquals(linkedSetOf("entityProp", "entityProp.entityProp", "entityProp.entityProp.entityProp"), fp.allProperties());
        assertEquals(
            fetchNone(TgPersistentEntityWithProperties.class)
                .with("entityProp", fetchNone(TgPersistentEntityWithProperties.class)
                        .with("entityProp", fetchNone(TgPersistentEntityWithProperties.class)
                                .with("entityProp", fetchOnly(TgPersistentEntityWithProperties.class)))),
            fp.fetchModel()
        );
    }
    
    @Test
    public void fetchNone_provider_generates_fetchNone_EQL_model_with_default_NONE_subproviders_for_dotNotated_property() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetchNone(TgPersistentEntityWithProperties.class).with("entityProp.entityProp.entityProp");
        
        assertEquals(linkedSetOf("entityProp", "entityProp.entityProp", "entityProp.entityProp.entityProp"), fp.allProperties());
        assertEquals(
            fetchNone(TgPersistentEntityWithProperties.class)
                .with("entityProp", fetchNone(TgPersistentEntityWithProperties.class)
                        .with("entityProp", fetchNone(TgPersistentEntityWithProperties.class)
                                .with("entityProp", fetchNone(TgPersistentEntityWithProperties.class)))),
            fp.fetchModel()
        );
    }
    
    /////////////////////////////////////////////////////////// addKeysTo method: ///////////////////////////////////////////////////////////
    
    // Adding keys to root of the tree:
    @Test(expected = FetchProviderException.class)
    public void keys_cannot_be_added_into_provider_with_category_other_than_NONE() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetch(TgPersistentEntityWithProperties.class);
        fp.addKeysTo("", false);
    }
    
    @Test
    public void single_regular_key_is_added_into_provider_for_entity_with_such_key() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetchNone(TgPersistentEntityWithProperties.class);
        fp.addKeysTo("", false);
        
        assertEquals(linkedSetOf(KEY, ID, VERSION), fp.allProperties()); // persistent entity type also requires ID and VERSION
    }
    
    @Test
    public void single_regular_key_and_description_are_both_added_into_provider_for_entity_with_such_key_and_desc() {
        final IFetchProvider<TgPersistentEntityWithProperties> fp = EntityUtils.fetchNone(TgPersistentEntityWithProperties.class);
        fp.addKeysTo("", true);
        
        assertEquals(linkedSetOf(KEY, ID, VERSION, DESC), fp.allProperties()); // persistent entity type also requires ID and VERSION
    }
    
    @Test
    public void single_regular_key_is_added_into_provider_for_synthetic_based_on_persistent_entity_with_such_key() {
        final IFetchProvider<TgReVehicleModel> fp = EntityUtils.fetchNone(TgReVehicleModel.class);
        fp.addKeysTo("", false);
        
        assertEquals(linkedSetOf(KEY, ID), fp.allProperties()); // synthetic entity type based on persistent type requires ID
    }
    
    @Test
    public void entity_typed_key_with_own_keys_are_added_into_provider_for_synthetic_entity_with_such_key() {
        final IFetchProvider<TgAverageFuelUsage> fp = EntityUtils.fetchNone(TgAverageFuelUsage.class);
        fp.addKeysTo("", false);
        
        assertEquals(linkedSetOf(KEY, // synthetic entity type does not require neither ID nor VERSION
            KEY + "." + KEY, KEY + "." + ID, KEY + "." + VERSION // subkey and ID / VERSION for key of persistent TgVehicle type 
        ), fp.allProperties());
    }
    
    @Test
    public void composite_keys_with_own_keys_are_added_into_provider_for_composite_entity_with_such_keys() {
        final IFetchProvider<TgAuthor> fp = EntityUtils.fetchNone(TgAuthor.class);
        fp.addKeysTo("", false);
        
        assertEquals(linkedSetOf("name", "name." + KEY, "name." + ID, "name." + VERSION, // subkey and ID / VERSION for 'name' of persistent TgPersonName type
                "surname", "patronymic",
                ID, VERSION
        ), fp.allProperties());
    }
    
    // Adding keys to first-level property:
    @Test(expected = FetchProviderException.class)
    public void key_cannot_be_added_into_property_subprovider_if_does_not_exist() {
        final IFetchProvider<TgAuthor> fp = EntityUtils.fetchNone(TgAuthor.class);
        fp.addKeysTo("name", false);
    }
    
    @Test(expected = FetchProviderException.class)
    public void key_cannot_be_added_into_property_subprovider_if_it_has_category_other_than_NONE() {
        final IFetchProvider<TgAuthor> fp = EntityUtils.fetchNone(TgAuthor.class).with("name", EntityUtils.fetch(TgPersonName.class));
        fp.addKeysTo("name", false);
    }
    
    @Test
    public void key_is_added_into_property_subprovider() {
        final IFetchProvider<TgAuthor> fp = EntityUtils.fetchNone(TgAuthor.class).with("name");
        fp.addKeysTo("name", false);
        
        assertEquals(linkedSetOf("name", "name." + KEY, "name." + ID, "name." + VERSION // subkey and ID / VERSION for 'name' property of persistent TgPersonName type
        ), fp.allProperties());
    }
    
    @Test
    public void both_key_and_desc_are_added_into_property_subprovider() {
        final IFetchProvider<TgAuthor> fp = EntityUtils.fetchNone(TgAuthor.class).with("name");
        fp.addKeysTo("name", true);
        
        assertEquals(linkedSetOf("name", "name." + KEY, "name." + ID, "name." + VERSION, "name." + DESC // subkey and ID / VERSION for 'name' property of persistent TgPersonName type
        ), fp.allProperties());
    }
    
    @Test
    public void adding_key_and_desc_to_regular_property_does_not_affect_provider() {
        final IFetchProvider<TgAuthor> fp = EntityUtils.fetchNone(TgAuthor.class).with("surname");
        fp.addKeysTo("surname", true);
        
        assertEquals(linkedSetOf("surname"), fp.allProperties());
    }
    
    // Adding keys to deep-level property:
    @Test(expected = FetchProviderException.class)
    public void key_cannot_be_added_into_deep_property_subprovider_if_does_not_exist() {
        final IFetchProvider<TgAuthorRoyalty> fp = EntityUtils.fetchNone(TgAuthorRoyalty.class);
        fp.addKeysTo("name", false);
    }
    
    @Test(expected = FetchProviderException.class)
    public void key_cannot_be_added_into_deep_property_subprovider_if_it_has_category_other_than_NONE() {
        final IFetchProvider<TgAuthorRoyalty> fp = EntityUtils.fetchNone(TgAuthorRoyalty.class).with("authorship.author.name", EntityUtils.fetch(TgPersonName.class));
        fp.addKeysTo("authorship.author.name", false);
    }
    
    @Test
    public void key_is_added_into_deep_property_subprovider() {
        final IFetchProvider<TgAuthorRoyalty> fp = EntityUtils.fetchNone(TgAuthorRoyalty.class).with("authorship.author.name");
        fp.addKeysTo("authorship.author.name", false);
        
        assertEquals(linkedSetOf("authorship", "authorship." + ID, "authorship." + VERSION,
                "authorship.author", "authorship.author." + ID, "authorship.author." + VERSION,
                "authorship.author.name", "authorship.author.name." + KEY, "authorship.author.name." + ID, "authorship.author.name." + VERSION // subkey and ID / VERSION for 'name' property of persistent TgPersonName type
        ), fp.allProperties());
    }
    
    @Test
    public void key_and_desc_are_both_added_into_deep_property_subprovider() {
        final IFetchProvider<TgAuthorRoyalty> fp = EntityUtils.fetchNone(TgAuthorRoyalty.class).with("authorship.author.name");
        fp.addKeysTo("authorship.author.name", true);
        
        assertEquals(linkedSetOf("authorship", "authorship." + ID, "authorship." + VERSION,
                "authorship.author", "authorship.author." + ID, "authorship.author." + VERSION,
                "authorship.author.name", "authorship.author.name." + KEY, "authorship.author.name." + ID, "authorship.author.name." + VERSION, "authorship.author.name." + DESC // subkey and ID / VERSION for 'name' property of persistent TgPersonName type
        ), fp.allProperties());
    }
    
    @Test
    public void neighbour_branches_do_not_interfere_when_adding_keys() {
        final IFetchProvider<TgAuthorRoyalty> fp = EntityUtils.fetchNone(TgAuthorRoyalty.class).with("authorship.author.name");
        fp.addKeysTo("authorship.author", true);
        fp.addKeysTo("authorship.author.name", false);
        
        assertEquals(linkedSetOf("authorship", "authorship." + ID, "authorship." + VERSION,
                "authorship.author", /*name is a key of author */ "authorship.author.surname", "authorship.author.patronymic",  "authorship.author." + ID, "authorship.author." + VERSION,
                "authorship.author.name", "authorship.author.name." + KEY, "authorship.author.name." + ID, "authorship.author.name." + VERSION, "authorship.author." + DESC // subkey and ID / VERSION for 'name' property of persistent TgPersonName type
        ), fp.allProperties());
        assertEquals(
            fetchNone(TgAuthorRoyalty.class)
                .with("authorship", fetchNone(TgAuthorship.class)
                        .with(ID)
                        .with(VERSION)
                        .with("author", fetchNone(TgAuthor.class)
                                .with("surname")
                                .with("patronymic")
                                .with(ID)
                                .with(VERSION)
                                .with("name", fetchNone(TgPersonName.class)
                                        .with(KEY)
                                        .with(ID)
                                        .with(VERSION))
                                .with(DESC))),
            fp.fetchModel()
        );
    }
    
    /////////////////////////////////////////////////////////// union common properties: ///////////////////////////////////////////////////////////
    
    @Test
    public void fetch_provider_with_common_property_indicates_that_common_property_should_be_fetched() {
        final IFetchProvider<TgUnionHolder> fp = fetchWithKeyAndDesc(TgUnionHolder.class).with("union.common");
        
        assertTrue(fp.shouldFetch("union.common"));
        assertFalse(fp.fetchFor("union.common").instrumented());
        assertEquals(setOf(), fp.fetchFor("union.common").allProperties());
    }
    
    @Test
    public void fetch_provider_with_common_property_includes_that_property_in_every_type_of_the_union() {
        final IFetchProvider<TgUnionHolder> fp = fetchWithKeyAndDesc(TgUnionHolder.class).with("union.common");
        
        assertTrue(fp.shouldFetch("union.union1"));
        assertTrue(fp.shouldFetch("union.union1.common"));
        assertFalse(fp.fetchFor("union.union1").instrumented());
        assertEquals(setOf("common"), fp.fetchFor("union.union1").allProperties());
        
        assertTrue(fp.shouldFetch("union.union2"));
        assertTrue(fp.shouldFetch("union.union2.common"));
        assertFalse(fp.fetchFor("union.union2").instrumented());
        assertEquals(setOf("common"), fp.fetchFor("union.union2").allProperties());
        
        assertEquals(setOf(
            "union", "union.common",
            "union.union1", "union.union1.common",
            "union.union2", "union.union2.common"
        ), fp.allProperties());
    }
    
    @Test
    public void common_property_fetch_provider_merges_with_fetch_providers_in_every_type_of_the_union() {
        final IFetchProvider<TgUnionHolder> fp = fetchWithKeyAndDesc(TgUnionHolder.class)
            .with("union.union1.common.desc")
            .with("union.common", fetchWithKeyAndDesc(TgUnionCommonType.class).with("lastUpdatedBy"));
        
        assertEquals(fetchWithKeyAndDesc(TgUnionCommonType.class).with("desc").with("lastUpdatedBy"), fp.fetchFor("union.union1.common"));
        assertEquals(setOf(
            "union", "union.common", "union.common.lastUpdatedBy",
            "union.union1", "union.union1.common", "union.union1.common.desc", "union.union1.common.lastUpdatedBy",
            "union.union2", "union.union2.common", "union.union2.common.lastUpdatedBy"
        ), fp.allProperties());
    }
    
    @Test
    public void fetch_provider_with_common_property_can_be_merged_with_fetch_providers_in_every_type_of_the_union() {
        final IFetchProvider<TgUnionHolder> fp = fetchWithKeyAndDesc(TgUnionHolder.class)
            .with("union.union1.common.desc")
            .with(fetchWithKeyAndDesc(TgUnionHolder.class).with("union.common", fetchWithKeyAndDesc(TgUnionCommonType.class).with("lastUpdatedBy")));
        
        assertEquals(fetchWithKeyAndDesc(TgUnionCommonType.class).with("desc").with("lastUpdatedBy"), fp.fetchFor("union.union1.common"));
        assertEquals(setOf(
            "union", "union.common", "union.common.lastUpdatedBy",
            "union.union1", "union.union1.common", "union.union1.common.desc", "union.union1.common.lastUpdatedBy",
            "union.union2", "union.union2.common", "union.union2.common.lastUpdatedBy"
        ), fp.allProperties());
    }
    
    @Test
    public void common_property_removal_does_not_affect_fetch_providers_in_types_of_the_union() {
        // at least now; we do not support automatic removal of properties from union types because it is not known whether they were added manually or automatically through common property addition
        final IFetchProvider<TgUnionHolder> fp = fetchWithKeyAndDesc(TgUnionHolder.class)
            .with("union.union1.common.desc")
            .with("union.common", fetchWithKeyAndDesc(TgUnionCommonType.class).with("lastUpdatedBy"))
            .without("union.common");
        
        assertEquals(fetchWithKeyAndDesc(TgUnionCommonType.class).with("desc").with("lastUpdatedBy"), fp.fetchFor("union.union1.common"));
        assertEquals(setOf(
            "union",
            "union.union1", "union.union1.common", "union.union1.common.desc", "union.union1.common.lastUpdatedBy",
            "union.union2", "union.union2.common", "union.union2.common.lastUpdatedBy"
        ), fp.allProperties());
    }
    
    @Test
    public void fetch_provider_with_common_property_generates_fetch_model_with_that_property_in_every_type_of_the_union() {
        final IFetchProvider<TgUnionHolder> fp = fetchWithKeyAndDesc(TgUnionHolder.class).with("union.common");
        
        assertEquals(
            fetchKeyAndDescOnly(TgUnion.class)
            .with("union1", fetchKeyAndDescOnly(TgUnionType1.class)
                .with("common", fetchKeyAndDescOnly(TgUnionCommonType.class)))
            .with("union2", fetchKeyAndDescOnly(TgUnionType2.class)
                .with("common", fetchKeyAndDescOnly(TgUnionCommonType.class))),
            
            fp.fetchFor("union").fetchModel()
        );
    }
    
    @Test
    public void fetch_provider_with_common_subproperty_indicates_that_common_subproperty_should_be_fetched() {
        final IFetchProvider<TgUnionHolder> fp = fetchWithKeyAndDesc(TgUnionHolder.class).with("union.common.createdBy");
        
        assertTrue(fp.shouldFetch("union.common"));
        assertTrue(fp.shouldFetch("union.common.createdBy"));
        assertFalse(fp.fetchFor("union.common").instrumented());
        assertEquals(setOf("createdBy"), fp.fetchFor("union.common").allProperties());
    }
    
    @Test
    public void fetch_provider_with_common_subproperty_includes_that_subproperty_in_every_type_of_the_union() {
        final IFetchProvider<TgUnionHolder> fp = fetchWithKeyAndDesc(TgUnionHolder.class).with("union.common.createdBy");
        
        assertTrue(fp.shouldFetch("union.union1"));
        assertTrue(fp.shouldFetch("union.union1.common"));
        assertTrue(fp.shouldFetch("union.union1.common.createdBy"));
        assertFalse(fp.fetchFor("union.union1").instrumented());
        assertFalse(fp.fetchFor("union.union1.common").instrumented());
        assertEquals(setOf("common", "common.createdBy"), fp.fetchFor("union.union1").allProperties());
        assertEquals(setOf("createdBy"), fp.fetchFor("union.union1.common").allProperties());
        
        assertTrue(fp.shouldFetch("union.union2"));
        assertTrue(fp.shouldFetch("union.union2.common"));
        assertTrue(fp.shouldFetch("union.union2.common.createdBy"));
        assertFalse(fp.fetchFor("union.union2").instrumented());
        assertFalse(fp.fetchFor("union.union2.common").instrumented());
        assertEquals(setOf("common", "common.createdBy"), fp.fetchFor("union.union2").allProperties());
        assertEquals(setOf("createdBy"), fp.fetchFor("union.union2.common").allProperties());
        
        assertEquals(setOf(
            "union", "union.common", "union.common.createdBy",
            "union.union1", "union.union1.common", "union.union1.common.createdBy",
            "union.union2", "union.union2.common", "union.union2.common.createdBy"
        ), fp.allProperties());
    }
    
    @Test
    public void fetch_provider_with_common_subproperty_generates_fetch_model_with_that_subproperty_in_every_type_of_the_union() {
        final IFetchProvider<TgUnionHolder> fp = fetchWithKeyAndDesc(TgUnionHolder.class).with("union.common.createdBy");
        
        assertEquals(
            fetchKeyAndDescOnly(TgUnion.class)
            .with("union1", fetchKeyAndDescOnly(TgUnionType1.class)
                .with("common", fetchKeyAndDescOnly(TgUnionCommonType.class).with("createdBy", fetchKeyAndDescOnly(User.class))))
            .with("union2", fetchKeyAndDescOnly(TgUnionType2.class)
                .with("common", fetchKeyAndDescOnly(TgUnionCommonType.class).with("createdBy", fetchKeyAndDescOnly(User.class)))),
            
            fp.fetchFor("union").fetchModel()
        );
    }
    
    @Test
    public void key_is_added_into_common_property_fetch_provider_and_into_that_property_fetch_provider_in_every_type_of_the_union() {
        final IFetchProvider<TgUnionHolder> fp = EntityUtils.fetchNone(TgUnionHolder.class)
            .with("union.common");
        fp.addKeysTo("union.common", false);
        
        assertEquals(setOf(
            "union", "union.common", "union.common." + KEY, "union.common." + ID, "union.common." + VERSION,
            "union.union1", "union.union1." + ID, "union.union1." + VERSION, "union.union1.common", "union.union1.common." + KEY, "union.union1.common." + ID, "union.union1.common." + VERSION,
            "union.union2", "union.union2." + ID, "union.union2." + VERSION, "union.union2.common", "union.union2.common." + KEY, "union.union2.common." + ID, "union.union2.common." + VERSION
        ), fp.allProperties());
    }
    
}