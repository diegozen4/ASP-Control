package com.asp.loginhome.secciones.panel.mensajeria

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.asp.loginhome.R
import com.asp.loginhome.recursos.BaseApi
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MensajeNuevo : AppCompatActivity() {

    private lateinit var multiAutoCompleteTextViewTo: MultiAutoCompleteTextView
    private lateinit var editTextSubject: EditText
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mensaje_nuevo)

        sharedPreferences = this.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

        // Inicializa las vistas
        multiAutoCompleteTextViewTo = findViewById(R.id.multiAutoCompleteTextViewTo)
        editTextSubject = findViewById(R.id.editTextSubject)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)


        // Configura el separador para múltiples destinatarios (en este caso, una coma)
        multiAutoCompleteTextViewTo.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())


        // Carga la lista de usuarios al MultiAutoCompleteTextView
        cargarListaUsuarios()

        // Configura el listener de clic para el botón "Enviar"
        buttonSend.setOnClickListener {
            enviarMensaje()
        }
    }

    private fun enviarMensaje() {
        // Obtiene la ID del remitente de las preferencias compartidas
        val idRemitente: String? = sharedPreferences.getString("idUsuario", null)

        // Obtiene los datos del formulario
        val destinatarios = multiAutoCompleteTextViewTo.text.toString().trim()
        val asunto = editTextSubject.text.toString().trim()
        val mensaje = editTextMessage.text.toString().trim()
        val hora = getCurrentTime()
        val fecha = getCurrentDate()

        // Valida que los campos no estén vacíos
        if (idRemitente.isNullOrEmpty() || destinatarios.isEmpty() || asunto.isEmpty() || mensaje.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Divide los destinatarios por comas y elimina espacios en blanco
        val destinatariosArray = destinatarios.split(",").map { it.trim() }

        // Filtra solo los IDs de los destinatarios
        val idDestinatariosArray = destinatariosArray.mapNotNull { extractIdFromText(it) }

        // Crea una solicitud POST con parámetros
        val url = "${BaseApi.BaseURL}enviarMensaje.php"
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        val parametros = HashMap<String, String>()
        parametros["idRemitente"] = idRemitente
        parametros["asunto"] = asunto
        parametros["mensaje"] = mensaje
        parametros["horaEnvio"] = hora
        parametros["fechaEnvio"] = fecha
        parametros["destinatarios"] = idDestinatariosArray.joinToString(",") // Convierte la lista en una cadena separada por comas

        // Agregar registros de depuración para ver los parámetros que se enviarán
        Log.d("EnviarMensaje", "Parámetros enviados: $parametros")

        // Configura la solicitud POST
        val stringRequest = object : StringRequest(Request.Method.POST, url,
            Response.Listener { response ->
                // Manejar la respuesta exitosa
                Toast.makeText(this, "Mensaje enviado exitosamente", Toast.LENGTH_SHORT).show()
                Log.d("EnviarMensaje", "Respuesta exitosa: $response")
                // Puedes agregar código para regresar a la lista de mensajes o hacer otra acción
                finish() // Cierra el Activity actual

            },
            Response.ErrorListener { error ->
                // Manejar errores de la solicitud
                Toast.makeText(this, "Error al enviar el mensaje", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
                Log.e("EnviarMensaje", "Error: ${error.message}")
            }) {
            // Adjunta los parámetros a la solicitud POST
            override fun getParams(): Map<String, String> {
                return parametros
            }
        }

        // Agregar registros de depuración para ver la URL y los parámetros de la solicitud
        Log.d("EnviarMensaje", "URL de la solicitud: $url")
        Log.d("EnviarMensaje", "Parámetros de la solicitud: ${stringRequest.body}")

        // Agrega la solicitud a la cola
        requestQueue.add(stringRequest)
    }

    // Función para extraer el ID del texto que contiene el ID entre paréntesis
    private fun extractIdFromText(text: String): String? {
        val idPattern = "\\((.*?)\\)".toRegex()
        val matchResult = idPattern.find(text)
        return matchResult?.groupValues?.getOrNull(1)
    }


    private fun cargarListaUsuarios() {
        // URL de tu API para obtener la lista de usuarios
        val urlUsuarios = "${BaseApi.BaseURL}obtenerUsuarios.php"

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, urlUsuarios, null,
            { response ->
                // Procesa la respuesta JSON y obtén la lista de usuarios
                val listaUsuarios = obtenerListaUsuarios(response)

                // Configura un adaptador para el AutoCompleteTextView
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaUsuarios)

                // Asocia el adaptador al AutoCompleteTextView
                multiAutoCompleteTextViewTo.setAdapter(adapter)
            },
            { error ->
                // Manejar errores de la solicitud
                Toast.makeText(this, "Error al cargar la lista de usuarios", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )

        // Agrega la solicitud a la cola
        requestQueue.add(jsonArrayRequest)
    }

    private fun obtenerListaUsuarios(response: JSONArray): ArrayList<String> {
        val listaUsuarios = ArrayList<String>()
        for (i in 0 until response.length()) {
            try {
                val usuarioJSON = response.getJSONObject(i)
                val nombres = usuarioJSON.getString("nombres")
                val apellidoP = usuarioJSON.getString("apellido_P")
                val idUsuario = usuarioJSON.getString("id_Usuario")

                // Combina nombres y apellido_P en un solo nombre de usuario
                val nombreUsuario = "$nombres $apellidoP ($idUsuario)"

                listaUsuarios.add(nombreUsuario)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return listaUsuarios
    }

    private fun getCurrentDate(): String {
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es"))
        return dateFormat.format(currentDate)
    }

    private fun getCurrentTime(): String {
        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentTime)
    }

    companion object {
        private const val PREFS_FILE_NAME = "MyPrefs"
    }
}
