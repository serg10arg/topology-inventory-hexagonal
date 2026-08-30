# Paquete `entity` — Entidades y agregado

> Los objetos del dominio que tienen **identidad y ciclo de vida**: routers y
> switches. Aquí no solo se guardan datos; aquí viven las **reglas** que gobiernan
> cómo se conectan y desconectan los elementos de la red.

Una analogía: el agregado es una **muñeca rusa**. Un `CoreRouter` contiene otros
routers; un `EdgeRouter` contiene switches; un `Switch` contiene redes. Abrir una
pieza revela la siguiente. Y hay una pieza que manda sobre todas: la raíz del
agregado (el `CoreRouter`), que es quien garantiza que el conjunto se mantenga
coherente.

## ¿Qué se implementó?

La jerarquía de equipos de red y las operaciones que los relacionan.

| Pieza | Qué es | Rol |
|---|---|---|
| **Base común** | `Equipment` | Identidad y atributos compartidos (id, vendor, model, ip, location). |
| **Jerarquía de routers** | `Router` (abstracto), `CoreRouter`, `EdgeRouter` | El troncal (`CoreRouter`, raíz del agregado) y los routers de borde. |
| **Switch** | `Switch` | Equipo que agrupa redes dentro de un `EdgeRouter`. |

## ¿Por qué se implementó así?

- **Para juntar identidad y comportamiento.** Una entity no es una bolsa de datos:
  sabe conectarse, desconectarse y proteger sus propias reglas. Es un modelo de
  dominio rico, no anémico.
- **Para tener una única puerta de coherencia.** El `CoreRouter` es la raíz del
  agregado: los cambios sobre el conjunto pasan por él, lo que evita estados
  inconsistentes.
- **Para aplicar las reglas en el momento justo.** Antes de conectar o eliminar, la
  entity consulta una *specification* como guarda; la regla se cumple o la operación
  no ocurre.

## ¿Cómo se implementó?

- Con **herencia**: `Equipment` → `Router` → `CoreRouter`/`EdgeRouter`, y
  `Equipment` → `Switch`.
- Las entities son inmutables salvo sus **colecciones internas** (los mapas de
  routers/switches), que se inicializan por defecto para no quedar nunca en estado
  nulo.
- Los **métodos de negocio** (`addRouter`, `addSwitch`, `addNetworkToSwitch`…)
  validan con especificaciones antes de mutar el estado.

## ¿Cuál es su responsabilidad?

**Mantener el estado y hacer cumplir las reglas del agregado.**

| Sí es responsabilidad de `entity` | NO es responsabilidad de `entity` |
|---|---|
| Mantener identidad y estado propio | Construirse a sí misma por tipo (lo hace `factory`) |
| Aplicar las reglas de conexión/eliminación | Consultar colecciones externas (lo hace `service`) |
| Proteger los invariantes del agregado | Saber de bases de datos o frameworks |

## Estado actual

Implementado y con pruebas unitarias que cubren la creación y las conexiones del
agregado.

## ¿Cómo se relaciona con el proyecto?

Es el corazón del núcleo:

```
   factory  ──▶  entity  ◀──  specification (reglas)
                   │  ▲
       service ────┘  └──── vo (se compone de value objects)
```

- Las crea la **`factory`**, las consultan los **`service`**, las validan las
  **`specification`**, y el **framework** las traduce a persistencia.

> Para el propósito y el estado del módulo completo, consulta el
> **README del módulo `domain`**.