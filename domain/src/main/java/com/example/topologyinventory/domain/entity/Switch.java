package com.example.topologyinventory.domain.entity;

import com.example.topologyinventory.domain.specification.CIDRSpecification;
import com.example.topologyinventory.domain.specification.NetworkAmountSpec;
import com.example.topologyinventory.domain.specification.NetworkAvailabilitySpec;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.domain.vo.Location;
import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.Network;
import com.example.topologyinventory.domain.vo.Protocol;
import com.example.topologyinventory.domain.vo.SwitchType;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.function.Predicate;

/**
 * Equipo de red que provee redes lógicas ({@link Network}). A diferencia de los
 * routers, un switch mantiene una lista de redes y solo puede conectarse a un
 * edge router. Las reglas para añadir redes se expresan con especificaciones.
 */
@Getter
public class Switch extends Equipment {

    /** Tipo de switch (LAYER2 o LAYER3). */
    private SwitchType switchType;
    /** Redes actualmente provistas por este switch. */
    private List<Network> switchNetworks;

    @Builder
    public Switch(Id id, Vendor vendor, Model model, IP ip, Location location,
                  SwitchType switchType, List<Network> switchNetworks) {
        super(id, vendor, model, ip, location);
        this.switchType = switchType;
        this.switchNetworks = switchNetworks;
    }

    /** Predicado para filtrar redes por protocolo (IPv4/IPv6). */
    public static Predicate<Network> getNetworkProtocolPredicate(Protocol protocol) {
        return network -> network.getNetworkAddress().getProtocol().equals(protocol);
    }

    /** Predicado para filtrar switches por tipo (LAYER2/LAYER3). */
    public static Predicate<Switch> getSwitchTypePredicate(SwitchType switchType) {
        return aSwitch -> aSwitch.switchType.equals(switchType);
    }

    /**
     * Añade una red al switch si se cumplen tres reglas de negocio, comprobadas
     * en orden mediante especificaciones:
     * <ul>
     *   <li>el CIDR de la red es válido ({@link CIDRSpecification});</li>
     *   <li>la red no existe ya en el switch ({@link NetworkAvailabilitySpec});</li>
     *   <li>no se supera el máximo de redes ({@link NetworkAmountSpec}).</li>
     * </ul>
     * Si alguna regla falla, la specification lanza excepción y la red no se añade.
     *
     * @param network red a añadir
     * @return {@code true} si la red se añadió correctamente
     */
    public boolean addNetworkToSwitch(Network network) {
        var availabilitySpec = new NetworkAvailabilitySpec(network);
        var cidrSpec = new CIDRSpecification();
        var amountSpec = new NetworkAmountSpec();

        cidrSpec.check(network.getNetworkCidr());
        availabilitySpec.check(this);
        amountSpec.check(this);

        return this.switchNetworks.add(network);
    }

    /**
     * Elimina una red del switch. No aplica ninguna regla: quitar una red no
     * viola ninguna restricción de negocio.
     *
     * @param network red a eliminar
     * @return {@code true} si la red estaba presente y se eliminó
     */
    public boolean removeNetworkFromSwitch(Network network) {
        return this.switchNetworks.remove(network);
    }
}