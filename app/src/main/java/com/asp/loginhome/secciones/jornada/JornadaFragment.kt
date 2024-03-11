@file:Suppress("DEPRECATION")

package com.asp.loginhome.secciones.jornada

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.asp.loginhome.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import kotlinx.coroutines.*
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@Suppress("DEPRECATION")
class JornadaFragment: Fragment(R.layout.fragment_jornada) {

    private var url: String = ""
    private var reporte: String = ""
    private var tipoNota: String = ""
    private var accionUbicacion: String = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // variables de textview para Nombre y Area
    private lateinit var tvNombre: TextView
    private lateinit var tvArea: TextView
    private lateinit var tvFecha: TextView

    // variables de textview para la jornada: hora de inicio y fin, y para generar una nueva marcacion de horas
    private lateinit var tvHoraInicio: TextView
    private lateinit var tvHoraFin: TextView
    private lateinit var tvHoraInicioRefrigerio: TextView
    private lateinit var tvHoraFinRefrigerio: TextView
    private lateinit var tvNuevaMarcacion: TextView

    // variables de tipo boton, para marcar el inicio/pausa/continuar y para finalizar
    private lateinit var btnInicioPausaContinuar: Button
    private lateinit var btnFin: Button
    private lateinit var btnInicioRefrigerio: Button
    private lateinit var btnFinRefrigerio: Button

    //variables String, Para almacenar la instacia de hora en texto con formato "hh:mm"
    private var horaInicioRefrigerio: String = ""
    private var horaFinRefrigerio: String = ""
    private var horaPausa: String= ""
    private var horaContinuar: String= ""
    private var horaInicio: String= ""
    private var horaFin: String= ""

    //variables tipo boolean, para determinar que inicien en false, para luego activarlas con la negacion "!"
    private var isStarted: Boolean = false
    private var isFinished: Boolean = true
    private var refrigerioInicio: Boolean = true
    private var refrigerioFin: Boolean = true
    private var isPaused: Boolean = true
    private var isContinue: Boolean = true
    private var isReseted: Boolean = true


    private lateinit var sharedPreferences: SharedPreferences

    //private val fechaLocal="fecha"
    private val nombreLocal="nombre"
    private val apellidoPLocal="apellidoP"
    private val apellidoMLocal="apellidoM"
    private val areaLocal="area"
    private val horaInicioLocal="horaInicio"
    private val horaPausaLocal="horaPausa"
    private val horaContinuarLocal="horaContinuar"
    private val horaIniRefriLocal="HoraIniRefri"
    private val horaFinRefriLocal="horaFinRefri"
    private val horaFinLocal="horaFin"

    private val ubiInicioLocal="ubiInicioLocal"
    private val ubiPausaLocal="ubiPausaLocal"
    private val ubiContinuarLocal="ubiContinuarLocal"
    private val ubiIniRefriLocal="ubiIniRefriLocal"
    private val ubiFinRefriLocal="ubiFinRefriLocal"
    private val ubiFinLocal="ubiFinLocal"

    private val horaInicioEnv="horaInicioEnv"
    private val horaPausaEnv="horaPausaEnv"
    private val horaContinuarEnv="horaContinuarEnv"
    private val horaIniRefriEnv="HoraIniRefriEnv"
    private val horaFinRefriEnv="horaFinRefriEnv"
    private val horaFinEnv="horaFinEnv"

    // Definir una variable para el código de solicitud de activación de ubicación
    private val REQUEST_CHECK_SETTINGS = 123
    //private val proyectosList: MutableList<String> = mutableListOf()
    //private lateinit var spinnerProyect: Spinner

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_jornada, container, false)

        sharedPreferences = requireContext().getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

        //Conexion de variable al layout por id, para enviar la fecha actual
        tvFecha = view.findViewById(R.id.fecha_textview)
        //la variable toma como valor el resultado de la funcion
        val currentDate = getCurrentDate()
        //dicho resultado ahora es presentado en la variable de tipo text.view
        tvFecha.text = currentDate
        //editor.putString(fechaLocal, currentDate)

        //Conexion de variables al layout por id, para enviar nombre y area del trabajador
        tvNombre = view.findViewById(R.id.textViewNombre)
        tvArea = view.findViewById(R.id.textViewArea)

        //Conexion de variables al layout por id, para enviar los datos de la jornada
        btnInicioPausaContinuar = view.findViewById(R.id.btnInicioPausaContinuar)
        btnFin = view.findViewById(R.id.btnFin)
        btnInicioRefrigerio = view.findViewById(R.id.btnInicioRefrigerio)
        btnFinRefrigerio = view.findViewById(R.id.btnFinRefrigerio)
        tvHoraInicio = view.findViewById(R.id.tvHoraInicio)
        tvHoraFin = view.findViewById(R.id.tvHoraFin)
        tvHoraInicioRefrigerio = view.findViewById(R.id.tvHoraInicioRefrigerio)
        tvHoraFinRefrigerio = view.findViewById(R.id.tvHoraFinRefrigerio)
        tvNuevaMarcacion = view.findViewById(R.id.btnNuevaMarcacion)

        val horaInicioAlmc = sharedPreferences.getString(horaInicioLocal, "00:00:00")
        val horaPausaAlmc = sharedPreferences.getString(horaPausaLocal, "00:00:00")
        val horaContinuarAlmc = sharedPreferences.getString(horaContinuarLocal, "00:00:00")
        val horaIniRefriAlmc = sharedPreferences.getString(horaIniRefriLocal, "00:00:00")
        val horaFinRefriAlmc = sharedPreferences.getString(horaFinRefriLocal, "00:00:00")
        val horaFinAlmc = sharedPreferences.getString(horaFinLocal, "00:00:00")

        val horaInicioEnviado = sharedPreferences.getString(horaInicioEnv, "00:00:00")
        val horaPausaEnviado = sharedPreferences.getString(horaPausaEnv, "00:00:00")
        val horaContinuarEnviado = sharedPreferences.getString(horaContinuarEnv, "00:00:00")
        val horaIniRefriEnviado = sharedPreferences.getString(horaIniRefriEnv, "00:00:00")
        val horaFinRefriEnviado = sharedPreferences.getString(horaFinRefriEnv, "00:00:00")
        val horaFinEnviado = sharedPreferences.getString(horaFinEnv, "00:00:00")

        val nombreAlmc = sharedPreferences.getString(nombreLocal, "")
        val apellidoPAlmc = sharedPreferences.getString(apellidoPLocal, "")
        val apellidoMAlmc = sharedPreferences.getString(apellidoMLocal, "")
        val areaAlmc = sharedPreferences.getString(areaLocal, "")

        tvNombre.text = "$nombreAlmc $apellidoPAlmc $apellidoMAlmc"
        tvArea.text = areaAlmc

        val idUsuario = sharedPreferences.getString("idUsuario", "")
        val editor = sharedPreferences.edit()

        if (idUsuario != null) {
            if (idUsuario.isNotEmpty()){

                if(siHayInternet()) {
                  /*  if (nombreAlmc.isNullOrEmpty()){
                        obtenerDatosUsuario("https://aspcontrol1.000webhostapp.com/db_asp/mostrarDatos.php?id_usuario=$idUsuario")
                    }*/
                    tvNombre.text = "$nombreAlmc $apellidoPAlmc $apellidoMAlmc"
                    tvArea.text = areaAlmc
                }else{
                    tvNombre.text = "$nombreAlmc $apellidoPAlmc $apellidoMAlmc"
                    tvArea.text = areaAlmc
                }


                if (horaInicioAlmc == "00:00:00" && horaInicioEnviado == "00:00:00") {
                    obtenerHoraSinFin("https://www.aspcontrol.com.pe/APP/marcacionPendiente.php", idUsuario)
                }else{
                    //si es que si hay hora inicio
                    //si no hay horaInicioAlmc
                    if (horaInicioAlmc == "00:00:00"){
                        //mostrar horaInicioEnviado
                        tvHoraInicio.text = horaInicioEnviado

                        //entonces el boton cambia a pausa
                        btnInicioPausaContinuar.text = "Pausa"
                    } else{
                        //si no, mostrar horaInicioAlmc
                        tvHoraInicio.text = horaInicioAlmc

                        //entonces el boton cambia a pausa
                        btnInicioPausaContinuar.text = "Pausa"
                    }
                    //pero si hay hora inicio sin hora fin
                    if(horaFinAlmc == "00:00:00" && horaFinEnviado == "00:00:00") {
                        //se inhabilita el boton iniciar
                        isStarted = true
                        //y se habilita los botones de inicio refrigerio, Pausa, Final y Nueva Marcacion
                        refrigerioInicio = false
                        isFinished = false
                        isReseted = false
                        isPaused = false
                    }else{
                        //pero si si hay horafin
                        //El boton Inicio, regresa a "Iniciar"
                        btnInicioPausaContinuar = view.findViewById(R.id.btnInicioPausaContinuar)
                        //Inhabilita los botones Inicio, pausa, continuar y Finalizar
                        isStarted = true
                        isFinished = true
                        isPaused = true
                        isContinue = true
                        //habilita el boton nueva marcacion
                        isReseted = false
                        if (horaFinAlmc == "00:00:00"){
                            tvHoraFin.text = horaFinEnviado
                        }else{
                            tvHoraFin.text = horaFinAlmc
                        }
                    }

                    if (horaIniRefriAlmc =="00:00:00" && horaIniRefriEnviado == "00:00:00"){
                    } else {
                        //boton inicio refrigerio se deshabilita
                        refrigerioInicio = true
                        //boton fin refrigerio se habilita
                        refrigerioFin = false
                        if (horaIniRefriAlmc == "00:00:00") {
                            tvHoraInicioRefrigerio.text = horaIniRefriEnviado
                        } else {
                            tvHoraInicioRefrigerio.text = horaIniRefriAlmc
                        }
                    }

                    if (horaFinRefriAlmc == "00:00:00" && horaFinRefriEnviado == "00:00:00") {
                    }else{
                        //boton inicio refrigerio se deshabilita
                        refrigerioInicio = true
                        //boton fin refrigerio se deshabilita
                        refrigerioFin = true
                        if (horaFinRefriAlmc == "00:00:00") {
                            tvHoraFinRefrigerio.text = horaFinRefriEnviado
                        } else {
                            tvHoraFinRefrigerio.text = horaFinRefriAlmc
                        }
                    }

                    if(horaPausaAlmc == "00:00:00" && horaPausaEnviado == "00:00:00") {
                    }else{
                        btnInicioPausaContinuar.text = "Continuar"
                        isPaused = true
                        isContinue = false
                    }

                    if(horaContinuarAlmc == "00:00:00" && horaContinuarEnviado == "00:00:00") {
                    }else{
                        btnInicioPausaContinuar.text = "Pausa"
                        isContinue = true
                        isPaused = false
                    }
                }
            } else {
                // El ID de usuario no está disponible en SharedPreferences
                // Realiza alguna acción o muestra un mensaje de error
            }
        }

        //Accion que tomará el boton inicio al click
        btnInicioPausaContinuar.setOnClickListener {
            //si El boton "Iniciar" está activo:
            if (!isStarted) {
                if (!ubicacionActivada()) {
                    mostrarDialogoActivacionUbicacion()
                } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Permiso no aceptado por el momento
                    requestLocationPermission()

                    }else {
                        //Marcacion de Hora Inicio
                        val currentTime = getCurrentTime()
                        horaInicio = currentTime
                        tvHoraInicio.text = horaInicio
                        //almacenar hora inicio en el alamacenamiento local
                        editor.putString(horaInicioLocal, horaInicio).apply()
                        //el boton cambia a "Pausa"
                        btnInicioPausaContinuar.text = "Pausa"
                        //se desactiva el boton inicio
                        isStarted = true
                        //y se activa los siguientes botones
                        refrigerioInicio = false
                        isFinished = false
                        isReseted = false
                        isPaused = false

                        if (idUsuario != null) {
                            if (siHayInternet()) {
                                //Insertar horaInicio
                                insertarHoraInicio(
                                    "https://www.aspcontrol.com.pe/APP/insertarHoraInicio.php",
                                    currentTime,
                                    currentDate,
                                    idUsuario
                                )
                            }
                                //verificar permisos de ubicacion para enviar ubicacion
                                verificarPermisosUbicacion(
                                    "https://www.aspcontrol.com.pe/APP/insertarUbicacionInicio1.php",
                                    "iniciar"
                                )

                        }
                }
            }
            //sino, si está en "Pausa":
            else if (!isPaused) {
                if (!ubicacionActivada()) {
                    mostrarDialogoActivacionUbicacion()
                } else {
                    //preguntar si deseas realizar una pausa
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Pausa en la Labor")
                    builder.setMessage("¿Estás seguro que deseas realizar una Pausa en la labor?")
                    // Configurar los botones del cuadro de diálogo
                    builder.setPositiveButton("Sí") { _, _ ->
                        //Marcacion de Hora Pausa
                        val currentTime = getCurrentTime()
                        horaPausa = currentTime
                        //almacenar hora pausa en almacenamiento local
                        editor.putString(horaPausaLocal, horaPausa).apply()
                        //El boton cambia a "Continua"
                        btnInicioPausaContinuar.text = "Continuar"
                        //se desactiva el boton pausa
                        isPaused = true
                        //se activa el boton continuar
                        isContinue = false
                        //Insertar hora Pausa
                        if (idUsuario != null) {
                            if (siHayInternet()) {
                                //insertar hora pausa
                                insertarHora(
                                    "https://www.aspcontrol.com.pe/APP/insertarHoraPausa.php",
                                    currentTime,
                                    currentDate,
                                    idUsuario,
                                    "pausa"
                                )
                                //enviar el nota de reporte de pausa
                                mostrarVentanaNotas(
                                    "https://www.aspcontrol.com.pe/APP/insertarReportePausa.php",
                                    "Pausa"
                                )
                            }
                            //verificar permisos de ubicacion para enviar la ubicacion
                            verificarPermisosUbicacion(
                                "https://www.aspcontrol.com.pe/APP/insertarUbicacionPausa.php",
                                "pausa"
                            )
                        }
                    }
                    builder.setNegativeButton("No") { _, _ ->
                        // Si el usuario selecciona "No", no se realiza ninguna acción y el cuadro de diálogo se cierra
                    }

                    // Mostrar el cuadro de diálogo
                    val dialog = builder.create()
                    dialog.show()
                }
            }

            //sino, estará en "Continuar"
            else if (!isContinue) {
                if (!ubicacionActivada()) {
                    mostrarDialogoActivacionUbicacion()
                } else {
                    //Marcacion Hora Continuar
                    val currentTime = getCurrentTime()
                    horaContinuar = currentTime
                    //alamacenar hora continuar en almacenamiento local
                    editor.putString(horaContinuarLocal, horaContinuar).apply()
                    //El boton cambia a "Pausa"
                    btnInicioPausaContinuar.text = "Pausa"
                    //se desactiva el boton continuar
                    isContinue = true
                    //se activa el boton pausa
                    isPaused = false
                    //Insertar horaContinuar
                    if (idUsuario != null) {
                        if (siHayInternet()) {
                            insertarHora(
                                "https://www.aspcontrol.com.pe/APP/insertarHoraContinuar.php",
                                currentTime,
                                currentDate,
                                idUsuario,
                                "continuar"
                            )
                        }
                        verificarPermisosUbicacion(
                            "https://www.aspcontrol.com.pe/APP/insertarUbicacionContinuar.php",
                            "continuar"
                        )
                    }
                }
            }
        }

        //Accion que tomará el boton Inicio Refrigerio al click
        btnInicioRefrigerio.setOnClickListener {
            //si esta activo
            if (!refrigerioInicio) {
                if (!ubicacionActivada()) {
                    mostrarDialogoActivacionUbicacion()
                } else {
                    //marcar la hora
                    val currentTime = getCurrentTime()
                    horaInicioRefrigerio = currentTime
                    //se almacena la hora en almacenamiento local
                    editor.putString(horaIniRefriLocal, horaInicioRefrigerio).apply()
                    //mostrar la hora
                    tvHoraInicioRefrigerio.text = horaInicioRefrigerio
                    //boton inicio refrigerio se deshabilita
                    refrigerioInicio = true
                    //boton fin refrigerio se habilita
                    refrigerioFin = false
                    if (idUsuario != null) {
                        if (siHayInternet()) {
                            //Insertar hora Inicio Refrigerio
                            insertarHora(
                                "https://www.aspcontrol.com.pe/APP/insertarHoraInicioRefrigerio.php",
                                currentTime,
                                currentDate,
                                idUsuario,
                                "iniRefri"
                            )
                        }
                        //verificar permisos de ubicacion para insertar la ubicacion
                        verificarPermisosUbicacion(
                            "https://www.aspcontrol.com.pe/APP/insertarUbicacionIniRefri.php",
                            "iniRefri"
                        )
                    }
                }
            }
        }

        //Accion que tomará el boton fin Refrigerio al click
        btnFinRefrigerio.setOnClickListener{
            //si esta habilitado
            if(!refrigerioFin) {
                if (!ubicacionActivada()) {
                    mostrarDialogoActivacionUbicacion()
                } else {
                    //marcar la hora
                    val currentTime = getCurrentTime()
                    horaFinRefrigerio = currentTime
                    //almacenar la hora en almacenamiento local
                    editor.putString(horaFinRefriLocal, horaFinRefrigerio).apply()
                    //mostrar la hora
                    tvHoraFinRefrigerio.text = horaFinRefrigerio
                    //deshabilitar el boton
                    refrigerioFin = true
                    if (idUsuario != null) {
                        if (siHayInternet()) {
                            //insertar hora fin refrigerio
                            insertarHora(
                                "https://www.aspcontrol.com.pe/APP/insertarHoraFinRefrigerio.php",
                                currentTime,
                                currentDate,
                                idUsuario,
                                "finRefrigerio"
                            )
                        }
                        //verificar permisos de ubicacion para insertar ubicacion
                        verificarPermisosUbicacion(
                            "https://www.aspcontrol.com.pe/APP/insertarUbicacionFinRefri.php",
                            "finRefri"
                        )
                    }
                }
            }
        }

        //Accion que tomará el boton finalizar al click
        btnFin.setOnClickListener {
            //si Finalizar esta Habilitado
            if(!isFinished) {
                if (!ubicacionActivada()) {
                    mostrarDialogoActivacionUbicacion()
                } else {
                    //consultar si desea finalizar la jornada
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Fin de Labor")
                    builder.setMessage("¿Estás seguro que deseas Finalizar la labor?")
                    // Configurar los botones del cuadro de diálogo
                    //si marca que si:
                    builder.setPositiveButton("Sí") { _, _ ->
                        //Marcacion de la hora de fin
                        val currentTime = getCurrentTime()
                        horaFin = currentTime
                        //mostrar la hora
                        tvHoraFin.text = horaFin
                        //alamacenar la hora fin en almacenamiento local
                        editor.putString(horaFinLocal, horaFin).apply()
                        //El boton Inicio, regresa a "Iniciar"
                        btnInicioPausaContinuar = view.findViewById(R.id.btnInicioPausaContinuar)
                        //Inhabilita los botones Inicio, pausa, continuar y Finalizar
                        isStarted = true
                        isFinished = true
                        isPaused = true
                        isContinue = true
                        //habilita el boton nueva marcacion
                        isReseted = false

                        //si no se marco el fin refrigerio:
                        if (refrigerioInicio) {
                            //marcar la hora fin refrigerio
                            horaFinRefrigerio = currentTime
                            //mostrar la hora
                            tvHoraFinRefrigerio.text = horaFinRefrigerio
                            //almacenar de forma local
                            editor.putString(horaFinRefriLocal, horaFinRefrigerio).apply()
                            if (idUsuario != null) {
                                if (siHayInternet()) {
                                    //insertar hora fin de refrigerio
                                    insertarHora(
                                        "https://www.aspcontrol.com.pe/APP/insertarHoraFinRefrigerio.php",
                                        currentTime,
                                        currentDate,
                                        idUsuario,
                                        "finRefrigerio"
                                    )
                                }
                                //verificar permisos para insertar la ubicacion del fin de refrigerio
                                verificarPermisosUbicacion(
                                    "https://www.aspcontrol.com.pe/APP/insertarUbicacionFinRefri.php",
                                    "finRefri"
                                )
                            }
                            //inhabilita los botones de refrigerio
                            refrigerioFin = true
                            refrigerioInicio = true
                        } else {
                            //inhabilita los botones de refrigerio de igual forma
                            refrigerioInicio = true
                            refrigerioFin = true
                        }

                        if (idUsuario != null) {
                            if (siHayInternet()) {
                                //Inserta horaFinLaboral
                                insertarHora(
                                    "https://www.aspcontrol.com.pe/APP/insertarHoraFin.php",
                                    currentTime,
                                    currentDate,
                                    idUsuario,
                                    "fin"
                                )
                                //insertar la nota de reporte de el fin de labor
                                mostrarVentanaNotas(
                                    "https://www.aspcontrol.com.pe/APP/insertarReporteFin.php",
                                    "Fin de Labor"
                                )
                            }
                            //verificar permisos de ubicacion para insertar ubicacion
                            verificarPermisosUbicacion(
                                "https://www.aspcontrol.com.pe/APP/insertarUbicacionFin.php",
                                "fin"
                            )
                        }
                    }
                    builder.setNegativeButton("No") { _, _ ->
                        // Si el usuario selecciona "No", no se realiza ninguna acción y el cuadro de diálogo se cierra
                    }
                    // Mostrar el cuadro de diálogo
                    val dialog = builder.create()
                    dialog.show()
                }
            }
        }


        //Accion que tomará el texto Nueva Marcacion al click
        tvNuevaMarcacion.setOnClickListener {
            //Si esta habilitada la nueva marcacion
            if (!isReseted) {

                    //preguntar si desea realizar una nueva marcacion
                    // Construir el cuadro de diálogo de confirmación
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Nueva Marcación")
                    builder.setMessage(
                        if (siHayInternet()) {
                            "¿Estás seguro que deseas realizar una nueva marcación?"
                        } else {
                            "No tienes conexión a Internet\nSi tienes horas marcadas, espera a tener conexión para que se envíen " +
                                    "y puedas realizar una nueva marcación.\n¿Estás seguro que deseas realizar una nueva marcación?"
                        }
                    )
                    // Configurar los botones del cuadro de diálogo
                    //si marca que si:
                    builder.setPositiveButton("Sí") { _, _ ->
                        // Si el usuario selecciona "Sí", se ejecutan las acciones para una nueva marcación
                        // Restablecer vistas y reiniciar estado de marcación
                        tvHoraInicio.text = "00:00:00"
                        tvHoraFin.text = "00:00:00"
                        tvHoraInicioRefrigerio.text = "00:00:00"
                        tvHoraFinRefrigerio.text = "00:00:00"
                        // Los botones regresan a su valor inicial
                        btnInicioPausaContinuar.text = "Iniciar Labor"
                        isStarted = false
                        isFinished = true
                        isReseted = true
                        isPaused = true
                        isContinue = true
                        refrigerioInicio = true
                        refrigerioFin = true
                        sharedPreferences.edit().remove(horaInicioLocal).apply()
                        sharedPreferences.edit().remove(horaPausaLocal).apply()
                        sharedPreferences.edit().remove(horaContinuarLocal).apply()
                        sharedPreferences.edit().remove(horaIniRefriLocal).apply()
                        sharedPreferences.edit().remove(horaFinRefriLocal).apply()
                        sharedPreferences.edit().remove(horaFinLocal).apply()
                        sharedPreferences.edit().remove(horaInicioEnv).apply()
                        sharedPreferences.edit().remove(horaPausaEnv).apply()
                        sharedPreferences.edit().remove(horaContinuarEnv).apply()
                        sharedPreferences.edit().remove(horaIniRefriEnv).apply()
                        sharedPreferences.edit().remove(horaFinRefriEnv).apply()
                        sharedPreferences.edit().remove(horaFinEnv).apply()
                        sharedPreferences.edit().remove(ubiFinLocal).apply()
                        sharedPreferences.edit().remove(ubiFinRefriLocal).apply()
                        sharedPreferences.edit().remove(ubiPausaLocal).apply()
                        sharedPreferences.edit().remove(ubiContinuarLocal).apply()
                        sharedPreferences.edit().remove(ubiIniRefriLocal).apply()
                        sharedPreferences.edit().remove(ubiInicioLocal).apply()
                    }
                    builder.setNegativeButton("No") { _, _ ->
                        // Si el usuario selecciona "No", no se realiza ninguna acción y el cuadro de diálogo se cierra
                    }
                    // Mostrar el cuadro de diálogo
                    val dialog = builder.create()
                    dialog.show()
            }
        }

        return view
    }

    private fun siHayInternet(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun ubicacionActivada(): Boolean {
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun mostrarDialogoActivacionUbicacion() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Para usar esta función, debes activar la ubicación del dispositivo.")
            .setTitle("Activar Ubicación")
            .setCancelable(false)
            .setPositiveButton("Activar") { _, _ ->
                solicitarActivacionUbicacion()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun solicitarActivacionUbicacion() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(requireActivity())
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // El usuario activó la ubicación
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    sendEx.printStackTrace()
                }
            }
        }
    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                // El usuario activó la ubicación
                // Puedes continuar con el código aquí
            } else {
                // El usuario no activó la ubicación, puedes mostrar un mensaje o realizar otra acción
            }
        }
    }

    private fun obtenerHoraSinFin(url: String, idUsuario: String) {
        // Crear la URL con los parámetros en formato GET
        // Obtener la fecha actual en el formato "25 de julio de 2023"
        val fechaJornada = getCurrentDate()

        // Codificar la fecha para que se pueda enviar en la URL
        val fechaCodificada = URLEncoder.encode(fechaJornada, "UTF-8")

        val urlWithParams = "$url?fecha_Jornada=$fechaCodificada&id_Usuario=$idUsuario"

        Log.d("horasinfin", "URL: $urlWithParams")

        val request = object : JsonObjectRequest(
            Method.GET, urlWithParams, null,
            Response.Listener { response ->
                Log.d("ResponseHora", response.toString())
                // Verificar si la respuesta contiene "<br>" (indicando un posible error en el servidor)
                val responseData = response.toString()
                if (responseData.startsWith("<br>")) {
                    Log.e("VolleyErrorObtenerHora", "Respuesta del servidor inválida: $responseData")
                    return@Listener
                }
                // Procesar la respuesta JSON recibida
                if (response.has("hora_Inicio") && !response.isNull("hora_Inicio")) {
                    // Obtener el valor de "hora_Inicio" si está presente
                    val horaInicio = response.getString("hora_Inicio")
                    tvHoraInicio.text = horaInicio
                    sharedPreferences.edit().putString(horaInicioEnv, horaInicio).apply()
                    /* val editor = sharedPreferences.edit()
                     editor.putString(horaInicioLocal, horaInicio)
                     editor.apply()*/
                    // Resto del código relacionado con "hora_Inicio"
                    btnInicioPausaContinuar.text = "Pausa"
                    isFinished = false
                    isStarted = true
                    refrigerioInicio = false
                    isReseted = false
                    isPaused = false
                } else {
                    // Si no está presente, muestra un mensaje o realiza alguna acción alternativa
                    tvHoraInicio.text = "00:00:00"
                }

                // Verificar si las otras horas están presentes
                if (response.has("hora_IniRefri") && !response.isNull("hora_IniRefri")) {
                    val horaIniRefri = response.getString("hora_IniRefri")
                    tvHoraInicioRefrigerio.text = horaIniRefri
                    sharedPreferences.edit().putString(horaIniRefriEnv, horaIniRefri).apply()
                    // Resto del código relacionado con "hora_IniRefri"
                    refrigerioInicio = true
                    refrigerioFin = false
                } else {
                    tvHoraInicioRefrigerio.text = "00:00:00"
                }

                if (response.has("hora_FinRefri") && !response.isNull("hora_FinRefri")) {
                    val horaFinRefri = response.getString("hora_FinRefri")
                    tvHoraFinRefrigerio.text = horaFinRefri
                    sharedPreferences.edit().putString(horaFinRefriEnv, horaFinRefri).apply()
                    // Resto del código relacionado con "hora_FinRefri"
                    refrigerioFin = true
                } else {
                    tvHoraFinRefrigerio.text = "00:00:00"
                }

                if (response.has("hora_Pausa") && !response.isNull("hora_Pausa")) {
                    // Obtener el valor de "hora_Pausa" de la respuesta JSON
                    val pausaSinContinuar = response.getString("hora_Pausa")
                    horaPausa = pausaSinContinuar
                    sharedPreferences.edit().putString(horaPausaEnv, pausaSinContinuar).apply()
                    //El boton cambia a "Continua"
                    btnInicioPausaContinuar.text = "Continuar"
                    isPaused = true
                    isContinue = false
                } else {
                    btnInicioPausaContinuar.text = "Pausa"
                }

            },
            Response.ErrorListener {
                // Manejar el error en caso de que ocurra
                //Log.e("VolleyError", error.toString())
                // tvHoraInicio.text = sharedPreferences.getString("hora", "")

            }
        ){}

        // Agregar la solicitud a la cola de solicitudes de Volley
        Volley.newRequestQueue(requireContext()).add(request)
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

    // Función para insertar la hora en la base de datos
    private fun insertarHoraInicio(url: String, hora: String, fecha:String, idUsuario:String) {
        val queue = Volley.newRequestQueue(requireContext())

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                Log.d("InsertarHoraInicio", response)

            },
            Response.ErrorListener { error ->
                Log.e("InsertarHoraInicio", "Error al insertar la hora: ${error.message}")
            }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["hora"] = hora
                params["fecha"] = fecha
                params["id_Usuario"] = idUsuario
                return params
            }
        }
        sharedPreferences.edit().putString(horaInicioEnv, hora).apply()
        sharedPreferences.edit().remove(horaInicioLocal).apply()

        queue.add(request)

    }

    // Función para insertar las horas restantes en la base de datos
    private fun insertarHora(url: String, hora: String, fecha: String, idUsuario: String, tipoAccion: String) {
        val queue = Volley.newRequestQueue(requireContext())

        val horaPausaAlmc = sharedPreferences.getString(horaPausaLocal, "00:00:00")
        val horaPausaEnviado= sharedPreferences.getString(horaPausaEnv, "00:00:00")

        Log.d("InsertarHora", "URL: $url")
        Log.d("InsertarHora", "Hora: $hora")
        Log.d("InsertarHora", "Fecha: $fecha")
        Log.d("InsertarHora", "ID Usuario: $idUsuario")
        Log.d("InsertarHora", "Tipo de acción: $tipoAccion")

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                Log.d("InsertarHora", response)
            },
            Response.ErrorListener { error ->
                Log.e("InsertarCierreHora", "Error al insertar la hora de cierre: ${error.message}")
            }) {

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                // Verificar el tipo de acción y agregar el parámetro correspondiente

                when (tipoAccion) {
                    "fin" -> {
                        params["id_Usuario"] = idUsuario
                        params["fecha"] = fecha
                        params["horaInicio"] = if (horaInicio.isNotEmpty()) horaInicio else view!!.findViewById<TextView>(R.id.tvHoraInicio).text.toString()
                        params["horaFin"] = hora
                    }
                    "pausa" -> {
                        params["id_Usuario"] = idUsuario
                        params["fecha"] = fecha
                        params["horaInicio"] = if (horaInicio.isNotEmpty()) horaInicio else view!!.findViewById<TextView>(R.id.tvHoraInicio).text.toString()
                        params["horaPausa"] = hora
                    }
                    "iniRefri" -> {
                        params["id_Usuario"] = idUsuario
                        params["fecha"] = fecha
                        params["horaInicio"] = if (horaInicio.isNotEmpty()) horaInicio else view!!.findViewById<TextView>(R.id.tvHoraInicio).text.toString()
                        params["horaIniRefri"] = hora
                    }
                    "continuar" -> {
                        params["id_Usuario"] = idUsuario
                        params["fecha"] = fecha
                        params["horaInicio"] = if (horaInicio.isNotEmpty()) horaInicio else view!!.findViewById<TextView>(R.id.tvHoraInicio).text.toString()
                        params["horaPausa"] = (if(horaPausa.isNotEmpty()) horaPausa else if (horaPausaAlmc == "00:00:00") horaPausaEnviado else horaPausaAlmc).toString()
                        params["horaContinuar"] = hora
                    }
                    "finRefrigerio" -> {
                        params["id_Usuario"] = idUsuario
                        params["fecha"] = fecha
                        params["horaIniRefri"] = if (horaInicioRefrigerio.isNotEmpty()) horaInicioRefrigerio else view!!.findViewById<TextView>(R.id.tvHoraInicioRefrigerio).text.toString()
                        params["horaFinRefri"] = hora
                    }
                }
                return params
            }
        }
        queue.add(request)
        when (tipoAccion) {
            "fin" -> {
                sharedPreferences.edit().remove(horaFinEnv).apply()
                sharedPreferences.edit().putString(horaFinEnv, hora).apply()
                sharedPreferences.edit().remove(horaFinLocal).apply()
            }
            "pausa" -> {
                sharedPreferences.edit().remove(horaPausaEnv).apply()
                sharedPreferences.edit().putString(horaPausaEnv, hora).apply()
                sharedPreferences.edit().remove(horaPausaLocal).apply()
            }
            "iniRefri" -> {
                sharedPreferences.edit().putString(horaIniRefriEnv, hora).apply()
                sharedPreferences.edit().remove(horaIniRefriLocal).apply()
            }
            "continuar" -> {
                Toast.makeText(requireContext(), "Continuar a las $hora", Toast.LENGTH_SHORT).show()
                sharedPreferences.edit().remove(horaContinuarEnv).apply()
                sharedPreferences.edit().putString(horaContinuarEnv, hora).apply()
                sharedPreferences.edit().remove(horaContinuarLocal).apply()
            }
            "finRefrigerio" -> {
                sharedPreferences.edit().putString(horaFinRefriEnv, hora).apply()
                sharedPreferences.edit().remove(horaFinRefriLocal).apply()
            }
        }

    }

    private fun verificarPermisosUbicacion(url: String, tipo: String) {
        this.url = url
        this.accionUbicacion =tipo

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permiso no aceptado por el momento
            requestLocationPermission()

        }else{
            marcarUbicacion()
        }
    }

    private fun marcarUbicacion() {
        // Verificar si se tienen los permisos necesarios
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            val ubiInicioAlmc = sharedPreferences.getString(ubiInicioLocal, null)
            val ubiPausaAlmc = sharedPreferences.getString(ubiPausaLocal, null)
            val ubiContinuarAlmc = sharedPreferences.getString(ubiContinuarLocal, null)
            val ubiIniRefriAlmc = sharedPreferences.getString(ubiIniRefriLocal, null)
            val ubiFinRefriAlmc = sharedPreferences.getString(ubiFinRefriLocal, null)
            val ubiFinAlmc = sharedPreferences.getString(ubiFinLocal, null)

            val horaInicioEnviado = sharedPreferences.getString(horaInicioEnv, "00:00:00")
            val horaIniRefriEnviado= sharedPreferences.getString(horaIniRefriEnv, "00:00:00")
            val horaFinRefriEnviado= sharedPreferences.getString(horaFinRefriEnv, "00:00:00")
            val horaPausaEnviado= sharedPreferences.getString(horaPausaEnv, "00:00:00")
            val horaContinuarEnviado= sharedPreferences.getString(horaContinuarEnv, "00:00:00")
            val horaFinEnviado= sharedPreferences.getString(horaFinEnv, "00:00:00")

            val horaInicioAlmc = sharedPreferences.getString(horaInicioLocal, "00:00:00")
            val horaPausaAlmc = sharedPreferences.getString(horaPausaLocal, "00:00:00")
            val horaContinuarAlmc = sharedPreferences.getString(horaContinuarLocal, "00:00:00")
            val horaIniRefriAlmc = sharedPreferences.getString(horaIniRefriLocal, "00:00:00")
            val horaFinRefriAlmc = sharedPreferences.getString(horaFinRefriLocal, "00:00:00")
            val horaFinAlmc = sharedPreferences.getString(horaFinLocal, "00:00:00")

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    // Verificar si se obtuvo una ubicación válida
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val ubicacion = "$latitude, $longitude"
                        if (siHayInternet()) {
                            val queue = Volley.newRequestQueue(requireContext())

                            val request = object : StringRequest(
                                Method.POST, url,
                                Response.Listener {response ->
                                    // Respuesta exitosa de la API
                                    Log.d("MarcarUbicacion", response)

                                    // Analizar la respuesta
                                    if (response.contains("exitoso")) {
                                        // La ubicación se marcó exitosamente
                                        Toast.makeText(requireContext(), "Ubicación marcada exitosamente", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // La API respondió con un mensaje de error
                                        Toast.makeText(requireContext(), "Error al marcar la ubicación: $response", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                Response.ErrorListener { error ->

                                    Toast.makeText(
                                        requireContext(),
                                        "Error en la solicitud a la API: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }) {
                                override fun getParams(): MutableMap<String, String> {
                                    val params = HashMap<String, String>()
                                    if (accionUbicacion == "iniciar") {
                                        if (ubiInicioAlmc == null) {
                                            params["hora_Inicio"] = horaInicio
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_Inicio"] = ubicacion
                                        } else {
                                            params["hora_Inicio"] = (if (horaInicioAlmc == "00:00:00") horaInicioEnviado else horaInicioAlmc).toString()
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_Inicio"] = ubiInicioAlmc
                                        }
                                        Log.d("parametros UbiInicio", "$params")
                                    } else if (accionUbicacion == "iniRefri") {
                                        if (ubiIniRefriAlmc == null) {
                                            params["hora_IniRefri"] = horaInicioRefrigerio
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_IniRefri"] = ubicacion
                                        } else {
                                            params["hora_IniRefri"] = (if (horaIniRefriAlmc == "00:00:00") horaIniRefriEnviado else horaIniRefriAlmc).toString()
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_IniRefri"] = ubiIniRefriAlmc
                                        }
                                        Log.d("parametros UbiIniRefri", "$params")
                                    } else if (accionUbicacion == "finRefri") {
                                        if (ubiFinRefriAlmc == null) {
                                            params["hora_FinRefri"] = horaFinRefrigerio
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_FinRefri"] = ubicacion
                                        } else {
                                            params["hora_FinRefri"] = (if (horaFinRefriAlmc == "00:00:00") horaFinRefriEnviado else horaIniRefriAlmc).toString()
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_FinRefri"] = ubiFinRefriAlmc
                                        }
                                        Log.d("parametros UbiFinRefri", "$params")
                                    } else if (accionUbicacion == "pausa") {
                                        if (ubiPausaAlmc == null) {
                                            params["hora_Pausa"] = horaPausa
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_Pausa"] = ubicacion
                                        } else {
                                            params["hora_Pausa"] = (if (horaPausaAlmc == "00:00:00") horaPausaEnviado else horaPausaAlmc).toString()
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_Pausa"] = ubiPausaAlmc
                                        }
                                        Log.d("parametros UbiPausa", "$params")
                                    } else if (accionUbicacion == "continuar") {
                                        if (ubiContinuarAlmc == null) {
                                            params["hora_Continuar"] = horaContinuar
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_Continuar"] = ubicacion
                                        } else {
                                            params["hora_Continuar"] = (if (horaContinuarAlmc == "00:00:00") horaContinuarEnviado else horaContinuarAlmc).toString()
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_Continuar"] = ubiContinuarAlmc
                                        }
                                        Log.d("parametros UbiContinuar", "$params")
                                    } else if (accionUbicacion == "fin") {
                                        if (ubiFinAlmc == null) {
                                            params["hora_Fin"] = horaFin
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_Fin"] = ubicacion
                                        } else {
                                            params["hora_Fin"] = (if (horaFinRefriAlmc == "00:00:00") horaFinEnviado else horaFinAlmc).toString()
                                            params["fecha"] = getCurrentDate()
                                            params["ubicacion_Fin"] = ubiFinAlmc
                                        }
                                        Log.d("parametros UbiFin", "$params")
                                    }
                                    Log.d("parametros ","$params")
                                    return params

                                }
                            }
                            queue.add(request)
                        } else {
                            when (accionUbicacion) {
                                "iniciar" -> {
                                    sharedPreferences.edit().putString(ubiInicioLocal, ubicacion).apply()
                                }
                                "fin" -> {
                                    sharedPreferences.edit().putString(ubiFinLocal, ubicacion).apply()
                                }
                                "iniRefri" -> {
                                    sharedPreferences.edit().putString(ubiIniRefriLocal, ubicacion).apply()
                                }
                                "finRefri" -> {
                                    sharedPreferences.edit().putString(ubiFinRefriLocal, ubicacion).apply()
                                }
                                "pausa" -> {
                                    sharedPreferences.edit().putString(ubiPausaLocal, ubicacion).apply()
                                }
                                "continuar" -> {
                                    sharedPreferences.edit().putString(ubiContinuarLocal, ubicacion).apply()
                                }
                            }
                        }
                        // Aquí puedes usar las coordenadas para marcar la ubicación en el mapa o realizar otras acciones
                        if (accionUbicacion == "pausa") {
                            if(horaPausa==null || horaPausa.isEmpty()){
                                Toast.makeText(requireContext(), "Pausa a las $horaPausaEnviado", Toast.LENGTH_SHORT).show()}
                            else{Toast.makeText(requireContext(), "Pausa a las $horaPausa", Toast.LENGTH_SHORT).show()}
                            Toast.makeText(requireContext(), "Ubicación marcada: $ubicacion", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Ubicación marcada:  $ubicacion", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                    }

                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error al obtener la ubicación: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "No se tienen los permisos de ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {

        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // El usuario ya ha rechazado los permisos
            Toast.makeText(requireContext(), "Permisos Rechazados. Habilitar en Configuración", Toast.LENGTH_SHORT).show()
            // El usuario ya ha rechazado los permisos anteriormente
            // Muestra un diálogo o mensaje explicando por qué necesitas los permisos y guía al usuario a la configuración de la aplicación para habilitarlos manualmente
            // Puedes utilizar AlertDialog para mostrar un diálogo con un mensaje explicativo
            val dialogBuilder = AlertDialog.Builder(requireContext())
                .setTitle("Permisos de ubicación requeridos")
                .setMessage("Esta aplicación requiere acceso a la ubicación para funcionar correctamente. Por favor, habilite los permisos de ubicación en la configuración de la aplicación.")
                .setPositiveButton("Configuración") { _, _ ->
                    // Abre la configuración de la aplicación para que el usuario pueda habilitar los permisos manualmente
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", requireActivity().packageName, null)
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
            dialogBuilder.create().show()
        } else {
            // Solicitar permisos
            val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
            val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
            val permissions = arrayOf(fineLocationPermission, coarseLocationPermission)
            requestPermissions(permissions, 777)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 777){//Permisos
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //marcarUbicacion()
            }else{
                Toast.makeText(requireContext(), "Haz rechazados los permisos", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun mostrarVentanaNotas(url: String, tipoNota: String) {
        this.reporte = url
        this.tipoNota = tipoNota

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Reporte de $tipoNota")
        val input = EditText(requireContext())
        builder.setView(input)
        builder.setPositiveButton("Enviar") { _, _ ->
            val nota = input.text.toString()
            if (nota.isNotEmpty()){
                // Aquí puedes hacer algo con la nota, como guardarla en tu base de datos
                guardarNota(nota)}
            else{
                //no realiza ninguna accion
            }
        }
        builder.setNegativeButton("No Enviar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun guardarNota(nota: String) {
        val requestQueue = Volley.newRequestQueue(requireContext())

        Log.d("guardar nota", "URL: $reporte")
        Log.d("guardar nota", "Nota: $nota")
        Log.d("guardar nota", "Tipo de acción: $tipoNota")

        val request = object : StringRequest(
            Method.POST, reporte,
            Response.Listener {response ->
                Log.d("guardar nota", response)

                // La nota se guardó correctamente en la base de datos
                // Aquí puedes manejar la respuesta del API si es necesario
                Toast.makeText(requireContext(), "Nota guardada correctamente", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener { error ->
                // Ocurrió un error al guardar la nota
                Toast.makeText(requireContext(), "Error al guardar la nota: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()

                if(tipoNota == "Pausa"){
                    params["hora_Pausa"] = horaPausa
                    params["fecha"] = getCurrentDate()
                    params["nota_Pausa"] = nota
                }else if(tipoNota == "Fin de Labor"){
                    params["hora_Fin"] = horaFin
                    params["fecha"] = getCurrentDate()
                    params["nota_Fin"] = nota
                }
                Log.d("guardar nota", "params: $params")
                return params
            }
        }
        requestQueue.add(request)

    }

    override fun onResume() {
        super.onResume()

        val idUsuario = sharedPreferences.getString("idUsuario", "")

        if (idUsuario != null && siHayInternet()) {
            val horaInicioAlmc = sharedPreferences.getString(horaInicioLocal, "00:00:00")
            val horaPausaAlmc = sharedPreferences.getString(horaPausaLocal, "00:00:00")
            val horaContinuarAlmc = sharedPreferences.getString(horaContinuarLocal, "00:00:00")
            val horaIniRefriAlmc = sharedPreferences.getString(horaIniRefriLocal, "00:00:00")
            val horaFinRefriAlmc = sharedPreferences.getString(horaFinRefriLocal, "00:00:00")
            val horaFinAlmc = sharedPreferences.getString(horaFinLocal, "00:00:00")

            val scope = CoroutineScope(Dispatchers.Main)

            scope.launch {
                if (horaInicioAlmc != null && horaInicioAlmc != "00:00:00") {
                    insertarHoraInicio(
                        "https://www.aspcontrol.com.pe/APP/insertarHoraInicio.php",
                        horaInicioAlmc,
                        getCurrentDate(),
                        idUsuario
                    )
                    verificarPermisosUbicacion("https://www.aspcontrol.com.pe/APP/insertarUbicacionInicio.php", "iniciar")
                    delay(3000) // Agregar un retraso de 1 segundo (puedes ajustar el valor según lo necesites)
                }

                if (horaPausaAlmc != null && horaPausaAlmc != "00:00:00") {
                    insertarHora(
                        "https://www.aspcontrol.com.pe/APP/insertarHoraPausa.php",
                        horaPausaAlmc,
                        getCurrentDate(),
                        idUsuario,
                        "pausa"
                    )
                    verificarPermisosUbicacion("https://www.aspcontrol.com.pe/APP/insertarUbicacionPausa.php", "pausa")
                    delay(3000) // Agregar un retraso de 1 segundo
                }

                if (horaContinuarAlmc != null && horaContinuarAlmc != "00:00:00") {
                    insertarHora(
                        "https://www.aspcontrol.com.pe/APP/insertarHoraContinuar.php",
                        horaContinuarAlmc,
                        getCurrentDate(),
                        idUsuario,
                        "continuar"
                    )
                    verificarPermisosUbicacion("https://www.aspcontrol.com.pe/APP/insertarUbicacionContinuar.php", "continuar")
                    delay(3000) // Agregar un retraso de 1 segundo
                }

                if (horaIniRefriAlmc != null && horaIniRefriAlmc != "00:00:00") {
                    insertarHora(
                        "https://www.aspcontrol.com.pe/APP/insertarHoraInicioRefrigerio.php",
                        horaIniRefriAlmc,
                        getCurrentDate(),
                        idUsuario,
                        "iniRefri"
                    )
                    verificarPermisosUbicacion("https://www.aspcontrol.com.pe/APP/insertarUbicacionIniRefri.php", "iniRefri")
                    delay(3000) // Agregar un retraso de 1 segundo
                }

                if (horaFinRefriAlmc != null && horaFinRefriAlmc != "00:00:00") {
                    insertarHora(
                        "https://www.aspcontrol.com.pe/APP/insertarHoraFinRefrigerio.php",
                        horaFinRefriAlmc,
                        getCurrentDate(),
                        idUsuario,
                        "finRefrigerio"
                    )
                    verificarPermisosUbicacion("https://www.aspcontrol.com.pe/APP/insertarUbicacionFinRefri.php", "finRefri")
                    delay(3000) // Agregar un retraso de 1 segundo
                }

                if (horaFinAlmc != null && horaFinAlmc != "00:00:00") {
                    insertarHora(
                        "https://www.aspcontrol.com.pe/APP/insertarHoraFin.php",
                        horaFinAlmc,
                        getCurrentDate(),
                        idUsuario,
                        "fin"
                    )
                    verificarPermisosUbicacion( "https://www.aspcontrol.com.pe/APP/insertarUbicacionFin.php", "fin" )
                    delay(3000) // Agregar un retraso de 1 segundo
                }
            }
        }
    }

    companion object {
        private const val PREFS_FILE_NAME = "MyPrefs"
    }

}