package ua.com.fielden.platform.entity.query;

import org.junit.Test;

import ua.com.fielden.platform.entity.query.generation.BaseEntQueryTCase;
import ua.com.fielden.platform.entity.query.model.AggregatedResultQueryModel;
import ua.com.fielden.platform.entity.query.model.EntityResultQueryModel;
import ua.com.fielden.platform.entity.query.model.PrimitiveResultQueryModel;
import ua.com.fielden.platform.sample.domain.TgVehicle;
import ua.com.fielden.platform.sample.domain.TgVehicleModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.select;

public class EntQueryResultTypeTest extends BaseEntQueryTCase {

    @Test
    public void test1() {
        // TODO this is irrational combination
        final AggregatedResultQueryModel qry = select(VEHICLE).modelAsAggregate();
        assertEquals("Incorrect result type", EntityAggregates.class, entResultQry(qry).type());
    }

    @Test
    public void test2() {
        final EntityResultQueryModel<TgVehicle> qry = select(VEHICLE).model();
        assertEquals("Incorrect result type", VEHICLE, entResultQry(qry).type());
    }

    @Test
    public void test3() {
        try {
            entResultQry(select(TgVehicle.class).modelAsEntity(MODEL));
            fail("Should have failed!");
        } catch (final Exception e) {
        }
    }

    @Test
    public void test4() {
        final EntityResultQueryModel<TgVehicleModel> qry = select(VEHICLE).yield().prop("model").modelAsEntity(MODEL);
        assertEquals("Incorrect result type", MODEL, entResultQry(qry).type());
    }

    @Test
    public void test5() {
        final PrimitiveResultQueryModel qry = select(VEHICLE).yield().prop("key").modelAsPrimitive();
        assertEquals("Incorrect result type", null, entSubQry(qry).type());
    }

    @Test
    public void test6() {
        final EntityResultQueryModel<TgVehicle> qry = select(VEHICLE).model();
        assertEquals("Incorrect result type", VEHICLE, qry.getResultType());
    }

    @Test
    public void test7() {
        final EntityResultQueryModel<TgVehicle> qry = select(VEHICLE).leftJoin(MODEL).as("model").on().prop("model.id").eq().prop("model").model();
        assertEquals("Incorrect result type", VEHICLE, qry.getResultType());
    }

    @Test
    public void test8() {
        final EntityResultQueryModel<TgVehicleModel> qry = select(VEHICLE).yield().prop("model").modelAsEntity(MODEL);
        assertEquals("Incorrect result type", MODEL, qry.getResultType());
    }
}