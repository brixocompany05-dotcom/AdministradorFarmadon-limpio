package com.app.administradorfarmadon.organizacion.modelo

/**
 * Decisiones del dueño grabadas en el esquema (Fase 0):
 *  1. ROL dice qué puede hacer; ALCANCE dice sobre qué.
 *  2. DUEÑO siempre TODAS y no puede ser reducido.
 *  3. Nadie otorga un alcance mayor al propio.
 *  4. "Todas" incluye las sedes que se creen en el futuro.
 */
enum class RolOrg { DUENO, ADMIN, OPERADOR }

data class AlcanceOrg(
    val todas: Boolean = false,
    val sedesPermitidas: List<String> = emptyList()
) {
    fun incluye(farmaciaId: String): Boolean =
        todas || farmaciaId in sedesPermitidas

    companion object {
        val TODAS = AlcanceOrg(todas = true)
        fun solo(sedes: List<String>) = AlcanceOrg(todas = false, sedesPermitidas = sedes)
    }
}

data class MiembroOrg(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val rolOrg: RolOrg = RolOrg.OPERADOR,
    val alcance: AlcanceOrg = AlcanceOrg.TODAS,
    val activo: Boolean = true
)

/** Perfil de cada local. El país define la moneda operativa automáticamente. */
data class PerfilFarmacia(
    val farmaciaId: String = "",
    val nombre: String = "",
    val ruc: String = "",
    val direccion: String = "",
    val pais: String = "Perú",
    val monedaOperativa: String = "PEN"
)

/**
 * La pertenencia de una persona a UNA organización, tal como viaja en sesión.
 * Serialización defensiva: datos corruptos jamás tumban la app (devuelven vacío/por defecto).
 */
data class MembresiaSesion(
    val orgId: String = "",
    val rolOrg: RolOrg = RolOrg.OPERADOR,
    val alcance: AlcanceOrg = AlcanceOrg.TODAS
) {
    fun toJson(): String {
        val sedes = org.json.JSONArray().apply { alcance.sedesPermitidas.forEach { put(it) } }
        return org.json.JSONObject()
            .put("orgId", orgId)
            .put("rol", rolOrg.name)
            .put("todas", alcance.todas)
            .put("sedes", sedes)
            .toString()
    }

    companion object {
        fun listaDesdeJson(json: String): List<MembresiaSesion> = runCatching {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val rol = runCatching { RolOrg.valueOf(o.optString("rol")) }.getOrDefault(RolOrg.OPERADOR)
                val sedes = mutableListOf<String>()
                val sArr = o.optJSONArray("sedes")
                if (sArr != null) for (j in 0 until sArr.length()) sedes.add(sArr.optString(j))
                MembresiaSesion(
                    orgId = o.optString("orgId"),
                    rolOrg = rol,
                    alcance = if (o.optBoolean("todas")) AlcanceOrg.TODAS else AlcanceOrg.solo(sedes)
                )
            }.filter { it.orgId.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
