@file:Suppress("DEPRECATION")

package com.asp.loginhome.login

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.asp.loginhome.R
import com.asp.loginhome.inicio.PantallaPrincipal
import com.asp.loginhome.recursos.BaseApi
import org.json.JSONObject
import java.io.File

class InicioSesion : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvHola: TextView
    private lateinit var nopassword: TextView
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var changeUserButton: Button

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val prefs = "MyPrefs"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @SuppressLint("SdCardPath", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_sesion)

        // Inicializar sharedPreferences y editor aquí
        sharedPreferences = getSharedPreferences(prefs, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        initializeViews()
        setupBiometricAuthentication()

        val idUsuario = sharedPreferences.getString("idUsuario", "")
        val nombre = sharedPreferences.getString("nombre","")
        val apellidoP = sharedPreferences.getString("apellidoP","")
        val apellidoM = sharedPreferences.getString("apellidoM", "")

        if (!siHayInternet()){
            showErrorDialog("Verifique su conexion a Internet")
        }

        //si hay un usuario existente, guardado
        if (!idUsuario.isNullOrEmpty()){
            inicioSesionRapido(idUsuario, nombre, apellidoP, apellidoM)
        }else {
            inicioSesionComun()
        }
    }

    private fun initializeViews() {
        usernameEditText = findViewById(R.id.textUsuario)
        passwordEditText = findViewById(R.id.textPassword)
        loginButton = findViewById(R.id.loginButton)
        changeUserButton = findViewById(R.id.cambiarUser)
        tvNombre = findViewById(R.id.textBienvenida)
        tvHola = findViewById(R.id.textBienvenida1)
        nopassword = findViewById(R.id.textNoPassword)

        val usernameInputFilter = InputFilter { source, _, _, _, _, _ ->
            val regex = "^[a-zA-Z0-9]*$".toRegex()
            if (source.matches(regex)) null else ""
        }

        usernameEditText.filters = arrayOf(usernameInputFilter)
        passwordEditText.filters = arrayOf(InputFilter.LengthFilter(8))
    }

    private fun setupBiometricAuthentication() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, authenticationCallback)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación Biométrica")
            .setDescription("Escanea tu huella digital para autenticarte")
            .setNegativeButtonText("Cancelar")
            .build()
    }

    private fun inicioSesionRapido(idUsuario: String, nombre: String?, apellidoP: String?, apellidoM: String?) {
        tvNombre.visibility = View.VISIBLE
        tvNombre.text = "$nombre\n$apellidoP $apellidoM"
        tvHola.visibility = View.VISIBLE

        passwordEditText.visibility = View.GONE
        usernameEditText.visibility = View.GONE
        nopassword.visibility = View.GONE

        loginButton.setOnClickListener {
            if (!siHayInternet()){
                showErrorDialog("Verifique su conexion a Internet")
            }else {
                if (deviceSupportsBiometrics()) {
                    biometricPrompt.authenticate(promptInfo)
                } else {
                    validarEstado(idUsuario)
                }
            }
        }

        changeUserButton.setOnClickListener {
            editor.clear().remove("idUsuario").apply()
            val preferencesFile = File("/data/data/$packageName/shared_prefs/$prefs.xml")
            if (preferencesFile.exists()) {
                preferencesFile.delete()
            }
            inicioSesionComun()
        }
    }

    private fun inicioSesionComun() {
        tvNombre.visibility = View.GONE
        tvHola.visibility = View.GONE
        changeUserButton.visibility = View.INVISIBLE
        loginButton.visibility = View.VISIBLE
        nopassword.visibility = View.VISIBLE
        usernameEditText.visibility = View.VISIBLE
        passwordEditText.visibility = View.VISIBLE

        nopassword.setOnClickListener {
            if (!siHayInternet()){
                showErrorDialog("Verifique su conexion a Internet")
            }else {
                showForgotPasswordDialog()
            }
        }

        loginButton.setOnClickListener {
            if (!siHayInternet()){
                showErrorDialog("Verifique su conexion a Internet")
            }else {
                val username = usernameEditText.text.toString().toUpperCase()
                val password = passwordEditText.text.toString()

                if (username.isNotEmpty() && password.isNotEmpty()) {
                    validarUsuario(username)
                } else {
                    Toast.makeText(
                        this@InicioSesion,
                        "Por favor, completa todos los campos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val editTextEmailOrPhone = dialogView.findViewById<EditText>(R.id.editTextEmail)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("¿Olvidaste tu contraseña?")
            .setMessage("Ingresa el correo asociado a la cuenta")
            .setView(dialogView)
            .setPositiveButton("Enviar") { dialog, _ ->
                val emailOrPhone = editTextEmailOrPhone.text.toString().trim()
                if (emailOrPhone.isNotEmpty()) {
                    // Enviar el correo electrónico a tu API para generar un código
                    enviarCorreoParaCodigo(emailOrPhone)
                } else {
                    showErrorDialog("Por favor, ingrese el correo asociado a la cuenta.")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .create()

        alertDialog.show()
    }

    private fun enviarCorreoParaCodigo(email: String) {
        // Enviar el correo electrónico a tu API para generar un código. Supongamos que la API devuelve un valor "success".
        val apiUrl = "${BaseApi.BaseURL}validarCorreo.php?email=$email"
        val requestQueue = Volley.newRequestQueue(this)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, apiUrl, null,
            { response ->
                val success = response.getBoolean("success")

                if (success) {
                    // El correo fue enviado con éxito, redirigir al usuario a la actividad de verificación de código.
                    val intent = Intent(this, VerificarCodigo::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                } else {
                    showErrorDialog("Error al enviar el correo.")
                }
            },
            { _ ->
                showErrorDialog("Hubo un error en la solicitud. Intente de nuevo")
            }
        )
        requestQueue.add(jsonObjectRequest)
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            val idUsuario = sharedPreferences.getString("idUsuario", "")
            if (!idUsuario.isNullOrEmpty()){
                //Validamos usuario habilitado o inhabilitado
                validarEstado(idUsuario)
            }
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            // Error en la autenticación biométrica
            Toast.makeText(this@InicioSesion, "Error en la autenticación biométrica: $errString", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validarUsuario(usuario: String) {
        val url = "${BaseApi.BaseURL}validacion.php"
        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                when (response) {
                    "Acceso permitido" -> {
                        // Almacena la ID del usuario en SharedPreferences
                        editor.putString("idUsuario", usuario).apply()

                        if (deviceSupportsBiometrics()) {
                            // Mostrar diálogo de autenticación biométrica
                            mostrarDialogoGuardarCuenta(usuario)
                        } else {
                            validarEstado(usuario)
                        }
                    }
                    "Contraseña incorrecta" -> {
                        Toast.makeText(this@InicioSesion, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                    }
                    "Usuario no encontrado o inhabilitado" -> {
                        Toast.makeText(this@InicioSesion, "Usuario no encontrado o inhabilitado", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            Response.ErrorListener { error ->
                Log.e("Error de red", "Error en la solicitud: $error")
                showErrorDialog("Hubo un error en la solicitud. Intente de nuevo")
            }) {
            @Throws(AuthFailureError::class)
            override fun getParams(): MutableMap<String, String> {
                val parametros: MutableMap<String, String> = HashMap()
                parametros["usuario"] = usernameEditText.text.toString()
                parametros["password"] = passwordEditText.text.toString()
                return parametros
            }
        }

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)
    }

    private fun validarEstado(idUsuario: String) {
        val apiUrl = "${BaseApi.BaseURL}validarEstadoUsuario.php"
        val stringRequest = object : StringRequest(
            Request.Method.POST, apiUrl,
            Response.Listener { response ->
                val isUserEnabled = response == "1"
                if (isUserEnabled) {
                    // El usuario está habilitado
                    obtenerDatosUsuario(idUsuario)
                    // Llama a la función de inicio de sesión
                    ingresarApp()
                    // Puedes realizar otras acciones aquí si es necesario
                } else {
                    // El usuario está inhabilitado
                    Toast.makeText(this@InicioSesion, "Usuario Inhabilitado", Toast.LENGTH_SHORT).show()
                    // Puedes manejar lo que deseas hacer en caso de usuario inhabilitado
                }
            },
            Response.ErrorListener { error ->
                Log.e("Error al Iniciar", "Error en la solicitud: $error")
                showErrorDialog("Hubo un error en la solicitud de Inicio. Intente de nuevo por favor")
            }) {
            @Throws(AuthFailureError::class)
            override fun getParams(): MutableMap<String, String> {
                val parametros: MutableMap<String, String> = HashMap()
                parametros["idUsuario"] = idUsuario
                return parametros
            }
        }

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)
    }

    private fun mostrarDialogoGuardarCuenta(idUsuario:String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Guardar cuenta")
            .setMessage("¿Deseas guardar tu cuenta para un inicio de sesión más rápido la próxima vez?")
            .setPositiveButton("Guardar") { _, _ ->
                guardarIdUsuario(idUsuario)
                biometricPrompt.authenticate(promptInfo)
            }
            .setNegativeButton("No guardar") { _, _ ->
                // El usuario seleccionó no guardar la cuenta
                // Llama a la función de validar estado
                validarEstado(idUsuario)
            }
            .setCancelable(false)
            .show()
    }

    private fun guardarIdUsuario(idUsuario: String) {
        // El usuario seleccionó guardar la cuenta

        editor.putString("guardarCuenta", "true")
            .putString("idUsuario", idUsuario)
            .apply()

        val guardarCuenta = sharedPreferences.getString("guardarCuenta", "")

        Log.d("guardarCuenta: ", guardarCuenta.toString())
    }

    private fun ingresarApp(){
        val intent = Intent(applicationContext, PantallaPrincipal::class.java)
        startActivity(intent)
        Toast.makeText(this@InicioSesion, "Inicio de Sesion Exitoso", Toast.LENGTH_SHORT).show()
        // Cerrar la actividad de inicio de sesión
        finish()
    }

    private fun obtenerDatosUsuario(idUsuario: String) {

        val url = "${BaseApi.BaseURL}mostrarDatos.php?id_usuario=$idUsuario"
        val request = JsonObjectRequest(
            Request.Method.POST, url, null,
            { response ->
                Log.d("Response Inicio", response.toString())
                // Asegúrate de que la respuesta JSON se procese con la codificación UTF-8
                val jsonString = response.toString()
                val utf8String = String(jsonString.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                val jsonObject = JSONObject(utf8String)
                // Luego, obtén los valores de la respuesta
                val nombre = jsonObject.optString("nombres", "")
                val apellidoP = jsonObject.optString("apellido_P", "")
                val apellidoM = jsonObject.optString("apellido_M", "")
                val sexo = jsonObject.optString("sexo", "")
                val fechaNacimiento = jsonObject.optString("fecha_Nacimiento", "")
                val correo = jsonObject.optString("correo", "")
                val especialidad = jsonObject.optString("especialidad", "")
                val area = jsonObject.optString("cargo", "")
                val rol = jsonObject.optString("rol", "")
                // Almacena los datos del usuario en SharedPreferences
                editor.putString("nombre", nombre)
                    .putString("apellidoP", apellidoP)
                    .putString("apellidoM", apellidoM)
                    .putString("sexo", sexo)
                    .putString("fechaNacimiento", fechaNacimiento)
                    .putString("correo", correo)
                    .putString("especialidad", especialidad)
                    .putString("area", area)
                    .putString("rol", rol)
                    .apply()
                Log.d("EDITOR", editor.toString())
                // Aplica los cambios
            },
            { _ ->
                showErrorDialog("Hubo un error al obtener los datos.")
            }
        )
        // Agregar la solicitud a la cola de solicitudes
        Volley.newRequestQueue(this).add(request)
    }

    private fun deviceSupportsBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate()
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun siHayInternet(): Boolean {
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}