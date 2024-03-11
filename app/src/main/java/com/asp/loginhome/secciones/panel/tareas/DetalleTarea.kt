package com.asp.loginhome.secciones.panel.tareas

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.asp.loginhome.R
import com.asp.loginhome.recursos.BaseApi
import org.json.JSONArray
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask


class DetalleTarea : AppCompatActivity() {

    private var isLoading = false // Agregado para indicar si está cargando

    private lateinit var editTextMensaje:EditText
    private lateinit var buttonEnviar:ImageButton
    private lateinit var textViewNombreTarea:TextView
    private lateinit var textViewFechaTarea:TextView
    private lateinit var textViewDescripcionTarea:TextView
    private lateinit var textViewRelacionados:TextView
    private lateinit var recyclerViewChat:RecyclerView
    private lateinit var comentariosAdapter : ComentariosAdapter
    private val listaComentarios = mutableListOf<Comentario>()
    private lateinit var timer: Timer
    private lateinit var idUsuario:String


    private var currentPage = 1

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_tarea)

        sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        idUsuario = sharedPreferences.getString("idUsuario", null).toString()


        recyclerViewChat = findViewById(R.id.recyclerViewChat)


        editTextMensaje = findViewById(R.id.editTextMensaje)
        buttonEnviar = findViewById(R.id.buttonEnviar)
        textViewNombreTarea = findViewById(R.id.textViewNombreTarea)
        textViewFechaTarea = findViewById(R.id.textViewFechaTarea)
        textViewDescripcionTarea = findViewById(R.id.textViewDescripcionTarea)
        textViewRelacionados = findViewById(R.id.textViewRelacionados)

        val idTarea = intent.getStringExtra("idTarea")
        val tarea = intent.getStringExtra("nombreTarea")
        val descripcion = intent.getStringExtra("descripcion")
        val fechaEntrega = intent.getStringExtra("fechaEntrega")

        textViewNombreTarea.text = tarea
        textViewDescripcionTarea.text = descripcion
        textViewFechaTarea.text = fechaEntrega

        obtenerComentariosDesdeServidor(idTarea.toString())
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {

                obtenerComentariosDesdeServidor(idTarea.toString())
            }
        }, 0, 1000) // 60000 milisegundos = 1 minuto


        buttonEnviar.setOnClickListener {

            val mensaje = editTextMensaje.text.toString().trim()
            if (mensaje.isNotEmpty()) {
                enviarMensaje(idUsuario.toString(), idTarea.toString(), mensaje)
                // Puedes borrar el texto del EditText después de enviar el mensaje si lo deseas
                editTextMensaje.text.clear()
            }
        }

        recyclerViewChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        comentariosAdapter = ComentariosAdapter(this, listaComentarios,idUsuario)
        recyclerViewChat.adapter = comentariosAdapter
    }

    private fun obtenerComentariosDesdeServidor(idTarea:String) {
        if (isLoading) {
            return
        }
        isLoading = true // Indicar que se está cargando

        val url = "${BaseApi.BaseURL}obtenerComentariosTarea.php?idTarea=$idTarea&page=$currentPage"

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response: JSONArray ->
                isLoading = false // Indicar que la carga ha finalizado

                procesarRespuestaJSON(response)
            },
            { error ->
                isLoading = false // Indicar que la carga ha finalizado

                error.printStackTrace()
            }
        )

        requestQueue.add(jsonArrayRequest)
    }


    // falta editar valores de la siguiente funcion
    private fun procesarRespuestaJSON(response: JSONArray) {
        // Guardar la posición actual del RecyclerView antes de actualizar los comentarios
        val primeraPosicionVisible = (recyclerViewChat.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        Log.d("Primera Poscion guardada: ", primeraPosicionVisible.toString())
        // Si currentPage es 1, limpiar la lista antes de agregar los nuevos comentarios
            listaComentarios.clear()

        for (i in 0 until response.length()) {
            val tareaJSON: JSONObject = response.getJSONObject(i)
            val usuario = tareaJSON.getString("id_Usuario")
            val contenido = tareaJSON.getString("contenido")
            val fecha = tareaJSON.getString("fecha")
            val comentario = Comentario(usuario, contenido, fecha)
            listaComentarios.add(comentario)

        }

        // Ordenar la lista de comentarios al revés
        listaComentarios.reverse()

        // Notifica al adaptador después de cargar las tareas
        comentariosAdapter.notifyDataSetChanged()
        comentariosAdapter.filter(null) // o tareasAdapter.filter("")

        // Obtener la posición de desplazamiento actual y la altura total
        val currentScrollOffset = recyclerViewChat.computeVerticalScrollOffset()
        val totalScrollRange = recyclerViewChat.computeVerticalScrollRange()

          // Si el usuario está viendo el primer mensaje, desplázate hacia arriba para cargar la siguiente página
        if (primeraPosicionVisible == 0) {
            // Obtén la cantidad de elementos en la página actual antes de cargar nuevos comentarios
            val itemsEnPaginaActual = listaComentarios.size

            // Cargar nuevos comentarios
           // obtenerComentariosDesdeServidor(idTarea.toString())

            // Desplázate hacia el primer elemento de la nueva página
            recyclerViewChat.scrollToPosition(itemsEnPaginaActual)
            currentPage++
        }

        // Desplaza el RecyclerView al último elemento si estás en la última página
        else if (currentScrollOffset == totalScrollRange - recyclerViewChat.height) {
            recyclerViewChat.scrollToPosition(listaComentarios.size - 1)
        }

        // Desplaza el RecyclerView al último elemento
        //recyclerViewChat.scrollToPosition(listaComentarios.size - 1)

    }
    class ComentariosAdapter(
        private val context: Context,
        listaTareas: List<Comentario>,
        private val idUsuarioActual: String
    ) : RecyclerView.Adapter<ComentariosAdapter.ViewHolder>() {

        private val VIEW_TYPE_USUARIO_ACTUAL = 1
        private val VIEW_TYPE_OTRO_USUARIO = 2

        private val listaComentariosOriginal: List<Comentario>
        private val listaComentariosFiltrada = mutableListOf<Comentario>()

        init {
            listaComentariosOriginal = listaTareas
            listaComentariosFiltrada.addAll(listaComentariosOriginal)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nombreTextView: TextView = itemView.findViewById(R.id.nombreUsuario)
            val contenidoTextView: TextView = itemView.findViewById(R.id.contenido)
            val fechaTextView: TextView = itemView.findViewById(R.id.fechaComentario)
        }

        override fun getItemViewType(position: Int): Int {
            return if (listaComentariosFiltrada[position].usuario == idUsuarioActual) {
                VIEW_TYPE_USUARIO_ACTUAL
            } else {
                VIEW_TYPE_OTRO_USUARIO
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutResId = if (viewType == VIEW_TYPE_USUARIO_ACTUAL) {
                R.layout.item_mi_comentario
            } else {
                R.layout.item_comentario
            }

            val view = LayoutInflater.from(context).inflate(layoutResId, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val comentario = listaComentariosFiltrada[position]

            // Inflar el layout correspondiente
            val layoutResId = if (comentario.usuario == idUsuarioActual) {
                R.layout.item_mi_comentario
            } else {
                R.layout.item_comentario
            }

            val view = LayoutInflater.from(context).inflate(layoutResId, holder.itemView as ViewGroup, false)

            // Limpiar cualquier vista previa del CardView
            (holder.itemView as LinearLayout).removeAllViews()

            // Agregar la vista inflada al CardView
            (holder.itemView as LinearLayout).addView(view)

            // Obtener referencias de vistas del CardView inflado
            val nombreTextView: TextView = view.findViewById(R.id.nombreUsuario)
            val contenidoTextView: TextView = view.findViewById(R.id.contenido)
            val fechaTextView: TextView = view.findViewById(R.id.fechaComentario)

            // Asignar valores a las vistas
            nombreTextView.text = comentario.usuario
            contenidoTextView.text = comentario.contenido
            fechaTextView.text = comentario.fecha

            // Establecer el listener de clics
            holder.itemView.setOnClickListener {
                onItemClickListener?.onItemClick(comentario)
            }
        }

        override fun getItemCount(): Int {
            return listaComentariosFiltrada.size
        }

        private var onItemClickListener: OnItemClickListener? = null

        interface OnItemClickListener {
            fun onItemClick(comentario: Comentario)
        }

        fun setOnItemClickListener(listener: OnItemClickListener) {
            onItemClickListener = listener
        }

        @SuppressLint("NotifyDataSetChanged")
        fun filter(query: String?) {
            listaComentariosFiltrada.clear()
            if (!query.isNullOrBlank() && !query.isEmpty()) {
                val lowerCaseQuery = query.lowercase()
                listaComentariosOriginal.forEach {
                    if (it.usuario.lowercase()
                            .contains(lowerCaseQuery) || it.contenido.lowercase()
                            .contains(lowerCaseQuery)
                    ) {
                        listaComentariosFiltrada.add(it)
                    }
                }
            } else {
                listaComentariosFiltrada.addAll(listaComentariosOriginal)
            }
            notifyDataSetChanged()
        }
    }


    private fun enviarMensaje(idUsuario: String, idTarea: String, mensaje: String) {
        val url = "${BaseApi.BaseURL}enviarComentario.php?idTarea=$idTarea&idUsuario=$idUsuario&mensaje=$mensaje"

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)

        val jsonArrayRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { _ -> // Manejar la respuesta del servidor si es necesario
                obtenerComentariosDesdeServidor(idTarea)
                recyclerViewChat.scrollToPosition(listaComentarios.size - 1)

            },
            { error ->
                error.printStackTrace()
            }
        )
        requestQueue.add(jsonArrayRequest)
    }

    data class Comentario(
        val usuario: String,
        val contenido: String,
        val fecha: String
    )
    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }

}
