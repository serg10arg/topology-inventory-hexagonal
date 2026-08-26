package com.example.topologyinventory.domain.specification;

import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.exception.GenericSpecificationException;
import com.example.topologyinventory.domain.specification.shared.AbstractSpecification;

/**
 * Regla: un switch solo puede eliminarse si no tiene redes conectadas. Impide
 * perder redes al desconectar un switch de un edge router.
 */
public class EmptyNetworkSpec extends AbstractSpecification<Switch> {

    @Override
    public boolean isSatisfiedBy(Switch switchNetwork) {
        return switchNetwork.getSwitchNetworks() == null || switchNetwork.getSwitchNetworks().isEmpty();
    }

    @Override
    public void check(Switch aSwitch) throws GenericSpecificationException {
        if (!isSatisfiedBy(aSwitch))
            throw new GenericSpecificationException("It's not possible to remove a switch with networks attached to it");
    }
}