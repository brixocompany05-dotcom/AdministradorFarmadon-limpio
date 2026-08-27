package com.app.administradorfarmadon.modulos.domain

/** Hijo (sub-función) del catálogo de herramientas. */
data class CatalogoHijo(
    val clave: String = "",
    val nombre: String = "",
    val activo: Boolean = true
)

/** Herramienta/módulo del catálogo compartido (compartido/ecosistema/herramientas_plan). */
data class CatalogoHerramienta(
    val id: String = "",
    val modulo: String = "",        // código canónico (RBAC, rutas, plan.features)
    val nombre: String = "",        // nombre visible
    val categoria: String = "OTROS",
    val orden: Int = 0,
    val icono: String = "",
    val estado: String = "activo",
    val hijos: List<CatalogoHijo> = emptyList()
)

/** Módulo que el cliente REALMENTE puede usar (resuelto). */
data class ModuloResuelto(
    val modulo: String,
    val nombre: String,
    val categoria: String,
    val orden: Int,
    val icono: String,
    val hijosActivos: List<String> = emptyList()
)

/**
 * ÚNICA fuente de verdad de "qué puede usar este cliente".
 *
 * Se calcula SIEMPRE en tiempo real desde 3 datos:
 *  - catálogo (herramientas activas),
 *  - plan (features = códigos de módulo (canónico) o nombres (transicional)),
 *  - overrides del cliente (apagado de herramienta completa por tenant).
 *
 * Reglas de verdad (sin mentiras):
 *  1. Padre visible solo si está en el plan (por código o por nombre), no apagado
 *     por el cliente, y tiene al menos un hijo activo (o es hoja sin hijos).
 *  2. Hijo visible solo si está activado en el catálogo.
 *  3. Apagado el padre completo → desaparece (quien lo consume también saca al
 *     usuario de la herramienta).
 *
 * Coherencia transicional: el plan puede referenciar cada herramienta por su
 * código canónico (`modulo`) o por su `nombre` (planes creados antes de la
 * normalización). Se aceptan AMBOS para features y overrides.
 *
 * Es lógica pura: no toca Firebase. 100% testeable.
 */
object ModulosResueltos {

    fun resolver(
        catalogo: List<CatalogoHerramienta>,
        featuresPlan: Set<String>,
        overridesCliente: Map<String, Boolean> = emptyMap()
    ): List<ModuloResuelto> {
        return catalogo
            .filter { it.estado == "activo" }
            .mapNotNull { tool ->
                // El plan incluye la herramienta por código canónico o por nombre
                // (transicional): se aceptan ambos para no romper planes existentes.
                val estaEnPlan = featuresPlan.contains(tool.modulo) || featuresPlan.contains(tool.nombre)
                if (!estaEnPlan) return@mapNotNull null

                // Apagado completo por el cliente (por código o por nombre) → oculta.
                if (overridesCliente[tool.modulo] == false || overridesCliente[tool.nombre] == false) {
                    return@mapNotNull null
                }

                val hijosActivos = tool.hijos
                    .filter { it.activo }
                    .map { it.clave }

                // Herramienta con hijos pero todos apagados → no se muestra.
                if (tool.hijos.isNotEmpty() && hijosActivos.isEmpty()) return@mapNotNull null

                ModuloResuelto(
                    modulo = tool.modulo,
                    nombre = tool.nombre,
                    categoria = tool.categoria,
                    orden = tool.orden,
                    icono = tool.icono,
                    hijosActivos = hijosActivos
                )
            }
            .sortedBy { it.orden }
    }
}
