package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

/**
 * Espejo de persistencia del value object {@code IP} del dominio.
 *
 * Es un tipo {@code @Embeddable}: sus columnas se incrustan en la tabla de la
 * entidad que lo contiene (router, switch o red). Deriva el protocolo de la
 * longitud de la dirección, igual que hace el value object de dominio.
 */
@Embeddable
@Getter
public class IPData {

    private String address;

    @Enumerated(EnumType.STRING)
    @Embedded
    private ProtocolData protocol;

    private IPData(String address) {
        if (address == null)
            throw new IllegalArgumentException("Null IP address");
        this.address = address;
        this.protocol = address.length() <= 15 ? ProtocolData.IPV4 : ProtocolData.IPV6;
    }

    /** Constructor requerido por JPA para instanciar el embeddable por reflexión. */
    public IPData() {
    }

    /**
     * Crea un {@code IPData} a partir de una dirección textual.
     *
     * @param address dirección IP (v4 o v6)
     * @return una nueva instancia de {@code IPData}
     */
    public static IPData fromAddress(String address) {
        return new IPData(address);
    }
}
