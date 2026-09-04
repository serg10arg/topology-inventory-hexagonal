package com.example.topologyinventory.application.ports.input;

import com.example.topologyinventory.application.usecases.SwitchManagementUseCase;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.*;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Application service que implementa {@link SwitchManagementUseCase}. Construye switches
 * mediante su builder y delega la conexión y desconexión en el agregado {@link EdgeRouter},
 * único router que admite switches.
 *
 * <p><b>Cableado (CDI).</b> Es un bean {@code @ApplicationScoped}: no inyecta nada (esta
 * rama no tiene puerto de salida —la persistencia va solo por el agregado router—), pero se
 * anota como bean para ser <em>inyectable</em> en {@code SwitchManagementGenericAdapter}.
 * Es una divergencia consciente respecto al libro, cuyo {@code SwitchManagementInputPort}
 * inyecta un {@code SwitchManagementOutputPort} que este núcleo no define.
 */
@ApplicationScoped
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
