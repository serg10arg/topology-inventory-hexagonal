package com.example.topologyinventory.domain.specification;

import com.example.topologyinventory.domain.entity.Equipment;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.exception.GenericSpecificationException;
import com.example.topologyinventory.domain.specification.shared.AbstractSpecification;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Network;

/**
 * Regla: una red no puede añadirse a un switch si ya existe otra con la misma
 * dirección, nombre y CIDR. Captura los datos de la red candidata en el
 * constructor y los compara contra las redes ya presentes en el switch.
 */
public class NetworkAvailabilitySpec extends AbstractSpecification<Equipment> {

    private final IP address;
    private final String name;
    private final int cidr;

    public NetworkAvailabilitySpec(Network network) {
        this.address = network.getNetworkAddress();
        this.name = network.getNetworkName();
        this.cidr = network.getNetworkCidr();
    }

    @Override
    public boolean isSatisfiedBy(Equipment switchNetworks) {
        return switchNetworks != null && isNetworkAvailable(switchNetworks);
    }

    @Override
    public void check(Equipment equipment) throws GenericSpecificationException {
        if (!isSatisfiedBy(equipment))
            throw new GenericSpecificationException("This network already exists");
    }

    private boolean isNetworkAvailable(Equipment switchNetworks) {
        var availability = true;
        for (Network network : ((Switch) switchNetworks).getSwitchNetworks()) {
            // Coincidencia total (dirección + nombre + CIDR) => la red ya existe.
            if (network.getNetworkAddress().equals(address) &&
                    network.getNetworkName().equals(name) &&
                    network.getNetworkCidr() == cidr) {
                availability = false;
                break;
            }
        }
        return availability;
    }
}