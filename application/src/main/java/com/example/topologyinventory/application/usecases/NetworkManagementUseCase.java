package com.example.topologyinventory.application.usecases;

import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Network;

/**
 * Puerto de entrada para la gestión de redes. Las redes son objetos de valor que
 * viven dentro de un {@link Switch}; por eso este contrato crea redes y las
 * añade o quita de un switch, sin operaciones de persistencia propias (la red se
 * persiste como parte del agregado del router).
 */
public interface NetworkManagementUseCase {

    /**
     * Crea una red (objeto de valor) sin asociarla todavía a ningún switch.
     *
     * @param networkAddress dirección de red
     * @param networkName    nombre de la red
     * @param networkCidr    prefijo CIDR
     * @return la {@link Network} recién creada
     */
    Network createNetwork(IP networkAddress, String networkName, int networkCidr);

    /**
     * Añade una red a un switch (sujeto a las reglas del dominio del switch).
     *
     * @param network       red a añadir
     * @param networkSwitch switch destino
     * @return el {@link Switch} con la red incorporada
     */
    Switch addNetworkToSwitch(Network network, Switch networkSwitch);

    /**
     * Elimina una red de un switch.
     *
     * @param network       red a eliminar
     * @param networkSwitch switch de origen
     * @return el {@link Switch} sin la red
     */
    Switch removeNetworkFromSwitch(Network network, Switch networkSwitch);
}