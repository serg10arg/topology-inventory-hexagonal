package com.example.topologyinventory.framework.adapters.output.h2.data;

/**
 * Espejo de persistencia del value object {@code Vendor} del dominio.
 *
 * Es un enum básico: se mapea en las entidades con {@code @Enumerated(STRING)}.
 * Vive en el hexágono de framework para que el modelo de base de datos no
 * dependa del dominio; la traducción entre ambos mundos la hace el mapper.
 */
public enum VendorData {
    CISCO, NETGEAR, HP, TPLINK, DLINK, JUNIPER
}
