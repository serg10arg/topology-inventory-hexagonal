package com.example.topologyinventory.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Value object que representa una dirección IP y su protocolo. El protocolo
 * (IPv4/IPv6) se deriva automáticamente de la longitud de la dirección al
 * construir el objeto.
 */
@Getter
@ToString
@EqualsAndHashCode
public class IP {

    private String ipAddress;
    private Protocol protocol;

    /**
     * @param ipAddress dirección IP (no nula); determina el protocolo
     * @throws IllegalArgumentException si la dirección es nula
     */
    public IP(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("Null IP address");
        }
        this.ipAddress = ipAddress;
        if (ipAddress.length() <= 15) {
            this.protocol = Protocol.IPV4;
        } else {
            this.protocol = Protocol.IPV6;
        }
    }

    /**
     * Factory de conveniencia para crear una IP a partir de su dirección. Se lee
     * mejor en el código cliente ({@code IP.fromAddress("10.0.0.1")}).
     *
     * @param ipAddress dirección IP
     * @return una nueva instancia de {@code IP}
     */
    public static IP fromAddress(String ipAddress) {
        return new IP(ipAddress);
    }
}