package com.example.topologyinventory.framework.adapters.input.rest.request;

import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.SwitchType;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para crear un switch (en memoria). Transporta los datos crudos; el
 * adapter los traduce al dominio y delega en el caso de uso. La IP viaja como {@code String}
 * y la ubicación como {@link LocationRequest}, igual que en el alta de routers.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateSwitchRequest {

    private Vendor vendor;
    private Model model;
    private String ip;
    private LocationRequest location;
    private SwitchType switchType;
}
