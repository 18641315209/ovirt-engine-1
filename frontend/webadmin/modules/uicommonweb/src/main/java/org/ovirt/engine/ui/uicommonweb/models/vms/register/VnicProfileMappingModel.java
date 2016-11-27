package org.ovirt.engine.ui.uicommonweb.models.vms.register;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.network.ExternalVnicProfileMapping;
import org.ovirt.engine.core.common.businessentities.network.VnicProfileView;
import org.ovirt.engine.ui.frontend.AsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

public class VnicProfileMappingModel extends Model {

    private static final String OK_COMMAND = "OK"; //$NON-NLS-1$

    private final ListModel<Cluster> targetCluster;

    private final ListModel<VnicProfileMappingItem> mappingModelRows;

    private final Model originModel;

    private final Map<Cluster, Set<VnicProfileMappingItem>> shownMappingRows;

    private Map<Cluster, Set<ExternalVnicProfileMapping>> externalVnicProfiles;

    public VnicProfileMappingModel(Model originModel,
            Map<Cluster, Set<ExternalVnicProfileMapping>> externalVnicProfiles) {
        this.originModel = originModel;
        this.externalVnicProfiles = externalVnicProfiles;
        this.mappingModelRows = new ListModel<>();
        this.targetCluster = new ListModel<>();
        this.shownMappingRows = new HashMap<>();
    }

    @Override
    public void initialize() {
        super.initialize();

        initTargetClusters();
        addCommands();
    }

    private void initTargetClusters() {
        targetCluster.getSelectedItemChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                updateMappingRows();
            }
        });
        targetCluster.setItems(externalVnicProfiles.keySet(), Linq.firstOrNull(targetCluster.getItems()));
    }

    private void addCommands() {
        final UICommand okCommand = UICommand.createDefaultOkUiCommand(OK_COMMAND, this);
        getCommands().add(okCommand);
        final UICommand CancelCommand = UICommand.createCancelUiCommand(CANCEL_COMMAND, this);
        getCommands().add(CancelCommand);
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (CANCEL_COMMAND.equals(command.getName())) {
            closeDialog();
        } else if (OK_COMMAND.equals(command.getName())) {
            mergeShownRows();
            closeDialog();
        }
    }

    private void mergeShownRows() {
        for (Entry<Cluster, Set<VnicProfileMappingItem>> showCluster : shownMappingRows.entrySet()) {
            final Cluster cluster = showCluster.getKey();
            final Set<VnicProfileMappingItem> showClusterRows = showCluster.getValue();
            final Set<ExternalVnicProfileMapping> existingMappings;
            if (externalVnicProfiles.containsKey(cluster)) {
                existingMappings = externalVnicProfiles.get(cluster);
            } else {
                existingMappings = new HashSet<>();
                externalVnicProfiles.put(cluster, existingMappings);
            }
            for (VnicProfileMappingItem shownRow : showClusterRows) {
                final ExternalVnicProfileMapping shownMapping = shownRow.getEntity();
                addOrReplace(existingMappings, shownMapping);
            }
        }
    }

    private <T> void addOrReplace(Set<T> set, T e) {
        set.remove(e);
        set.add(e);
    }

    private void closeDialog() {
        originModel.setWindow(null);
    }

    private void updateMappingRows() {

        startProgress();

        AsyncDataProvider.getInstance().getVnicProfilesByClusterId(
                new AsyncQuery<>(new AsyncCallback<List<VnicProfileView>>() {
                    @Override
                    public void onSuccess(List<VnicProfileView> returnValue) {
                        final List<VnicProfileView> vnicProfiles = new ArrayList<>();
                        vnicProfiles.add(VnicProfileView.EMPTY);
                        vnicProfiles.addAll(returnValue);
                        Collections.sort(vnicProfiles, new Linq.VnicProfileViewComparator());

                        populateMappingRows(vnicProfiles);

                        stopProgress();
                    }
                }),
                targetCluster.getSelectedItem().getId());
    }

    private void populateMappingRows(List<VnicProfileView> targetVnicProfiles) {
        final Cluster selectedCluster = targetCluster.getSelectedItem();
        final Set<ExternalVnicProfileMapping> vnicProfilesToBeMapped = externalVnicProfiles.get(selectedCluster);
        final List<VnicProfileMappingItem> mappingItems = new ArrayList<>();

        for (ExternalVnicProfileMapping externalVnicProfile : vnicProfilesToBeMapped) {
            mappingItems.add(new VnicProfileMappingItem(externalVnicProfile, targetVnicProfiles));
        }
        Collections.sort(mappingItems, new VnicProfileMappingItemComparator());
        mappingModelRows.setItems(mappingItems);
        shownMappingRows.put(selectedCluster, new HashSet<>(mappingItems));
    }

    // in use by view
    public ListModel<Cluster> getTargetCluster() {
        return targetCluster;
    }

    public ListModel<VnicProfileMappingItem> getMappingModelRows() {
        return mappingModelRows;
    }

}