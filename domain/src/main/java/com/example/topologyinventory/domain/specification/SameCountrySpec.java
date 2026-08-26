package com.example.topologyinventory.domain.specification;

import com.example.topologyinventory.domain.entity.CoreRouter;
import com.example.topologyinventory.domain.entity.Equipment;
import com.example.topologyinventory.domain.exception.GenericSpecificationException;
import com.example.topologyinventory.domain.specification.shared.AbstractSpecification;

/**
 * Regla: un equipo solo puede conectarse a un router si ambos están en el mismo
 * país. Excepción: dos core routers pueden conectarse aunque estén en países
 * distintos (la troncal es transnacional); por eso la regla se da por cumplida
 * cuando el equipo evaluado es un {@link CoreRouter}.
 */
public class SameCountrySpec extends AbstractSpecification<Equipment> {

    /** Equipo de referencia contra el que se compara el país. */
    private final Equipment equipment;

    public SameCountrySpec(Equipment equipment) {
        this.equipment = equipment;
    }

    @Override
    public boolean isSatisfiedBy(Equipment anyEquipment) {
        // Los core routers pueden interconectarse entre países: regla satisfecha.
        if (anyEquipment instanceof CoreRouter) {
            return true;
        } else if (anyEquipment != null && this.equipment != null) {
            return this.equipment.getLocation().getCountry()
                    .equals(anyEquipment.getLocation().getCountry());
        } else {
            return false;
        }
    }

    @Override
    public void check(Equipment equipment) throws GenericSpecificationException {
        if (!isSatisfiedBy(equipment))
            throw new GenericSpecificationException("The equipments should be in the same country");
    }
}