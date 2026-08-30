package com.example.topologyinventory.framework.adapters.output.h2.data;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

import java.sql.Types;
import java.util.UUID;

/**
 * Converter de EclipseLink que mapea la columna {@code UUID} de H2 al tipo
 * {@link java.util.UUID} de Java y viceversa.
 *
 * Se registra en las entidades con {@code @Converter}/{@code @Convert}. Es
 * detalle de la frontera tecnológica: aísla al resto del sistema de cómo H2
 * representa los identificadores.
 */
public class UUIDTypeConverter implements Converter {

    @Override
    public UUID convertObjectValueToDataValue(Object objectValue, Session session) {
        return (UUID) objectValue;
    }

    @Override
    public UUID convertDataValueToObjectValue(Object dataValue, Session session) {
        return (UUID) dataValue;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public void initialize(DatabaseMapping mapping, Session session) {
        DatabaseField field = mapping.getField();
        field.setSqlType(Types.OTHER);
        field.setTypeName("java.util.UUID");
        field.setColumnDefinition("UUID");
    }
}
