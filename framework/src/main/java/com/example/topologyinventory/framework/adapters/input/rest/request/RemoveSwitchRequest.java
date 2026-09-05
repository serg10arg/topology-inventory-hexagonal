package com.example.topologyinventory.framework.adapters.input.rest.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para desconectar un switch de un edge router. Ambos se identifican por
 * su id: el adapter recupera el edge y localiza el switch entre sus hijos materializados.
 * (El switch debe estar sin redes; la specification del dominio lo exige.)
 */
@Getter
@Setter
@NoArgsConstructor
public class RemoveSwitchRequest {

    private String edgeRouterId;
    private String switchId;
}
