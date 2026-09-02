package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Espejo de persistencia del value object {@code Network} del dominio.
 *
 * Modela la tabla {@code networks}. A diferencia de router y switch, su clave es
 * un entero autogenerado por la base de datos ({@link GenerationType#IDENTITY}),
 * no un UUID: por eso el seed inserta redes sin aportar {@code network_id}. Se
 * persiste como parte del switch que la contiene.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "networks")
public class NetworkData implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "network_id")
    private int id;

    @Column(name = "switch_id")
    private UUID switchId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address",
                    column = @Column(name = "network_address")),
            @AttributeOverride(name = "protocol",
                    column = @Column(name = "network_protocol")),
    })
    IPData ip;

    @Column(name = "network_name")
    String name;

    @Column(name = "network_cidr")
    Integer cidr;

    /**
     * Constructor de conveniencia usado por el mapper al reconstruir las redes
     * de un switch (el {@code network_id} lo genera la base de datos).
     */
    public NetworkData(UUID switchId, IPData ip, String name, Integer cidr) {
        this.switchId = switchId;
        this.ip = ip;
        this.name = name;
        this.cidr = cidr;
    }
}
