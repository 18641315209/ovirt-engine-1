package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.Map;

import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.QuotaCRUDParameters;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;

public class AddQuotaCommand<T extends QuotaCRUDParameters> extends CommandBase<T> {

    /**
     * Generated serialization UUID.
     */
    private static final long serialVersionUID = 8037593564997496657L;

    public AddQuotaCommand(T parameters) {
        super(parameters);
        setStoragePoolId(getParameters().getQuota() != null ? getParameters().getQuota().getStoragePoolId() : null);
    }

    @Override
    protected boolean canDoAction() {
        return (QuotaHelper.getInstance().checkQuotaValidationForAddEdit(getParameters().getQuota(),
                getReturnValue().getCanDoActionMessages()));
    }

    @Override
    protected void executeCommand() {
        setQuotaParameter();
        getQuotaDAO().save(getQuota());
        getReturnValue().setSucceeded(true);
    }

    @Override
    public Map<Guid, VdcObjectType> getPermissionCheckSubjects() {
        return Collections.singletonMap(getStoragePoolId() == null ? null : getStoragePoolId().getValue(),
                VdcObjectType.StoragePool);
    }

    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__QUOTA);
    }

    /**
     * Set quota from the parameter
     *
     * @param parameters
     * @return
     */
    private void setQuotaParameter() {
        Quota quotaParameter = getParameters().getQuota();
        quotaParameter.setId(Guid.NewGuid());
        setStoragePoolId(quotaParameter.getStoragePoolId());
        setQuotaName(quotaParameter.getQuotaName());
        if (quotaParameter.getQuotaStorages() != null) {
            for (QuotaStorage quotaStorage : quotaParameter.getQuotaStorages()) {
                quotaStorage.setQuotaId(getQuotaId());
                quotaStorage.setQuotaStorageId(Guid.NewGuid());
            }
        }
        if (quotaParameter.getQuotaVdsGroups() != null) {
            for (QuotaVdsGroup quotaVdsGroup : quotaParameter.getQuotaVdsGroups()) {
                quotaVdsGroup.setQuotaId(getQuotaId());
                quotaVdsGroup.setQuotaVdsGroupId(Guid.NewGuid());
            }
        }
        setQuota(quotaParameter);
    }
}
