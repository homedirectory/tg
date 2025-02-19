package ua.com.fielden.platform.entity.validation;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.inject.Injector;

import ua.com.fielden.platform.entity.factory.EntityFactory;
import ua.com.fielden.platform.entity.meta.MetaProperty;
import ua.com.fielden.platform.entity.validation.test_entities.EntityWithMaxLengthValidation;
import ua.com.fielden.platform.ioc.ApplicationInjectorFactory;
import ua.com.fielden.platform.test.CommonTestEntityModuleWithPropertyFactory;
import ua.com.fielden.platform.test.EntityModuleWithPropertyFactory;

/**
 * A test case for validation with {@link MaxLengthValidator}.
 *
 * @author TG Team
 *
 */
public class MaxLengthValidatorTest {

    private final EntityModuleWithPropertyFactory module = new CommonTestEntityModuleWithPropertyFactory();
    private final Injector injector = new ApplicationInjectorFactory().add(module).getInjector();
    private final EntityFactory factory = injector.getInstance(EntityFactory.class);

    /**
     * This test covers a situation where {@code limit} is specified and {@code length} is not.
     */
    @Test
    public void string_property_value_cannot_be_longer_than_its_limit() {
        final Integer limit = 5;
        final EntityWithMaxLengthValidation entity = factory.newEntity(EntityWithMaxLengthValidation.class);

        final MetaProperty<String> mp = entity.getProperty("propWithLimit");
        entity.setPropWithLimit("1");
        assertTrue(mp.isValid());

        entity.setPropWithLimit("12345");
        assertTrue(mp.isValid());

        entity.setPropWithLimit("123456");
        assertFalse(mp.isValid());

        assertEquals(format(MaxLengthValidator.ERR_VALUE_SHOULD_NOT_EXCEED_MAX_LENGTH, limit), mp.getFirstFailure().getMessage());
    }

    /**
     * This test covers a situation where {@code limit} is not specified and {@code length} is specified.
     */
    @Test
    public void string_property_value_cannot_be_longer_than_its_length() {
        final Integer length = 3;
        final EntityWithMaxLengthValidation entity = factory.newEntity(EntityWithMaxLengthValidation.class);

        final MetaProperty<String> mp = entity.getProperty("propWithLength");
        entity.setPropWithLength("1");
        assertTrue(mp.isValid());

        entity.setPropWithLength("123");
        assertTrue(mp.isValid());

        entity.setPropWithLength("1234");
        assertFalse(mp.isValid());

        assertEquals(format(MaxLengthValidator.ERR_VALUE_SHOULD_NOT_EXCEED_MAX_LENGTH, length), mp.getFirstFailure().getMessage());
    }

    /**
     * This test covers a situation where both {@code limit} and {@code length} are specified, but {@code limit} is greater.
     * This case demonstrates the preference for smaller of the two values.
     */
    @Test
    public void string_property_value_cannot_be_longer_than_its_smaller_length_even_if_limit_is_greater() {
        final Integer length = 3; // limit == 5
        final EntityWithMaxLengthValidation entity = factory.newEntity(EntityWithMaxLengthValidation.class);

        final MetaProperty<String> mp = entity.getProperty("propWithSmallerLengthAndGreaterLimit");
        entity.setPropWithSmallerLengthAndGreaterLimit("1");
        assertTrue(mp.isValid());

        entity.setPropWithSmallerLengthAndGreaterLimit("123");
        assertTrue(mp.isValid());

        entity.setPropWithSmallerLengthAndGreaterLimit("1234");
        assertFalse(mp.isValid());

        assertEquals(format(MaxLengthValidator.ERR_VALUE_SHOULD_NOT_EXCEED_MAX_LENGTH, length), mp.getFirstFailure().getMessage());
    }

    /**
     * This test covers a situation where both {@code limit} and {@code length} are specified, but {@code limit} is smaller.
     * This case demonstrates the preference for smaller of the two values.
     */
    @Test
    public void string_property_value_cannot_be_longer_than_its_smaller_limite_even_if_length_is_greater() {
        final Integer limit = 3; // length == 5
        final EntityWithMaxLengthValidation entity = factory.newEntity(EntityWithMaxLengthValidation.class);

        final MetaProperty<String> mp = entity.getProperty("propWithGreaterLengthAndSmallerLimit");
        entity.setPropWithGreaterLengthAndSmallerLimit("1");
        assertTrue(mp.isValid());

        entity.setPropWithGreaterLengthAndSmallerLimit("123");
        assertTrue(mp.isValid());

        entity.setPropWithGreaterLengthAndSmallerLimit("1234");
        assertFalse(mp.isValid());

        assertEquals(format(MaxLengthValidator.ERR_VALUE_SHOULD_NOT_EXCEED_MAX_LENGTH, limit), mp.getFirstFailure().getMessage());
    }

    /**
     * This test covers an invalid situation where both {@code limit} and {@code length} are missing.
     */
    @Test
    public void string_property_value_cannot_be_set_to_a_non_empty_value_if_neither_limit_nor_length_are_specified() {
        final EntityWithMaxLengthValidation entity = factory.newEntity(EntityWithMaxLengthValidation.class);

        final MetaProperty<String> mp = entity.getProperty("propWithInvalidDeclaration");
        entity.setPropWithInvalidDeclaration("1");
        assertFalse(mp.isValid());

        assertEquals(MaxLengthValidator.ERR_MISSING_MAX_LENGTH, mp.getFirstFailure().getMessage());
    }

    @Test
    public void string_property_accepts_empty_values_regardless_of_max_length_validator_parameters() {
        final EntityWithMaxLengthValidation entity = factory.newEntity(EntityWithMaxLengthValidation.class);

        final MetaProperty<String> mpPropWithLimit = entity.getProperty("propWithLimit");
        entity.setPropWithLimit("");
        assertTrue(mpPropWithLimit.isValid());

        final MetaProperty<String> mpPropWithLength = entity.getProperty("propWithLength");
        entity.setPropWithLength("");
        assertTrue(mpPropWithLength.isValid());

        final MetaProperty<String> mpPropWithGreaterLengthAndSmallerLimit = entity.getProperty("propWithGreaterLengthAndSmallerLimit");
        entity.setPropWithGreaterLengthAndSmallerLimit("");
        assertTrue(mpPropWithGreaterLengthAndSmallerLimit.isValid());

        final MetaProperty<String> mpPropWithSmallerLengthAndGreaterLimit = entity.getProperty("propWithSmallerLengthAndGreaterLimit");
        entity.setPropWithSmallerLengthAndGreaterLimit("");
        assertTrue(mpPropWithSmallerLengthAndGreaterLimit.isValid());

        final MetaProperty<String> mpPropWithInvalidDeclaration = entity.getProperty("propWithInvalidDeclaration");
        entity.setPropWithInvalidDeclaration("");
        assertTrue(mpPropWithInvalidDeclaration.isValid());
    }
}