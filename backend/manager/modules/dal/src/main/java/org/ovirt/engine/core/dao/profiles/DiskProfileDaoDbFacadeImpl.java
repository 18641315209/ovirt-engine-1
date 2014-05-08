package org.ovirt.engine.core.dao.profiles;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.profiles.DiskProfile;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class DiskProfileDaoDbFacadeImpl extends ProfileBaseDaoFacadeImpl<DiskProfile> implements DiskProfileDao {
    private static final DiskProfileDaoDbFacadaeImplMapper MAPPER = new DiskProfileDaoDbFacadaeImplMapper();

    public DiskProfileDaoDbFacadeImpl() {
        super("DiskProfile");
    }

    @Override
    public List<DiskProfile> getAllForStorageDomain(Guid storageDomainId) {
        return getCallsHandler().executeReadList("GetDiskProfilesByStorageDomainId",
                createEntityRowMapper(),
                getCustomMapSqlParameterSource().addValue("storage_domain_id", storageDomainId));
    }

    @Override
    protected RowMapper<DiskProfile> createEntityRowMapper() {
        return MAPPER;
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(DiskProfile obj) {
        MapSqlParameterSource map = super.createFullParametersMapper(obj);
        map.addValue("storage_domain_id", obj.getStorageDomainId());
        return map;
    }

    protected static class DiskProfileDaoDbFacadaeImplMapper extends ProfileBaseDaoFacadaeImplMapper<DiskProfile> {

        @Override
        protected DiskProfile createProfileEntity(ResultSet rs) throws SQLException {
            DiskProfile diskProfile = new DiskProfile();
            diskProfile.setStorageDomainId(getGuid(rs, "storage_domain_id"));
            return diskProfile;
        }

    }
}
