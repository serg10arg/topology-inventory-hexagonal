package com.example.topologyinventory.framework.adapters.input.generic;

import com.example.topologyinventory.application.usecases.NetworkManagementUseCase;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Network;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adapter de entrada (<em>driving</em>) para la gestión de redes.
 *
 * Reenvía las peticiones al caso de uso de redes. Las redes viven dentro de un
 * {@link Switch}, así que las operaciones reciben el switch destino directamente; su
 * persistencia ocurre con el agregado del router. Es la base del futuro adapter REST.
 *
 * <p><b>Cableado (CDI).</b> Es un bean {@code @ApplicationScoped} y el caso de uso llega por
 * {@code @Inject}: Arc provee el bean que implementa {@link NetworkManagementUseCase}
 * ({@code NetworkManagementInputPort}). Sustituye al {@code new NetworkManagementInputPort()}
 * del constructor anterior.
 */
@ApplicationScoped
public class NetworkManagementGenericAdapter {

    @Inject
    NetworkManagementUseCase networkManagementUseCase;

    /**
     * POST /network/create — crea una red (sin asociarla todavía a un switch).
     *
     * @return la {@link Network} creada
     */
    public Network createNetwork(IP networkAddress, String networkName, int networkCidr) {
        return networkManagementUseCase.createNetwork(networkAddress, networkName, networkCidr);
    }

    /**
     * POST /network/add — añade una red a un switch.
     *
     * @return el {@link Switch} con la red incorporada
     */
    public Switch addNetworkToSwitch(Network network, Switch networkSwitch) {
        return networkManagementUseCase.addNetworkToSwitch(network, networkSwitch);
    }

    /**
     * POST /network/remove — elimina una red de un switch.
     *
     * @return el {@link Switch} sin la red
     */
    public Switch removeNetworkFromSwitch(Network network, Switch networkSwitch) {
        return networkManagementUseCase.removeNetworkFromSwitch(network, networkSwitch);
    }
}
