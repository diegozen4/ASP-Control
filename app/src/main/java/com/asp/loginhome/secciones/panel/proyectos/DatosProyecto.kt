package com.asp.loginhome.secciones.panel.proyectos

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.asp.loginhome.R
import com.asp.loginhome.recursos.BaseApi
import com.asp.loginhome.secciones.panel.tareas.CrearTarea
import com.asp.loginhome.secciones.panel.tareas.DetalleTarea
import com.asp.loginhome.secciones.panel.tareas.Tareas
import org.json.JSONArray
import org.json.JSONObject

class DatosProyecto : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tareasAdapter: Tareas.TareasAdapter
    private val listaTareas = mutableListOf<Tareas.Tarea>()

    private lateinit var txtNombreProyecto: TextView
    private lateinit var txtEtapaProyecto: TextView
    private lateinit var txtCodigoProyecto: TextView
    private lateinit var txtDescripcionProyecto: TextView
    private lateinit var txtSupervisorProyecto: TextView
    private lateinit var txtTiempoDesarrollo: TextView
    private lateinit var txtFechaEntrega: TextView
    private lateinit var txtFechaCreacion: TextView
    private lateinit var txtListaMateriales: TextView

    private lateinit var btnCrearTarea: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_proyecto)

        txtCodigoProyecto=findViewById(R.id.txtCodigoProyecto)
        txtNombreProyecto=findViewById(R.id.txtNombreProyecto)
        txtDescripcionProyecto=findViewById(R.id.txtDescripcionProyecto)
        txtSupervisorProyecto=findViewById(R.id.txtSupervisorProyecto)
        txtEtapaProyecto=findViewById(R.id.txtEtapaProyecto)
        txtTiempoDesarrollo=findViewById(R.id.txtTiempoDesarrollo)
        txtFechaEntrega=findViewById(R.id.txtFechaEntrega)
        txtFechaCreacion=findViewById(R.id.txtFechaCreacion)
        btnCrearTarea=findViewById(R.id.btnCrearTarea)

        val idProyecto = intent.getStringExtra("idProyecto")
        val nombre = intent.getStringExtra("nombre")
        val descripcion = intent.getStringExtra("descripcion")
        val supervisor = intent.getStringExtra("supervisor")
        val estado = intent.getStringExtra("estado")
        val tiempoDesarrollo = intent.getStringExtra("tiempoDesarrollo")
        val fechaEntrega = intent.getStringExtra("fechaEntrega")
        val fechaCreacion = intent.getStringExtra("fechaCreacion")

        txtCodigoProyecto.text = idProyecto
        txtNombreProyecto.text = nombre
        txtDescripcionProyecto.text = descripcion
        txtSupervisorProyecto.text = supervisor
        txtEtapaProyecto.text = estado
        txtTiempoDesarrollo.text = tiempoDesarrollo
        txtFechaEntrega.text = fechaEntrega
        txtFechaCreacion.text = fechaCreacion

        obtenerTareasProyectoDesdeServidor(idProyecto.toString())

        recyclerView = findViewById(R.id.recyclerViewTareas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        tareasAdapter = Tareas.TareasAdapter(this, listaTareas)
        recyclerView.adapter = tareasAdapter

        tareasAdapter.setOnItemClickListener(object : Tareas.TareasAdapter.OnItemClickListener {
            override fun onItemClick(tarea: Tareas.Tarea) {
                val intent = Intent(this@DatosProyecto, DetalleTarea::class.java)
                intent.putExtra("idTarea", tarea.id)
                intent.putExtra("nombreTarea", tarea.nombre)
                intent.putExtra("descripcion", tarea.descripcion)
                intent.putExtra("fechaEntrega", tarea.fecha)
                startActivity(intent)

            }
        })

        btnCrearTarea.setOnClickListener {
            val intent = Intent(this@DatosProyecto, CrearTarea::class.java)
            startActivity(intent)
        }

    }

    private fun obtenerTareasProyectoDesdeServidor(idProyecto:String) {
        val url = "${BaseApi.BaseURL}obtenerTareasProyecto.php?idProyecto=$idProyecto"

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response: JSONArray ->
                procesarRespuestaJSON(response)
            },
            { error ->
                error.printStackTrace()
            }
        )

        requestQueue.add(jsonArrayRequest)
    }

    private fun procesarRespuestaJSON(response: JSONArray) {
        for (i in 0 until response.length()) {
            val tareaJSON: JSONObject = response.getJSONObject(i)
            val id = tareaJSON.getString("id_Tarea")
            val nombre = tareaJSON.getString("nombre")
            val descripcion = tareaJSON.getString("descripcion")
            val fecha = tareaJSON.getString("fecha")
            val estado = tareaJSON.getString("estado")
            val tarea = Tareas.Tarea(id, nombre, descripcion, fecha, estado)
            listaTareas.add(tarea)

            Log.d("Tareas", "Tarea en posición $i - ID: $id, Título: $nombre, Descripción: $descripcion, Fecha: $fecha, Estado: $estado")
        }

        // Notifica al adaptador después de cargar las tareas
        tareasAdapter.notifyDataSetChanged()
        tareasAdapter.filter(null) // o tareasAdapter.filter("")

        Log.d("Tareas", "Total de tareas: ${listaTareas.size}")
    }


    class TareasAdapter(
        private val context: Context,
        listaTareas: List<Tarea>
    ) : RecyclerView.Adapter<TareasAdapter.ViewHolder>() {

        private val listaTareasOriginal: List<Tarea>
        private val listaTareasFiltrada = mutableListOf<Tarea>()

        init {
            listaTareasOriginal = listaTareas
            listaTareasFiltrada.addAll(listaTareasOriginal)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nombreTextView: TextView = itemView.findViewById(R.id.nombreTarea)
            val fechaTextView: TextView = itemView.findViewById(R.id.fechaTarea)
            val estadoTextView: TextView = itemView.findViewById(R.id.estadoTarea)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_tarea, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tarea = listaTareasFiltrada[position]
            holder.nombreTextView.text = tarea.nombre
            holder.fechaTextView.text = tarea.fecha
            holder.estadoTextView.text = tarea.estado

            holder.itemView.setOnClickListener {
                onItemClickListener?.onItemClick(tarea)
            }
        }

        override fun getItemCount(): Int {
            return listaTareasFiltrada.size
        }

        private var onItemClickListener: OnItemClickListener? = null

        interface OnItemClickListener {
            fun onItemClick(tarea: Tarea)
        }

        fun setOnItemClickListener(listener: OnItemClickListener) {
            onItemClickListener = listener
        }

        @SuppressLint("NotifyDataSetChanged")
        fun filter(query: String?) {
            listaTareasFiltrada.clear()
            if (!query.isNullOrBlank() && !query.isEmpty()) {
                val lowerCaseQuery = query.lowercase()
                listaTareasOriginal.forEach {
                    if (it.nombre.lowercase()
                            .contains(lowerCaseQuery)
                    ) {
                        listaTareasFiltrada.add(it)
                    }
                }
            } else {
                listaTareasFiltrada.addAll(listaTareasOriginal)
            }
            notifyDataSetChanged()
        }
    }

    data class Tarea(
        val id: String,
        val nombre: String,
        val fecha: String,
        val estado: String,
        val descripcion: String
    )

}