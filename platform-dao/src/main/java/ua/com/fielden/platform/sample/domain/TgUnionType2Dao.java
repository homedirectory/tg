package ua.com.fielden.platform.sample.domain;

import com.google.inject.Inject;

import ua.com.fielden.platform.dao.CommonEntityDao;
import ua.com.fielden.platform.entity.annotation.EntityType;
import ua.com.fielden.platform.entity.query.IFilter;

/** 
 * DAO implementation for companion object {@link TgUnionType2Co}.
 * 
 * @author TG Team
 *
 */
@EntityType(TgUnionType2.class)
public class TgUnionType2Dao extends CommonEntityDao<TgUnionType2> implements TgUnionType2Co {

    @Inject
    public TgUnionType2Dao(final IFilter filter) {
        super(filter);
    }

}