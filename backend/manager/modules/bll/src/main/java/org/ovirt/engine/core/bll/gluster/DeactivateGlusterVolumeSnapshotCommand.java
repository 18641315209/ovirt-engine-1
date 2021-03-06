package org.ovirt.engine.core.bll.gluster;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeSnapshotActionParameters;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterSnapshotStatus;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeSnapshotActionVDSParameters;

@NonTransactiveCommandAttribute
public class DeactivateGlusterVolumeSnapshotCommand extends GlusterVolumeSnapshotCommandBase<GlusterVolumeSnapshotActionParameters> {

    public DeactivateGlusterVolumeSnapshotCommand(GlusterVolumeSnapshotActionParameters params,
            CommandContext commandContext) {
        super(params, commandContext);
    }

    @Override
    protected void setActionMessageParameters() {
        addValidationMessage(EngineMessage.VAR__ACTION__DEACTIVATE);
        super.setActionMessageParameters();
    }

    @Override
    public void executeCommand() {
        VDSReturnValue retVal =
                runVdsCommand(VDSCommandType.DeactivateGlusterVolumeSnapshot,
                        new GlusterVolumeSnapshotActionVDSParameters(getUpServer().getId(),
                                getGlusterVolumeName(),
                                getParameters().getSnapshotName()));
        setSucceeded(retVal.getSucceeded());

        if (!getSucceeded()) {
            handleVdsError(AuditLogType.GLUSTER_VOLUME_SNAPSHOT_DEACTIVATE_FAILED, retVal.getVdsError().getMessage());
        } else {
            glusterVolumeSnapshotDao.updateSnapshotStatus(getSnapshot().getId(), GlusterSnapshotStatus.DEACTIVATED);
        }
    }

    @Override
    protected boolean validate() {
        if (!super.validate()) {
            return false;
        }

        if (getSnapshot().getStatus() == GlusterSnapshotStatus.DEACTIVATED) {
            return failValidation(EngineMessage.ACTION_TYPE_FAILED_GLUSTER_VOLUME_SNAPSHOT_ALREADY_DEACTIVATED,
                    getSnapshot().getSnapshotName());
        }

        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.GLUSTER_VOLUME_SNAPSHOT_DEACTIVATED;
        } else {
            return AuditLogType.GLUSTER_VOLUME_SNAPSHOT_DEACTIVATE_FAILED;
        }
    }
}
