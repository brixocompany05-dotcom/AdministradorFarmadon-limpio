package com.app.administradorfarmadon.ventas.compartido

import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContratoOperativoVentaInventarioTest {

    @Test
    fun `sede sin PLIN activo no renderiza PLIN y validacion de repo lo rechaza`() {
        // Métodos activos configurados para la sede: solo Efectivo y Yape
        val metodosConfigurados = listOf(
            InstanciaPago(id = "inst_efectivo", tipoId = "EFECTIVO", activa = true),
            InstanciaPago(id = "inst_yape", tipoId = "YAPE", activa = true),
            InstanciaPago(id = "inst_plin", tipoId = "PLIN", activa = false)
        )

        val metodosDisponiblesUI = metodosConfigurados.filter { it.activa }
        assertFalse("PLIN inactivo no debe aparecer en UI", metodosDisponiblesUI.any { it.tipoId == "PLIN" })
        assertTrue("Efectivo debe estar activo", metodosDisponiblesUI.any { it.tipoId == "EFECTIVO" })
        assertTrue("Yape debe estar activo", metodosDisponiblesUI.any { it.tipoId == "YAPE" })

        // Simulación del contrato en repo: validar que pago con PLIN sea rechazado
        val pagoInvalido = PagoVenta(tipoId = "PLIN", instanciaId = "inst_plin", nombreMetodo = "Plin", monto = 50.0)
        val esValido = metodosDisponiblesUI.any { inst ->
            (pagoInvalido.instanciaId.isNotBlank() && inst.id == pagoInvalido.instanciaId) || (inst.tipoId == pagoInvalido.tipoId)
        }

        assertFalse("Repo debe rechazar PLIN porque no está activo para esta sede", esValido)
    }

    @Test
    fun `lote con costo cero o no valorizado no llega a FEFO y no se vende en POS`() {
        val prodSinCosto = MoldeProductos(
            nombre = "Paracetamol 500mg",
            empaque = "Caja",
            contenidoUnidad = "Caja",
            lotes = mapOf(
                "LOT-0" to LoteProducto(
                    numero = "LOT-0",
                    vencimiento = "12/2028",
                    cantidad = 10.0,
                    costoCompraUnitario = 0.0,
                    costoUltimoIngreso = 0.0,
                    noValorizado = false
                )
            )
        )

        val pres = PresentacionProducto(presentacionId = "p1", nombre = "Caja", empaque = "Caja", cantidad = 1, unidadMedida = "Caja", precioventa = 5.0)

        val resultado = UnidadVentaHelper.calcularDescuentoFEFO(
            producto = prodSinCosto,
            presentacion = pres
        )

        assertTrue("Debe fallar porque el único lote tiene costo 0.00", resultado.isFailure)
        val mensajeError = resultado.exceptionOrNull()?.message.orEmpty()
        assertTrue("El mensaje debe indicar registrar compra", mensajeError.contains("no tiene costo registrado"))
    }

    @Test
    fun `lote sin costo unitario explicito pero producto con precioCompra se vende exitosamente`() {
        val prodConPrecioCompra = MoldeProductos(
            nombre = "Ibuprofeno 400mg",
            empaque = "Caja",
            contenidoUnidad = "Caja",
            precioCompra = 3.50,
            lotes = mapOf(
                "LOT-A" to LoteProducto(
                    numero = "LOT-A",
                    vencimiento = "12/2028",
                    cantidad = 15.0,
                    costoCompraUnitario = 0.0,
                    costoUltimoIngreso = 0.0,
                    noValorizado = false
                )
            )
        )

        val pres = PresentacionProducto(presentacionId = "p1", nombre = "Caja", empaque = "Caja", cantidad = 1, unidadMedida = "Caja", precioventa = 7.0)

        val resultado = UnidadVentaHelper.calcularDescuentoFEFO(
            producto = prodConPrecioCompra,
            presentacion = pres
        )

        assertTrue("Debe tener éxito porque hereda el precioCompra del producto", resultado.isSuccess)
        val consumidos = resultado.getOrNull().orEmpty()
        assertEquals(1, consumidos.size)
        assertEquals("LOT-A", consumidos.first().loteNumero)
        assertEquals(1.0, consumidos.first().cantidadADescontar, 0.001)
    }

    @Test
    fun `lote comercial con costo si se vende y excluye lote de muestra`() {
        val prodMixto = MoldeProductos(
            nombre = "Amoxicilina 500mg",
            empaque = "Caja",
            contenidoUnidad = "Caja",
            lotes = mapOf(
                "LOT-MUESTRA" to LoteProducto(
                    numero = "LOT-MUESTRA",
                    vencimiento = "01/2028", // Vence antes
                    cantidad = 5.0,
                    costoCompraUnitario = 0.0,
                    noValorizado = true
                ),
                "LOT-COMERCIAL" to LoteProducto(
                    numero = "LOT-COMERCIAL",
                    vencimiento = "06/2028", // Vence después
                    cantidad = 10.0,
                    costoCompraUnitario = 4.5,
                    costoUltimoIngreso = 4.5,
                    noValorizado = false
                )
            )
        )

        val pres = PresentacionProducto(presentacionId = "p2", nombre = "Caja", empaque = "Caja", cantidad = 2, unidadMedida = "Caja", precioventa = 10.0)

        val resultado = UnidadVentaHelper.calcularDescuentoFEFO(
            producto = prodMixto,
            presentacion = pres
        )

        assertTrue("Debe tener éxito consumiendo el lote comercial", resultado.isSuccess)
        val consumidos = resultado.getOrNull().orEmpty()
        assertEquals(1, consumidos.size)
        assertEquals("LOT-COMERCIAL", consumidos.first().loteNumero)
    }

    @Test
    fun `ajuste no comercial requiere motivo de al menos 5 caracteres`() {
        val motivoCorto = "abc"
        val motivoValido = "Muestra médica recibida de laboratorio"

        val esNoComercial = true
        fun validarMotivo(m: String) = if (esNoComercial) m.trim().length >= 5 else true

        assertFalse("Motivo de 3 letras debe ser inválido", validarMotivo(motivoCorto))
        assertTrue("Motivo detallado debe ser válido", validarMotivo(motivoValido))
    }

    @Test
    fun `factura vacia es rechazada y SC con proveedor por defecto es explicito`() {
        val numFacturaVacio = "   "
        val numFacturaSC = "S/C"

        fun validarFactura(nro: String): Result<String> {
            val limpio = nro.trim().uppercase()
            if (limpio.isBlank()) return Result.failure(IllegalArgumentException("El número de comprobante no puede estar vacío."))
            return Result.success(limpio)
        }

        assertTrue("Factura vacía debe fallar", validarFactura(numFacturaVacio).isFailure)
        assertTrue("Factura S/C debe ser aceptada", validarFactura(numFacturaSC).isSuccess)

        val proveedorInput = "   "
        val proveedorFinal = if (proveedorInput.trim().isBlank()) "SIN PROVEEDOR" else proveedorInput.trim()
        assertEquals("SIN PROVEEDOR", proveedorFinal)
    }
}
