package ua.com.fielden.platform.example.entities;

import java.util.List;

import ua.com.fielden.platform.entity.annotation.DescTitle;
import ua.com.fielden.platform.entity.annotation.IsProperty;
import ua.com.fielden.platform.entity.annotation.KeyTitle;
import ua.com.fielden.platform.entity.annotation.Observable;
import ua.com.fielden.platform.entity.validation.annotation.EntityExists;
import ua.com.fielden.platform.entity.validation.annotation.NotNull;

/**
 * Bogie rotable business entity
 * 
 * @author nc
 * 
 */
@KeyTitle(value = "Bogie No", desc = "Bogie key")
@DescTitle(value = "Description", desc = "Bogie description")
public class Bogie extends Rotable {

    private static final long serialVersionUID = 1L;

    @IsProperty(BogieSlot.class)
    private List<BogieSlot> slots;

    protected Bogie() {
    }

    public Bogie(final String name, final String desc) {
        super(name, desc);
    }

    public List<BogieSlot> getSlots() {
        return slots;
    }

    @Observable
    protected void setSlots(final List<BogieSlot> slots) {
        this.slots = slots;
    }

    /**
     * Gets slot by slot position.
     * 
     * @param slotPosition
     * @return
     * @throws Exception
     */
    public BogieSlot getSlot(final Integer slotPosition) throws Exception {
        if (slotPosition > 0 && slotPosition <= slots.size()) {
            return slots.get(slotPosition - 1);
        } else {
            throw new Exception("Invalid slot position.");
        }
    }

    /**
     * Tests compatibility of the given wheelset with this bogie.
     * 
     * @param rotable
     * @return
     */
    public boolean isClassCompatible(final Wheelset rotable) {
        return getRotableClass().isWheelsetClassCompatible(rotable.getRotableClass());
    };

    @Override
    public BogieClass getRotableClass() {
        return (BogieClass) super.getRotableClass();
    }

    @Override
    @NotNull
    @EntityExists(BogieClass.class)
    @Observable
    public Bogie setRotableClass(final RotableClass klass) {
        super.setRotableClass(klass);
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer();
        result.append("Name: " + getKey() + "\n");
        result.append("Desc: " + getDesc() + "\n");
        result.append(getRotableClass().toString() + "\n");
        result.append("\nLocation: " + getLocation());
        return result.toString();
    }
}
