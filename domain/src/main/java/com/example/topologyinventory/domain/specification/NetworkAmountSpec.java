package com.example.topologyinventory.domain.specification;

import com.example.topologyinventory.domain.entity.Equipment;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.exception.GenericSpecificationException;
import com.example.topologyinventory.domain.specification.shared.AbstractSpecification;

/**
 * Regla: un switch no puede provisionar más de {@value #MAXIMUM_ALLOWED_NETWORKS}
 * redes. El parámetro se tipa como {@link Equipment} para encajar en la jerarquía
 * de especificaciones, pero la regla solo aplica a un {@link Switch}, de ahí el cast.
 */
public class NetworkAmountSpec extends AbstractSpecification<Equipment> {

    /** Número máximo de redes permitidas por switch. */
    public static final int MAXIMUM_ALLOWED_NETWORKS = 6;

    @Override
    public boolean isSatisfiedBy(Equipment switchNetwork) {
        return ((Switch) switchNetwork).getSwitchNetworks().size() <= MAXIMUM_ALLOWED_NETWORKS;
    }

    @Override
    public void check(Equipment equipment) throws GenericSpecificationException {
        if (!isSatisfiedBy(equipment))
            throw new GenericSpecificationException("The max number of networks is " + NetworkAmountSpec.MAXIMUM_ALLOWED_NETWORKS);
    }
}