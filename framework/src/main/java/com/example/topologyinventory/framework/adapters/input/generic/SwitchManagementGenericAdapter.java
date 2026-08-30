package com.example.topologyinventory.framework.adapters.input.generic;

import com.example.topologyinventory.application.ports.input.SwitchManagementInputPort;
import com.example.topologyinventory.application.usecases.SwitchManagementUseCase;
import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.*;

/**
 * Adapter de entrada (<em>driving</em>) para la gestión de switches.
 *
 * Reenvía las peticiones al caso de uso de switches, que solo admite conexión a
 * un {@link EdgeRouter}. Como en este sistema los switches se persisten a través
 * del agregado del router (no de forma independiente), este adapter no expone
 * recuperación ni persistencia: crear, conectar y desconectar bastan en esta
 * fase. Es un POJO, base del futuro adapter REST.
 */
public class SwitchManagementGenericAdapter {

    private final SwitchManagementUseCase switchManagementUseCase;

    public SwitchManagementGenericAdapter() {
        this.switchManagementUseCase = new SwitchManagementInputPort();
    }

    /**
     * POST /switch/create — crea un switch (sin persistir).
     *
     * @return el {@link Switch} creado
     */
    public Switch createSwitch(Vendor vendor, Model model, IP ip,
                               Location location, SwitchType switchType) {
        return switchManagementUseCase.createSwitch(vendor, model, ip, location, switchType);
    }

    /**
     * POST /switch/add — conecta un switch a un edge router.
     *
     * @return el {@link EdgeRouter} con el switch ya incorporado
     */
    public EdgeRouter addSwitchToEdgeRouter(Switch networkSwitch, EdgeRouter edgeRouter) {
        return switchManagementUseCase.addSwitchToEdgeRouter(networkSwitch, edgeRouter);
    }

    /**
     * POST /switch/remove — desconecta un switch de un edge router.
     *
     * @return el {@link EdgeRouter} sin el switch
     */
    public EdgeRouter removeSwitchFromEdgeRouter(Switch networkSwitch, EdgeRouter edgeRouter) {
        return switchManagementUseCase.removeSwitchFromEdgeRouter(networkSwitch, edgeRouter);
    }
}