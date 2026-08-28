package com.example.topologyinventory.application.usecases;

import com.example.topologyinventory.domain.entity.EdgeRouter;
import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.*;

/**
 * Puerto de entrada para la gestión de switches. Un switch solo puede conectarse
 * a un {@link EdgeRouter}, de modo que este contrato cubre la creación de
 * switches y su conexión y desconexión respecto de un edge router.
 */
public interface SwitchManagementUseCase {

    /**
     * Crea un switch sin persistirlo.
     *
     * @param vendor     fabricante del switch
     * @param model      modelo del switch
     * @param ip         dirección IP asignada
     * @param location   ubicación física
     * @param switchType tipo de switch (LAYER2 o LAYER3)
     * @return el {@link Switch} recién creado
     */
    Switch createSwitch(Vendor vendor, Model model, IP ip, Location location, SwitchType switchType);

    /**
     * Conecta un switch a un edge router.
     *
     * @param networkSwitch switch a conectar
     * @param edgeRouter    edge router destino
     * @return el {@link EdgeRouter} con el switch ya incorporado
     */
    EdgeRouter addSwitchToEdgeRouter(Switch networkSwitch, EdgeRouter edgeRouter);

    /**
     * Desconecta un switch de un edge router.
     *
     * @param networkSwitch switch a desconectar
     * @param edgeRouter    edge router de origen
     * @return el {@link EdgeRouter} sin el switch
     */
    EdgeRouter removeSwitchFromEdgeRouter(Switch networkSwitch, EdgeRouter edgeRouter);
}