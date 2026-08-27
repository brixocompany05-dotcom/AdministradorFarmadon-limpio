package com.app.administradorfarmadon.organizacion.datos

/**
 * Identidad determinística de la organización derivada del ID empresarial
 * (cada país lo llama distinto: RUC, NIT, RFC, RUT…).
 * CONTRATO CON BRIXOPANEL: el panel estampa este MISMO orgId al aprobar,
 * así ambas apps lo derivan sin coordinación entre equipos.
 */
object OrgId {
    fun desdeIdEmpresarial(idEmpresarial: String): String {
        val limpio = idEmpresarial.filter { it.isLetterOrDigit() }.uppercase()
        return "org_$limpio"
    }
}
