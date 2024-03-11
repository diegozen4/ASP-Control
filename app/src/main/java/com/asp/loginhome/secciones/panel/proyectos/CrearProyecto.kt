package com.asp.loginhome.secciones.panel.proyectos

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import android.widget.TextView
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

class CrearProyecto : AppCompatActivity() {

    private lateinit var autoCompleteTextViewSupervisor: AutoCompleteTextView
    private lateinit var multiAutoCompleteTextViewUsuarios: MultiAutoCompleteTextView
    private lateinit var editTextNombreProyecto: EditText
    private lateinit var editTextDescripcionProyecto: EditText
    private lateinit var textViewFechaEntrega: TextView
    private lateinit var textViewCodigoProyecto: TextView
    private lateinit var buttonDesignar: Button
    private val cal = Calendar.getInstance()
    private val year = cal.get(Calendar.YEAR)
    private val month = cal.get(Calendar.MONTH)
    private val day = cal.get(Calendar.DAY_OF_MONTH)

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_proyecto)

        // Inicializa las vistas
        autoCompleteTextViewSupervisor = findViewById(R.id.AutoCompleteTextViewSupervisor)
        multiAutoCompleteTextViewUsuarios = findViewById(R.id.multiAutoCompleteTextViewUsuarios)
        editTextNombreProyecto = findViewById(R.id.editTextNombreProyecto)
        editTextDescripcionProyecto = findViewById(R.id.editTextDescripcion)
        textViewFechaEntrega = findViewById(R.id.textViewFechaEntrega)
        textViewCodigoProyecto = findViewById(R.id.textViewCodigoProyecto)
        buttonDesignar = findViewById(R.id.buttonGuardarProyecto)

        // Configura el separador para múltiples usuarios (en este caso, una coma)
        multiAutoCompleteTextViewUsuarios.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        // Carga la lista de usuarios al MultiAutoCompleteTextView
        cargarListaUsuarios()

        // Carga la lista de usuarios al AutoCompleteTextView
        cargarListaUsuariosSupervisores()

        // Configura el listener de clic para el botón "Designar"
        buttonDesignar.setOnClickListener {
            crearProyecto()
        }

        textViewFechaEntrega.setOnClickListener {showDatePickerDialog()}
    }

    private fun showDatePickerDialog() {
        val currentDate = Calendar.getInstance()

        // Calcula el año máximo permitido (2005, 18 años atrás desde el año actual)
        val minYear = currentDate.get(Calendar.YEAR)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Crea un objeto Calendar con la fecha seleccionada
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                Log.d("Fecha Seleccionada: ", selectedDate.toString())
                Log.d("Fecha Actual: ", currentDate.toString())
                Log.d("Año Seleccionado: ", selectedYear.toString())
                Log.d("Año Minimo: ", minYear.toString())

                // Compara con la fecha actual y el año máximo permitido
                if (selectedYear <= minYear) {
                    // Si la fecha seleccionada es igual o posterior a la fecha actual y el año
                    // es más de 18 años atrás desde el año actual, la acepta
                    val formattedDate = formatDate(selectedDay, selectedMonth, selectedYear)
                    textViewFechaEntrega.setText(formattedDate)
                }
            },
            year, month, day
        )

        // Establece la fecha máxima permitida como el año máximo calculado
        datePickerDialog.datePicker.minDate = currentDate.apply { set(Calendar.YEAR, minYear) }.timeInMillis

        datePickerDialog.show()
    }

    private fun formatDate(day: Int, month: Int, year: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun crearProyecto() {
        // Obtiene los datos del formulario
        val supervisor = autoCompleteTextViewSupervisor.text.toString().trim()
        val usuarios = multiAutoCompleteTextViewUsuarios.text.toString().trim()
        val nombreProyecto = editTextNombreProyecto.text.toString().trim()
        val descripcionProyecto = editTextDescripcionProyecto.text.toString().trim()
        val fechaEntrega = textViewFechaEntrega.text.toString().trim()
        val fechaCreacion = getCurrentDate()
        val horaCreacion = getCurrentTime()

        // Valida que los campos no estén vacíos
        if (usuarios.isEmpty() || nombreProyecto.isEmpty() || descripcionProyecto.isEmpty() || fechaEntrega.isEmpty()){
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Divide la cadena de usuarios en un arreglo
        val usuariosArray = usuarios.split(",").map { it.trim() }

        // URL de tu API para designar tareas
        val url = "https://www.aspcontrol.com.pe/APP/crearProyecto.php"

        // Parámetros a enviar en la solicitud POST
        val params = HashMap<String, String>()
        params["supervisor"] = supervisor
        params["usuarios"] = usuariosArray.joinToString(",") // Convierte a una cadena separada por comas
        params["nombreTarea"] = nombreProyecto
        params["descripcionTarea"] = descripcionProyecto
        params["fechaCreacion"] = fechaCreacion
        params["fechaEntrega"] = fechaEntrega
        params["horaCreacion"] = horaCreacion

        Log.d("Parametros: ", params.toString())

        // Configura la solicitud POST
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val stringRequest = object : StringRequest(Request.Method.POST, url,
            Response.Listener<String> { response ->
                // Manejar la respuesta exitosa
                Toast.makeText(this, "Proyecto creado exitosamente", Toast.LENGTH_SHORT).show()
                // Puedes agregar código para hacer otra acción después de designar la tarea
            },
            Response.ErrorListener { error ->
                // Manejar errores de la solicitud
                Toast.makeText(this, "Error al crear el Proyecto", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }) {
            override fun getParams(): Map<String, String> {
                return params
            }
        }

        // Agrega la solicitud a la cola
        requestQueue.add(stringRequest)
    }

    private fun cargarListaUsuariosSupervisores() {
        // URL de tu API para obtener la lista de usuarios
        val urlUsuarios = "${BaseApi.BaseURL}obtenerUsuariosSupervisor.php"

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, urlUsuarios, null,
            { response ->
                // Procesa la respuesta JSON y obtén la lista de usuarios
                val listaUsuarios = obtenerListaUsuarios(response)
                Log.d("CargarListaUsuarios", "Lista de usuarios: $listaUsuarios")

                // Configura un adaptador para el AutoCompleteTextView del supervisor
                val adapterSupervisor = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaUsuarios)

                // Asocia los adaptadores a los respectivos AutoCompleteTextView
                autoCompleteTextViewSupervisor.setAdapter(adapterSupervisor)
            },
            { error ->
                Log.e("CargarListaUsuarios", "Error al cargar la lista de usuarios: ${error.message}")

                // Manejar errores de la solicitud
                Toast.makeText(this, "Error al cargar la lista de usuarios", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )

        // Agrega la solicitud a la cola
        requestQueue.add(jsonArrayRequest)
    } private fun cargarListaUsuarios() {
        // URL de tu API para obtener la lista de usuarios
        val urlUsuarios = "${BaseApi.BaseURL}obtenerUsuarios.php"

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, urlUsuarios, null,
            { response ->
                // Procesa la respuesta JSON y obtén la lista de usuarios
                val listaUsuarios = obtenerListaUsuarios(response)
                Log.d("CargarListaUsuarios", "Lista de usuarios: $listaUsuarios")

                // Configura un adaptador para el MultiAutoCompleteTextView de usuarios
                val adapterUsuarios = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaUsuarios)

                // Asocia los adaptadores a los respectivos AutoCompleteTextView
                multiAutoCompleteTextViewUsuarios.setAdapter(adapterUsuarios)
            },
            { error ->
                Log.e("CargarListaUsuarios", "Error al cargar la lista de usuarios: ${error.message}")

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
                val nombreUsuario = "$nombres $apellidoP ( $idUsuario )"

                listaUsuarios.add(nombreUsuario)
            } catch (e: JSONException) {
                e.printStackTrace()
                Log.e("ObtenerListaUsuarios", "Error al procesar respuesta JSON: ${e.message}")

            }
        }
        Log.d("ObtenerListaUsuarios", "Lista de usuarios obtenida: $listaUsuarios")

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
}
