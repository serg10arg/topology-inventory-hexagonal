package com.example.topologyinventory.framework.adapters.input.rest.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para conectar un router a un core router. Solo transporta las dos
 * identidades implicadas como {@code String}; el adapter las recupera del almacenamiento
 * (por su id) antes de delegar en el caso de uso.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddRouterRequest {

    private String routerId;
    private String coreRouterId;
}
