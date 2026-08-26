package com.example.topologyinventory.domain.specification;

import com.example.topologyinventory.domain.entity.Equipment;
import com.example.topologyinventory.domain.exception.GenericSpecificationException;
import com.example.topologyinventory.domain.specification.shared.AbstractSpecification;

/**
 * Regla: no se pueden conectar dos equipos que compartan la misma dirección IP.
 * Compara el equipo de referencia con cualquier otro equipo candidato.
 */
public class SameIpSpec extends AbstractSpecification<Equipment> {

    /** Equipo de referencia cuya IP no debe repetirse. */
    private final Equipment equipment;

    public SameIpSpec(Equipment equipment) {
        this.equipment = equipment;
    }

    @Override
    public boolean isSatisfiedBy(Equipment anyEquipment) {
        return !equipment.getIp().equals(anyEquipment.getIp());
    }

    @Override
    public void check(Equipment equipment) {
        if (!isSatisfiedBy(equipment))
            throw new GenericSpecificationException("It's not possible to attach routers with the same IP");
    }
}