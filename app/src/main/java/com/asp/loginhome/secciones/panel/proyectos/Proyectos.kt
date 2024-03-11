    package com.asp.loginhome.secciones.panel.proyectos

    import android.annotation.SuppressLint
    import android.content.Context
    import android.content.Intent
    import android.content.SharedPreferences
    import android.os.Bundle
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.Button
    import android.widget.TextView
    import androidx.appcompat.app.AppCompatActivity
    import androidx.appcompat.widget.SearchView
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    import com.android.volley.Request
    import com.android.volley.RequestQueue
    import com.android.volley.toolbox.JsonArrayRequest
    import com.android.volley.toolbox.Volley
    import com.asp.loginhome.R
    import com.asp.loginhome.recursos.BaseApi
    import org.json.JSONArray
    import org.json.JSONObject

    class Proyectos : AppCompatActivity() {

        private lateinit var recyclerView: RecyclerView
        private lateinit var proyectosAdapter: ProyectosAdapter

        private lateinit var sharedPreferences: SharedPreferences

        private val rol = "rol"

        private lateinit var btnCrearProyecto: Button

        private lateinit var swipeRefreshLayout: SwipeRefreshLayout

        @SuppressLint("MissingInflatedId")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContentView(R.layout.activity_proyectos)

            sharedPreferences = this.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

            btnCrearProyecto = findViewById(R.id.buttonCrearProyecto)

            when (sharedPreferences.getString(rol, "")) {
                "1" -> {
                    btnCrearProyecto.visibility = View.VISIBLE
                }

                "2" -> {
                    btnCrearProyecto.visibility = View.GONE
                }

                "0" -> {
                    btnCrearProyecto.visibility = View.GONE
                }

            }

            btnCrearProyecto.setOnClickListener {
                val intent = Intent(this@Proyectos, CrearProyecto::class.java)
                startActivity(intent)
            }

            recyclerView = findViewById(R.id.recyclerViewProyectos)
            recyclerView.layoutManager = LinearLayoutManager(this)
            proyectosAdapter = ProyectosAdapter(this, listaProyectos)
            recyclerView.adapter = proyectosAdapter

            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayoutProyectos)
            swipeRefreshLayout.setOnRefreshListener {
                listaProyectos.clear()
                obtenerProyectosDesdeServidor()
                // Lógica para recargar los datos
            }

            obtenerProyectosDesdeServidor()

            proyectosAdapter.setOnItemClickListener(object : ProyectosAdapter.OnItemClickListener {
                override fun onItemClick(proyecto: Proyecto) {
                    val intent = Intent(this@Proyectos, DatosProyecto::class.java)
                    intent.putExtra("idProyecto", proyecto.id)
                    intent.putExtra("nombre", proyecto.nombre)
                    intent.putExtra("descripcion", proyecto.descripcion)
                    intent.putExtra("supervisor", proyecto.supervisor)
                    intent.putExtra("estado", proyecto.estado)
                    intent.putExtra("tiempoDesarrollo", proyecto.tiempoDesarrollo)
                    intent.putExtra("fechaEntrega", proyecto.fechaEntrega)
                    intent.putExtra("fechaCreacion", proyecto.fechaCreacion)
                    startActivity(intent)
                }
            })


            // Dentro de onCreate()
            val searchViewProyectos = findViewById<SearchView>(R.id.searchViewProyectos)
            searchViewProyectos.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrBlank()) {
                        proyectosAdapter.mostrarTodosLosProyectos()
                    } else {
                        proyectosAdapter.filter(newText)
                    }
                    return true
                }
            })

        }


        class ProyectosAdapter(
            private val context: Context,
            listaProyectos: List<Proyecto>
        ) : RecyclerView.Adapter<ProyectosAdapter.ViewHolder>() {

            private val listaProyectosOriginal: List<Proyecto> = listaProyectos
            private val listaProyectosFiltrada = mutableListOf<Proyecto>()

            init {
                listaProyectosFiltrada.addAll(listaProyectosOriginal)
            }

            inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val idTextView: TextView = itemView.findViewById(R.id.idProyecto)
                val nombreTextView: TextView = itemView.findViewById(R.id.nombreTarea)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val view = LayoutInflater.from(context).inflate(R.layout.item_proyecto, parent, false)
                return ViewHolder(view)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val proyecto = listaProyectosFiltrada[position]
                holder.idTextView.text = proyecto.id
                holder.nombreTextView.text = proyecto.nombre

                holder.itemView.setOnClickListener {
                    onItemClickListener?.onItemClick(proyecto)
                }
            }

            override fun getItemCount(): Int {
                return listaProyectosFiltrada.size
            }

            private var onItemClickListener: OnItemClickListener? = null

            interface OnItemClickListener {
                fun onItemClick(proyecto: Proyecto)
            }

            fun setOnItemClickListener(listener: OnItemClickListener) {
                onItemClickListener = listener
            }
            @SuppressLint("NotifyDataSetChanged")
            fun mostrarTodosLosProyectos() {
                listaProyectosFiltrada.clear()
                listaProyectosFiltrada.addAll(listaProyectosOriginal)
                notifyDataSetChanged()
            }

            @SuppressLint("NotifyDataSetChanged")
            fun filter(query: String?) {
                listaProyectosFiltrada.clear()
                if (query.isNullOrBlank()) {
                    listaProyectosFiltrada.addAll(listaProyectosOriginal)
                } else {
                    val lowerCaseQuery = query.lowercase()
                    listaProyectosOriginal.forEach {
                        if (it.id.lowercase().contains(lowerCaseQuery)) {
                            listaProyectosFiltrada.add(it)
                        }
                    }
                }
                notifyDataSetChanged()
            }
        }

        data class Proyecto(
            val id: String,
            val nombre: String,
            val descripcion: String,
            val supervisor: String,
            val estado: String,
            val tiempoDesarrollo: String,
            val fechaEntrega: String,
            val fechaCreacion: String
        )

        companion object {
            private const val PREFS_FILE_NAME = "MyPrefs"
        }

        private val listaProyectos = mutableListOf<Proyecto>()

        @SuppressLint("NotifyDataSetChanged")
        private fun obtenerProyectosDesdeServidor() {
            val url = "${BaseApi.BaseURL}obtenerProyectos.php"

            val requestQueue: RequestQueue = Volley.newRequestQueue(this)
            val jsonArrayRequest = JsonArrayRequest(
                Request.Method.GET, url, null,
                { response: JSONArray ->
                    procesarRespuestaJSON(response)
                    proyectosAdapter.notifyDataSetChanged() // Notificar cambios aquí
                },
                { error ->
                    error.printStackTrace()
                }
            )

            requestQueue.add(jsonArrayRequest)
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun procesarRespuestaJSON(response: JSONArray) {
            listaProyectos.clear()
            for (i in 0 until response.length()) {
                val proyectoJSON: JSONObject = response.getJSONObject(i)
                val id = proyectoJSON.getString("idProyecto")
                val nombre = proyectoJSON.getString("nombreProyecto")
                val descripcion = proyectoJSON.getString("descripcion")
                val supervisor = proyectoJSON.getString("supervisor")
                val estado = proyectoJSON.getString("estado")
                val tiempoDesarrollo = proyectoJSON.getString("tiempoDesarrollo")
                val fechaEntrega = proyectoJSON.getString("fechaEntrega")
                val fechaCreacion = proyectoJSON.getString("fechaCreacion")
                Log.d("Proyectos", "Proyecto en posición $i - ID: $id, Nombre: $nombre")

                val proyecto = Proyecto(id, nombre, descripcion, supervisor, estado, tiempoDesarrollo, fechaEntrega, fechaCreacion)
                listaProyectos.add(proyecto)
            }
            // Actualiza la lista filtrada
            proyectosAdapter.mostrarTodosLosProyectos()
            proyectosAdapter.notifyDataSetChanged() // Notificar cambios aquí
            // Detener la animación de actualización del SwipeRefreshLayout
            swipeRefreshLayout.isRefreshing = false
        }
    }

