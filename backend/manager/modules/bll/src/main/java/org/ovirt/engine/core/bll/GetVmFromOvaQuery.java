package org.ovirt.engine.core.bll;

import java.io.IOException;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.bll.exportimport.ExtractOvaCommand;
import org.ovirt.engine.core.bll.storage.ovfstore.OvfHelper;
import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.queries.GetVmFromOvaQueryParameters;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.ansible.AnsibleCommandBuilder;
import org.ovirt.engine.core.common.utils.ansible.AnsibleConstants;
import org.ovirt.engine.core.common.utils.ansible.AnsibleExecutor;
import org.ovirt.engine.core.common.utils.ansible.AnsibleReturnCode;
import org.ovirt.engine.core.common.utils.ansible.AnsibleReturnValue;
import org.ovirt.engine.core.common.utils.ansible.AnsibleVerbosity;
import org.ovirt.engine.core.common.vdscommands.GetOvaInfoParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.dao.VdsStaticDao;
import org.ovirt.engine.core.utils.ovf.OvfReaderException;

public class GetVmFromOvaQuery<T extends GetVmFromOvaQueryParameters> extends QueriesCommandBase<T> {

    @Inject
    private VdsStaticDao vdsStaticDao;
    @Inject
    private AnsibleExecutor ansibleExecutor;
    @Inject
    private OvfHelper ovfHelper;

    public GetVmFromOvaQuery(T parameters, EngineContext engineContext) {
        super(parameters, engineContext);
    }

    @Override
    protected void executeQueryCommand() {
        String ovf = runAnsibleQueryOvaInfoPlaybook();

        boolean originOvirt = ovf.contains("xmlns:ovirt");
        VM vm = null;
        try {
            vm = readVmFromOva(ovf);
        } catch (Exception e) {
            if (originOvirt) {
                log.debug("failed to parse a given ovf configuration: \n" + ovf, e);
                getQueryReturnValue().setExceptionString("failed to parse a given ovf configuration " + e.getMessage());
            }
        }

        if (!originOvirt && vm == null) {
            // If we fail to parse an OVF that resides within an OVA generated by others,
            // let's try the old-way, using the minimal parsing of OVF in VDSM
            vm = getVmInfoFromOvaFile();
        }

        if (originOvirt && vm != null) {
            vm.setOrigin(OriginType.OVIRT);
        }

        setReturnValue(vm);
        getQueryReturnValue().setSucceeded(vm != null);
    }

    private VM getVmInfoFromOvaFile() {
        return (VM) runVdsCommand(VDSCommandType.GetOvaInfo, buildGetOvaInfoParameters()).getReturnValue();
    }

    private GetOvaInfoParameters buildGetOvaInfoParameters() {
        return new GetOvaInfoParameters(
                getParameters().getVdsId(),
                getParameters().getPath());
    }

    private String runAnsibleQueryOvaInfoPlaybook() {
        String hostname = vdsStaticDao.get(getParameters().getVdsId()).getHostName();
        AnsibleCommandBuilder command = new AnsibleCommandBuilder()
                .hostnames(hostname)
                .variables(
                    new Pair<>("ovirt_query_ova_path", getParameters().getPath())
                )
                // /var/log/ovirt-engine/ova/ovirt-query-ova-ansible-{hostname}-{timestamp}.log
                .logFileDirectory(ExtractOvaCommand.IMPORT_OVA_LOG_DIRECTORY)
                .logFilePrefix("ovirt-query-ova-ansible")
                .logFileName(hostname)
                .verboseLevel(AnsibleVerbosity.LEVEL0)
                .stdoutCallback(AnsibleConstants.OVA_QUERY_CALLBACK_PLUGIN)
                .playbook(AnsibleConstants.QUERY_OVA_PLAYBOOK);

        boolean succeeded = false;
        AnsibleReturnValue ansibleReturnValue = null;
        try {
            ansibleReturnValue = ansibleExecutor.runCommand(command);
            succeeded = ansibleReturnValue.getAnsibleReturnCode() == AnsibleReturnCode.OK;
        } catch (IOException | InterruptedException e) {
            log.debug("Failed to query OVA info", e);
        }

        if (!succeeded) {
            log.error("Failed to query OVA info");
            throw new EngineException(EngineError.GeneralException, "Failed to query OVA info");
        }

        return ansibleReturnValue.getStdout();
    }

    private VM readVmFromOva(String ovf) throws OvfReaderException {
        return ovf != null ? ovfHelper.readVmFromOva(ovf) : null;
    }

}
