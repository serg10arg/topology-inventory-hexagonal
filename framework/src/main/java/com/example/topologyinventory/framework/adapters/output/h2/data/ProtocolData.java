package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.Embeddable;

/** Espejo de persistencia del value object {@code Protocol} (IPv4 / IPv6). */
@Embeddable
public enum ProtocolData {
    IPV4, IPV6
}
