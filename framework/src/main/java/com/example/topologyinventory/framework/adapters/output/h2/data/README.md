# Paquete `data` — Modelo de persistencia

> El **espejo de la base de datos**. Un conjunto de clases planas que reproducen,
> en forma de tablas y columnas, la información que en el dominio vive como
> entidades ricas. No contienen reglas: solo estructura orientada al
> almacenamiento.

Una analogía: si una entidad del dominio (`Router`, `Switch`) es la persona real
—con su comportamiento y sus reglas—, una clase `*Data` es su **fotocopia para el
archivo**: los mismos datos, pero recortados y ordenados para caber en el cajón
(la tabla). La fotocopia no sabe hacer nada; solo guarda información en el formato
que el archivo entiende.

## ¿Qué se implementó?

Las clases que definen cómo se guarda cada concepto del dominio en la base de datos.

| Pieza | Qué es | Ejemplos |
|---|---|---|
| **Entidades JPA** (`@Entity`) | Clases que se mapean a una tabla; cada instancia es una fila. | `RouterData`, `SwitchData`, `NetworkData`, `LocationData` |
| **Value objects incrustados** (`@Embeddable`) | Tipos cuyas columnas se incrustan en la tabla de la entidad que los contiene. | `IPData`, y los enums `VendorData`, `ModelData`, `ProtocolData`, `RouterTypeData`, `SwitchTypeData` |
| **Converter** | Traduce un tipo Java a/desde su representación en columna. | `UUIDTypeConverter` (mapea `UUID` ↔ columna `UUID` de H2) |

## ¿Por qué se implementó así?

- **Para separar la forma del dominio de la forma de la base de datos.** Una entidad
  de dominio es rica (tiene comportamiento e invariantes); una fila es plana. Cada
  una tiene su representación óptima, y no se fuerza a una a parecerse a la otra.
- **Para mantener el dominio libre de tecnología.** Las anotaciones JPA viven aquí,
  no en `domain`. El núcleo no sabe que existe una base de datos.
- **Para resolver los desajustes de tipo en la frontera.** El `UUIDTypeConverter`
  existe precisamente para que el identificador del dominio encaje con el tipo de
  columna del motor, sin contaminar a ninguno de los dos lados.

## ¿Cómo se implementó?

- Con anotaciones **`jakarta.persistence`** para el mapeo objeto-relacional, y
  **Lombok** (`@Builder`, `@Getter`…) para evitar código repetitivo.
- El identificador se persiste como `UUID` mediante un *converter* del proveedor.
- El paquete se **abre por reflexión** (`opens`) al proveedor JPA en el
  `module-info` del módulo, porque este accede a los campos privados de las
  entidades para poblarlas.

| Anotación | Rol |
|---|---|
| `@Entity` / `@Table` | Declara una tabla y su nombre |
| `@Id` / `@Column` | Clave primaria y columnas |
| `@Embeddable` / `@Embedded` | Tipos incrustados (IP, enums) |
| `@OneToMany` / `@ManyToOne` | Relaciones entre tablas |
| `@Convert` / `@Converter` | Conversión de tipos (UUID) |

## ¿Cuál es su responsabilidad?

**Representar los datos para la base de datos.** Ser una estructura fiel al esquema
relacional, construible por el proveedor JPA.

| Sí es responsabilidad de `data` | NO es responsabilidad de `data` |
|---|---|
| Reflejar la estructura de tablas y columnas | Contener reglas de negocio (viven en `domain`) |
| Declarar el mapeo objeto-relacional | Validar invariantes del dominio |
| Ser instanciable por el proveedor (reflexión) | Traducir a/desde el dominio (lo hace `mappers`) |

## Estado actual

Implementado y en uso. Estas clases son el destino y el origen de la traducción
que realiza el paquete `mappers`, y el material con el que el output adapter lee y
escribe en H2.

## ¿Cómo se relaciona con el proyecto?

Se sitúa en la frontera de salida, entre el dominio y la base de datos:

```
   domain (entidades)  ◀──  mappers  ──▶  data (este paquete)  ──▶  H2
```

- El paquete **`mappers`** convierte las entidades del dominio en estas clases y
  viceversa.
- El **output adapter** las usa para persistir y recuperar filas en H2.

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `framework`**.