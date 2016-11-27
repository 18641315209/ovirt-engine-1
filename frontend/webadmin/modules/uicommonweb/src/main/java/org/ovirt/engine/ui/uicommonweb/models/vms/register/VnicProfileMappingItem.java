package org.ovirt.engine.ui.uicommonweb.models.vms.register;

import java.util.List;
import java.util.Objects;

import org.ovirt.engine.core.common.businessentities.network.ExternalVnicProfileMapping;
import org.ovirt.engine.core.common.businessentities.network.VnicProfileView;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.Linq.IPredicate;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

public class VnicProfileMappingItem extends EntityModel<ExternalVnicProfileMapping> {

    private final ListModel<VnicProfileView> targetVnicProfile;

    public VnicProfileMappingItem(ExternalVnicProfileMapping entity, List<VnicProfileView> targetVnicProfiles) {
        setEntity(new ExternalVnicProfileMapping(entity));
        this.targetVnicProfile = new ListModel<>();
        this.targetVnicProfile.setItems(targetVnicProfiles);
    }

    @Override
    public void initialize() {
        super.initialize();

        this.targetVnicProfile.getSelectedItemChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                getEntity().setVnicProfileId(getTargetVnicProfileId());
            }
        });
        selectInitialTargetVnicProfile();
    }

    private Guid getTargetVnicProfileId() {
        final VnicProfileView selectedVnicProfile = targetVnicProfile.getSelectedItem();
        if (selectedVnicProfile == null || selectedVnicProfile == VnicProfileView.EMPTY) {
            return null;
        } else {
            return selectedVnicProfile.getId();
        }
    }

    private void selectInitialTargetVnicProfile() {
        final IPredicate<VnicProfileView> predicate;
        if (getEntity().getVnicProfileId() == null) {
            predicate = new IPredicate<VnicProfileView>() {
                @Override
                public boolean match(VnicProfileView vnicProfile) {
                    return Objects.equals(getEntity().getExternalNetworkName(), vnicProfile.getNetworkName())
                            && Objects.equals(getEntity().getExternalNetworkName(), vnicProfile.getName());
                }
            };
        } else {
            predicate = new IPredicate<VnicProfileView>() {
                @Override
                public boolean match(VnicProfileView vnicProfile) {
                    return Objects.equals(getEntity().getVnicProfileId(), vnicProfile.getId());
                }
            };
        }
        selectTargetVnicProfileByPredicate(predicate);
    }

    private void selectTargetVnicProfileByPredicate(IPredicate<VnicProfileView> predicate) {
        final VnicProfileView vnicProfile =
                Linq.firstOrDefault(targetVnicProfile.getItems(), predicate, VnicProfileView.EMPTY);
        targetVnicProfile.setSelectedItem(vnicProfile);
    }

    public ListModel<VnicProfileView> getTargetVnicProfile() {
        return targetVnicProfile;
    }
}