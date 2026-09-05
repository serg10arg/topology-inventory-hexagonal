package com.example.topologyinventory.framework.adapters.input.rest.response;

import com.example.topologyinventory.domain.entity.Switch;
import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.SwitchType;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.Getter;

import java.util.List;

/**
 * DTO de salida de un switch. Expone sus datos y sus redes como una lista de
 * {@link NetworkResponse}. A diferencia de los hijos de un router (proyectados como ids),
 * las redes se serializan completas: son objetos de valor pequeños, ya materializados por
 * el mapper de persistencia, sin colecciones perezosas que navegar.
 *
 * <p>El {@code from(...)} se invoca dentro del método {@code @Blocking} del adapter, con el
 * switch ya materializado (recuperado como hijo de su edge router), de modo que aquí solo
 * se copian datos.
 */
@Getter
public class SwitchResponse {

    private String id;
    private SwitchType switchType;
    private Vendor vendor;
    private Model model;
    private String ip;
    private LocationResponse location;
    private List<NetworkResponse> networks = List.of();

    /** Proyecta un {@link Switch} del dominio a su DTO de salida, con sus redes anidadas. */
    public static SwitchResponse from(Switch networkSwitch) {
        var response = new SwitchResponse();
        response.id = networkSwitch.getId().getId().toString();
        response.switchType = networkSwitch.getSwitchType();
        response.vendor = networkSwitch.getVendor();
        response.model = networkSwitch.getModel();
        response.ip = networkSwitch.getIp().getIpAddress();
        response.location = LocationResponse.from(networkSwitch.getLocation());
        response.networks = networkSwitch.getSwitchNetworks().stream()
                .map(NetworkResponse::from)
                .toList();
        return response;
    }
}
