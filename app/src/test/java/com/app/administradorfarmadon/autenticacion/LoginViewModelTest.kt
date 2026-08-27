package com.app.administradorfarmadon.autenticacion

import android.app.Application
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.app.administradorfarmadon.appconexioninternet.NetworkHealthMonitor
import com.app.administradorfarmadon.appconexioninternet.NetworkStatus
import com.app.administradorfarmadon.autenticacion.login.datos.ListaNegraRepository
import com.app.administradorfarmadon.autenticacion.login.datos.ErrorVerificacionListaNegra
import com.app.administradorfarmadon.autenticacion.login.datos.LoginIncidenteTipo
import com.app.administradorfarmadon.autenticacion.login.logica.LoginViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestRule

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LoginViewModel
    private val application = mockk<Application>(relaxed = true)

    private val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    private val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
    private val mockUser = mockk<FirebaseUser>(relaxed = true)
    
    private val mockFarmaciappCol = mockk<CollectionReference>(relaxed = true)
    private val mockAppDoc = mockk<DocumentReference>(relaxed = true)
    private val mockSolCol = mockk<CollectionReference>(relaxed = true)
    private val mockDocRef = mockk<DocumentReference>(relaxed = true)
    private val mockUserCol = mockk<CollectionReference>(relaxed = true)
    private val mockUserDocRef = mockk<DocumentReference>(relaxed = true)
    private val mockFarmaciasCol = mockk<CollectionReference>(relaxed = true)
    private val mockFarmaciaDocRef = mockk<DocumentReference>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0

        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test-uid"
        
        // Mock de red
        mockkObject(NetworkHealthMonitor)
        every { NetworkHealthMonitor.status } returns MutableStateFlow(NetworkStatus.CONECTADO)

        // Mock de ListaNegraRepository
        mockkObject(ListaNegraRepository)
        coEvery { ListaNegraRepository.consultar(any(), any(), any()) } returns null

        // Mock de cadena Firestore por defecto
        every { mockFirestore.collection("farmaciapp") } returns mockFarmaciappCol
        every { mockFarmaciappCol.document("app") } returns mockAppDoc
        every { mockAppDoc.collection("solicitudes") } returns mockSolCol
        every { mockAppDoc.collection("usuarios_farmacia") } returns mockUserCol
        every { mockAppDoc.collection("farmacias") } returns mockFarmaciasCol
        every { mockUserCol.document(any()) } returns mockUserDocRef
        every { mockFarmaciasCol.document(any()) } returns mockFarmaciaDocRef

        viewModel = LoginViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `iniciarSesion bloquea post-signIn si email esta en lista negra`() = runTest {
        viewModel.onUsuarioChange("baneado@test.com")
        viewModel.onContrasenaChange("Pass1234")

        // Mock Auth exitoso
        val mockAuthResult = mockk<AuthResult>()
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)
        every { mockAuth.currentUser?.uid } returns "test-uid"

        // Mock BAN detectado post-signIn
        val banDoc = mockk<DocumentSnapshot>()
        every { banDoc.getString("motivo") } returns "Usuario Malicioso"
        coEvery { ListaNegraRepository.consultar(any(), "", "baneado@test.com") } returns banDoc

        viewModel.iniciarSesion()
        advanceUntilIdle()

        // Debe haber echado al usuario
        verify { mockAuth.signOut() }
        assertEquals(LoginIncidenteTipo.USUARIO_BANEADO, viewModel.uiState.value.incidente?.tipo)
        assertEquals("Usuario Malicioso", viewModel.uiState.value.incidente?.mensaje)
    }

    @Test
    fun `credenciales invalidas en Auth responden CREDENCIALES_INCORRECTAS`() = runTest {
        viewModel.onUsuarioChange("noexiste@test.com")
        viewModel.onContrasenaChange("Pass1234")

        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(
            com.google.firebase.auth.FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "User not found")
        )
        every { mockAuth.currentUser } returns null

        viewModel.iniciarSesion()
        advanceUntilIdle()

        // Respuesta clara y directa al usuario
        assertEquals(LoginIncidenteTipo.CREDENCIALES_INCORRECTAS, viewModel.uiState.value.incidente?.tipo)
        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun `iniciarSesion aplica fail-closed si falla verificacion de ban`() = runTest {
        viewModel.onUsuarioChange("test@test.com")
        viewModel.onContrasenaChange("Pass1234")

        val mockAuthResult = mockk<AuthResult>()
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)
        every { mockAuth.currentUser?.uid } returns "test-uid"

        // Mock fallo en consulta
        coEvery { ListaNegraRepository.consultar(any(), any(), any()) } throws ErrorVerificacionListaNegra(Exception("Red fallida"))

        viewModel.iniciarSesion()
        advanceUntilIdle()

        // Debe haber bloqueado por error de base de datos (fail-closed)
        assertEquals(LoginIncidenteTipo.ERROR_BASE_DATOS, viewModel.uiState.value.incidente?.tipo)
    }

    @Test
    fun `recuperar contrasena envia email a Firebase Auth`() = runTest {
        every { mockAuth.sendPasswordResetEmail("admin@farmacia.com") } returns Tasks.forResult(null)

        viewModel.recuperarContrasena("admin@farmacia.com")
        advanceUntilIdle()

        verify { mockAuth.sendPasswordResetEmail("admin@farmacia.com") }
        assertEquals("Correo Enviado", viewModel.uiState.value.mensajeDialogo?.first)
    }
}
