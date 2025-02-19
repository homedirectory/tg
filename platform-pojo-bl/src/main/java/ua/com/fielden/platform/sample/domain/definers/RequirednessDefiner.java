package ua.com.fielden.platform.sample.domain.definers;

import static ua.com.fielden.platform.error.Result.informative;

import ua.com.fielden.platform.entity.meta.IAfterChangeEventHandler;
import ua.com.fielden.platform.entity.meta.MetaProperty;

public class RequirednessDefiner implements IAfterChangeEventHandler<Integer> {

    @Override
    public void handle(final MetaProperty<Integer> prop, final Integer entityPropertyValue) {
        prop.getEntity().getPropertyIfNotProxy("entityProp").ifPresent(
                mp -> {
                    mp.setRequired(prop.getValue() != null && prop.getValue() > 100);
                    mp.setEditable(prop.getValue() == null || prop.getValue() != 13);
                });
        if (entityPropertyValue.intValue() > 75) {
            prop.setDomainValidationResult(informative("Information that is over 75"));
        }
    }

}
