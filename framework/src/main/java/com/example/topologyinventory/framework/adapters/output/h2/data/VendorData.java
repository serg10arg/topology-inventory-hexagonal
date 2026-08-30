package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.Embeddable;

/**
 * Espejo de persistencia del value object {@code Vendor} del dominio.
 *
 * Vive en el hexágono de framework para que el modelo de base de datos no
 * dependa del dominio; la traducción entre ambos mundos la hace el mapper.
 */
@Embeddable
public enum VendorData {
    CISCO, NETGEAR, HP, TPLINK, DLINK, JUNIPER
}
