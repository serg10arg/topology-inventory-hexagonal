package com.example.topologyinventory.framework.adapters.input.rest.request;

import com.example.topologyinventory.domain.vo.Model;
import com.example.topologyinventory.domain.vo.RouterType;
import com.example.topologyinventory.domain.vo.Vendor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para crear (y persistir) un router. Transporta los datos crudos que
 * el cliente envía por HTTP; el adapter los traduce al dominio y llama al caso de uso.
 *
 * <p>Los enums del dominio ({@link Vendor}, {@link Model}, {@link RouterType}) viajan
 * como tales: Jackson los mapea desde/hacia su nombre sin necesidad de conversores. La
 * IP viaja como {@code String} y se envuelve en el value object {@code IP} en el adapter.
 * La ubicación se anida como {@link LocationRequest} para no arrastrar el {@code Location}
 * del dominio (sin constructor sin-args) a la deserialización.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateRouterRequest {

    private Vendor vendor;
    private Model model;
    private String ip;
    private LocationRequest location;
    private RouterType routerType;
}
