package com.example.topologyinventory.application.ports.input;

import com.example.topologyinventory.application.usecases.SwitchManagementUseCase;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.*;

/**
 * Application service que implementa {@link SwitchManagementUseCase}. Construye
 * switches mediante su builder y delega la conexión y desconexión en el agregado
 * {@link EdgeRouter}, único router que admite switches.
 */
public class SwitchManagementInputPort implements SwitchManagementUseCase {

    /** {@inheritDoc} Genera una identidad nueva para el switch mediante {@code Id.withoutId()}. */
    @Override
    public Switch createSwitch(Vendor vendor, Model model, IP ip, Location location, SwitchType switchType) {
        return Switch.builder()
                .id(Id.withoutId())
                .vendor(vendor).model(model).ip(ip)
                .location(location).switchType(switchType)
                .build();
    }

    /** {@inheritDoc} La validación de la conexión ocurre dentro del edge router. */
    @Override
    public EdgeRouter addSwitchToEdgeRouter(Switch networkSwitch, EdgeRouter edgeRouter) {
        edgeRouter.addSwitch(networkSwitch);
        return edgeRouter;
    }

    /** {@inheritDoc} La validación de la desconexión ocurre dentro del edge router. */
    @Override
    public EdgeRouter removeSwitchFromEdgeRouter(Switch networkSwitch, EdgeRouter edgeRouter) {
        edgeRouter.removeSwitch(networkSwitch);
        return edgeRouter;
    }
}