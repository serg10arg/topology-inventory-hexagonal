package com.example.topologyinventory.application.ports.input;

import com.example.topologyinventory.application.usecases.NetworkManagementUseCase;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.IP;
import com.example.topologyinventory.domain.vo.Network;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NoArgsConstructor;

/**
 * Application service que implementa {@link NetworkManagementUseCase}. Construye redes
 * mediante su builder y delega en el {@link Switch} el alta y baja de redes, donde se aplican
 * las reglas de CIDR, cupo y no duplicación.
 *
 * <p><b>Cableado (CDI).</b> Es un bean {@code @ApplicationScoped}: no inyecta nada (esta rama
 * no usa puerto de salida), pero se anota como bean para ser <em>inyectable</em> en
 * {@code NetworkManagementGenericAdapter}. Es una divergencia consciente respecto al libro,
 * cuyo {@code NetworkManagementInputPort} inyecta un {@code RouterManagementOutputPort} que
 * este núcleo no consume aquí.
 */
@NoArgsConstructor
@ApplicationScoped
public class NetworkManagementInputPort implements NetworkManagementUseCase {

    /** {@inheritDoc} */
    @Override
    public Network createNetwork(IP networkAddress, String networkName, int networkCidr) {
        return Network.builder()
                .networkAddress(networkAddress)
                .networkName(networkName)
                .networkCidr(networkCidr)
                .build();
    }

    /** {@inheritDoc} Las reglas de negocio se comprueban dentro del switch. */
    @Override
    public Switch addNetworkToSwitch(Network network, Switch networkSwitch) {
        networkSwitch.addNetworkToSwitch(network);
        return networkSwitch;
    }

    /** {@inheritDoc} */
    @Override
    public Switch removeNetworkFromSwitch(Network network, Switch networkSwitch) {
        networkSwitch.removeNetworkFromSwitch(network);
        return networkSwitch;
    }
}
