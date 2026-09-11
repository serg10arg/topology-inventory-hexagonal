package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Espejo de persistencia de la entidad {@code Switch} del dominio.
 *
 * Modela la tabla {@code switches} y su relación con {@code networks}. Guarda el
 * {@code router_id} del EdgeRouter al que pertenece: la persistencia del switch
 * ocurre siempre a través de la raíz del agregado (el router), nunca de forma
 * independiente.
 *
 * <p>Persistencia gestionada por Hibernate Reactive. El id y la FK al router se declaran como
 * {@link String} sobre columnas {@code VARCHAR(36)} (Fase 8; se retiró el
 * {@code columnDefinition = "uuid"} de H2), los enums con {@link Enumerated}, y la colección
 * {@link OneToMany} de redes va en solo lectura sobre {@code networks.switch_id} y se navega
 * con {@code session.fetch}.
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "switches")
public class SwitchData implements Serializable {

    @Id
    @Column(name = "switch_id", length = 36, updatable = false)
    private String switchId;

    @Column(name = "router_id", length = 36)
    private String routerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "switch_vendor")
    private VendorData switchVendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "switch_model")
    private ModelData switchModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "switch_type")
    private SwitchTypeData switchType;

    @OneToMany
    @JoinColumn(name = "switch_id", insertable = false, updatable = false)
    private List<NetworkData> networks;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address",
                    column = @Column(name = "switch_ip_address")),
            @AttributeOverride(name = "protocol",
                    column = @Column(name = "switch_ip_protocol")),
    })
    private IPData ip;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address",
                    column = @Column(name = "location_address")),
            @AttributeOverride(name = "city",
                    column = @Column(name = "location_city")),
            @AttributeOverride(name = "state",
                    column = @Column(name = "location_state")),
            @AttributeOverride(name = "zipcode",
                    column = @Column(name = "location_zipcode")),
            @AttributeOverride(name = "country",
                    column = @Column(name = "location_country")),
            @AttributeOverride(name = "latitude",
                    column = @Column(name = "location_latitude")),
            @AttributeOverride(name = "longitude",
                    column = @Column(name = "location_longitude")),
    })
    private LocationData switchLocation;
}
