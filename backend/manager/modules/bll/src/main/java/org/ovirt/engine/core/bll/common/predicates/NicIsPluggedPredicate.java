package org.ovirt.engine.core.bll.common.predicates;

import java.util.function.Predicate;

import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;

public final class NicIsPluggedPredicate implements Predicate<VmNetworkInterface> {

    private static final Predicate<VmNetworkInterface> instance = new NicIsPluggedPredicate();

    public static Predicate<VmNetworkInterface> getInstance() {
        return instance;
    }

    @Override
    public boolean test(VmNetworkInterface vNic) {
        return vNic.isPlugged();
    }
}
