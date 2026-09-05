package com.example.topologyinventory.framework.adapters.input.rest.request;

import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.SwitchType;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para crear un switch y conectarlo a un edge router en un solo paso.
 * Como en este núcleo un switch no se persiste ni se recupera de forma independiente
 * (solo existe dentro del agregado de un router), el alta parte de los datos del switch
 * y del id del edge router destino: el adapter recupera el edge, crea el switch y lo conecta.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddSwitchRequest {

    private Vendor vendor;
    private Model model;
    private String ip;
    private LocationRequest location;
    private SwitchType switchType;
    private String edgeRouterId;
}
