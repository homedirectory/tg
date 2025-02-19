package ua.com.fielden.platform.test.mapping;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ua.com.fielden.platform.dao.HibernateMappingsGenerator;
import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.query.DbVersion;
import ua.com.fielden.platform.entity.query.metadata.DomainMetadata;
import ua.com.fielden.platform.ui.config.EntityCentreConfig;

public class MappingGenerationTest {

    @Test
    public void dump_mapping_for_type_wity_byte_array_property() {
        final List<Class<? extends AbstractEntity<?>>> domainTypes = new ArrayList<>();
        domainTypes.add(EntityCentreConfig.class);
        final DomainMetadata mg = new DomainMetadata(null, null, domainTypes, DbVersion.H2);
        final String tgModelMapping = new HibernateMappingsGenerator().generateMappings(mg);
        final String expectedMapping = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!DOCTYPE hibernate-mapping PUBLIC\n" + "\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"\n"
                + "\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n" + "<hibernate-mapping default-access=\"field\">\n"
                + "<class name=\"ua.com.fielden.platform.ui.config.EntityCentreConfig\" table=\"ENTITY_CENTRE_CONFIG\">\n"
                + "	<id name=\"id\" column=\"_ID\" type=\"org.hibernate.type.LongType\" access=\"property\">\n" 
                + "	</id>\n" + "	<version name=\"version\" type=\"org.hibernate.type.LongType\" access=\"field\" insert=\"false\">\n"
                + "		<column name=\"_VERSION\" default=\"0\" />\n" + "	</version>\n"
                + "	<property name=\"configBody\" column=\"BODY\" type=\"org.hibernate.type.BinaryType\" length=\"%s\"/>\n"
                + "	<property name=\"configUuid\" column=\"CONFIGUUID_\" type=\"org.hibernate.type.StringType\"/>\n"
                + "	<many-to-one name=\"dashboardRefreshFrequency\" class=\"ua.com.fielden.platform.dashboard.DashboardRefreshFrequency\" column=\"DASHBOARDREFRESHFREQUENCY_\"/>\n"
                + "	<property name=\"dashboardable\" column=\"DASHBOARDABLE_\" type=\"org.hibernate.type.YesNoType\"/>\n"
                + "	<property name=\"dashboardableDate\" column=\"DASHBOARDABLEDATE_\" type=\"org.hibernate.type.TimestampType\"/>\n"
                + "	<property name=\"desc\" column=\"DESC_\" type=\"org.hibernate.type.StringType\"/>\n"
                + "	<many-to-one name=\"menuItem\" class=\"ua.com.fielden.platform.ui.config.MainMenuItem\" column=\"ID_MAIN_MENU\"/>\n"
                + "	<many-to-one name=\"owner\" class=\"ua.com.fielden.platform.security.user.User\" column=\"ID_CRAFT\"/>\n"
                + "	<property name=\"preferred\" column=\"PREFERRED_\" type=\"org.hibernate.type.YesNoType\"/>\n"
                + "	<property name=\"principal\" column=\"IS_PRINCIPAL\" type=\"org.hibernate.type.YesNoType\"/>\n"
                + "	<property name=\"runAutomatically\" column=\"RUNAUTOMATICALLY_\" type=\"org.hibernate.type.YesNoType\"/>\n"
                + "	<property name=\"title\" column=\"TITLE\" type=\"org.hibernate.type.StringType\"/>\n" + "</class>\n\n" + "</hibernate-mapping>", Integer.MAX_VALUE);
        assertEquals("Incorrect mapping.", expectedMapping, tgModelMapping);
    }

}