package com.example.topologyinventory.domain.entity;

import com.example.topologyinventory.domain.specification.EmptyNetworkSpec;
import com.example.topologyinventory.domain.specification.SameCountrySpec;
import com.example.topologyinventory.domain.specification.SameIpSpec;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.domain.vo.Location;
import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.RouterType;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * Router de borde (edge). Es el punto de la topología donde se conectan los
 * switches (y, a través de ellos, las redes). Solo un edge router puede contener
 * switches, por lo que concentra las reglas para añadirlos y quitarlos.
 */
@Getter
@ToString
public class EdgeRouter extends Router {

    /** Switches conectados a este edge router, indexados por su Id. */
    private Map<Id, Switch> switches;

    @Builder
    public EdgeRouter(Id id, Vendor vendor, Model model, IP ip, Location location,
                      RouterType routerType, Map<Id, Switch> switches) {
        super(id, vendor, model, ip, location, routerType);
        // Un router recién creado por la fábrica llega sin mapa: se parte de uno vacío y mutable.
        this.switches = switches == null ? new HashMap<>() : switches;
    }

    /**
     * Conecta un switch, previa comprobación de que está en el mismo país
     * ({@link SameCountrySpec}) y no comparte IP con el router ({@link SameIpSpec}).
     *
     * @param anySwitch switch a conectar
     */
    public void addSwitch(Switch anySwitch) {
        var sameCountryRouterSpec = new SameCountrySpec(this);
        var sameIpSpec = new SameIpSpec(this);

        sameCountryRouterSpec.check(anySwitch);
        sameIpSpec.check(anySwitch);

        this.switches.put(anySwitch.id, anySwitch);
    }

    /**
     * Desconecta un switch, siempre que no tenga redes conectadas
     * ({@link EmptyNetworkSpec}).
     *
     * @param anySwitch switch a desconectar
     * @return el switch eliminado, o {@code null} si no estaba presente
     */
    public Switch removeSwitch(Switch anySwitch) {
        var emptyNetworkSpec = new EmptyNetworkSpec();
        emptyNetworkSpec.check(anySwitch);

        return this.switches.remove(anySwitch.id);
    }
}