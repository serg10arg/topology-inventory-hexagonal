package com.example.topologyinventory.framework.adapters.input.rest.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para desconectar un router de un core router. Comparte forma con
 * {@link AddRouterRequest} pero se nombra por su intención: mantiene la frontera REST
 * legible (el endpoint de baja no reutiliza el tipo del endpoint de alta).
 */
@Getter
@Setter
@NoArgsConstructor
public class RemoveRouterRequest {

    private String routerId;
    private String coreRouterId;
}
