package com.asp.loginhome.secciones.cuenta.perfil

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.graphics.Paint
import java.io.File
import android.os.Environment
import java.io.FileOutputStream
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.icu.util.Calendar
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
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

class MisJornadas : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var misJornadasAdapter: MisJornadasAdapter

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val usarDataFake = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_jornadas)


        recyclerView = findViewById(R.id.recyclerViewMisJornadas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        misJornadasAdapter = MisJornadasAdapter(this, listaMisJornadas)
        recyclerView.adapter = misJornadasAdapter

        val idUsuario = intent.getStringExtra("idUsuario")

        val btnExportar = findViewById<Button>(R.id.btnExportarReporte)
        btnExportar.setOnClickListener {
            // CA1: Seleccionar rango de fechas
            seleccionarRangoFechas(idUsuario.toString())
        }


        obtenerMisJornadasDesdeServidor(idUsuario.toString())

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayoutMisJornadas)
        swipeRefreshLayout.setOnRefreshListener {
            listaMisJornadas.clear()
            obtenerMisJornadasDesdeServidor(idUsuario.toString())
            // Lógica para recargar los datos
        }
/*
        MisJornadasAdapter.setOnItemClickListener(object : MisJornadasAdapter.OnItemClickListener {
            override fun onItemClick(miJornada: MiJornada) {

            }
        })

 */
    }

    private fun seleccionarRangoFechas(idUsuario: String) {
        val calendar = Calendar.getInstance()

        // Primero seleccionamos fecha de inicio
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val fechaInicio = "$dayOfMonth/${month + 1}/$year"

                // Luego fecha de fin
                DatePickerDialog(
                    this,
                    { _, year2, month2, dayOfMonth2 ->
                        val fechaFin = "$dayOfMonth2/${month2 + 1}/$year2"

                        // Pasamos al popup de formatos (CA2)
                        mostrarPopupFormato(idUsuario, fechaInicio, fechaFin)

                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()

            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    private fun mostrarPopupFormato(idUsuario: String, fechaInicio: String, fechaFin: String) {
        val opciones = arrayOf("PDF", "Excel (.xlsx)")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exportar reporte como:")
        builder.setItems(opciones) { _, which ->
            when (which) {
                0 -> exportarReportePDF(idUsuario, fechaInicio, fechaFin)   // PDF
                1 -> exportarReporteExcel(idUsuario, fechaInicio, fechaFin) // Excel
            }
        }
        builder.show()
    }
    private fun exportarReporteExcel(idUsuario: String, fechaInicio: String, fechaFin: String) {
        Toast.makeText(this, "Funcionalidad de Excel en desarrollo", Toast.LENGTH_SHORT).show()
    }


    @SuppressLint("InflateParams")
    private fun exportarReportePDF(idUsuario: String, fechaInicio: String, fechaFin: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Generando reporte PDF...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        if (usarDataFake) {
            val fakeData = """
            [
                {"fecha_Jornada":"03/02/2025","id_Jornada":"3","reporte_Jornada":"Reporte 3",
                 "hora_Inicio":"07:30","hora_Fin":"16:00",
                 "ubicacion_Inicio":"Chorrillos","ubicacion_Fin":"Surco",
                 "hora_IniRefri":"12:30","hora_FinRefri":"13:15",
                 "ubicacion_IniRefri":"Cafetería","ubicacion_FinRefri":"Cafetería",
                 "total_Horas":"7.5"}
            ]
        """.trimIndent()
            val response = JSONArray(fakeData)
            recyclerView.postDelayed({
                progressDialog.dismiss()
                generarPDF(response,fechaInicio, fechaFin)
            }, 1000)
        } else {
            // Simular llamada a la API para obtener datos
            // API con rango de fechas
            val url = "${BaseApi.BaseURL}reporteJornadas.php?idUsuario=$idUsuario&fechaInicio=$fechaInicio&fechaFin=$fechaFin"
            val requestQueue: RequestQueue = Volley.newRequestQueue(this)

            val jsonArrayRequest = JsonArrayRequest(
                Request.Method.GET, url, null,
                { response ->
                    progressDialog.dismiss()
                    generarPDF(response, fechaInicio, fechaFin)
                },
                { error ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error al generar reporte", Toast.LENGTH_SHORT).show()
                }
            )
            requestQueue.add(jsonArrayRequest)
        }
    }
    private fun generarPDF(response: JSONArray, fechaInicio: String, fechaFin: String) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // A4 horizontal: ancho = 2010px, alto = 1200px
        val pageInfo = PdfDocument.PageInfo.Builder(2010, 1200, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // === Encabezado ===
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 48f
        canvas.drawText("Reporte de Jornadas", 800f, 100f, titlePaint)

        paint.textSize = 24f
        canvas.drawText("Nombre de la Empresa: Demo S.A.C", 40f, 160f, paint)
        canvas.drawText("RUC: 12345678901", 40f, 200f, paint)
        canvas.drawText("Nombre del Trabajador: Juan Pérez", 40f, 240f, paint)
        canvas.drawText("Documento: DNI 12345678", 40f, 280f, paint)
        canvas.drawText("Periodo: $fechaInicio - $fechaFin", 40f, 320f, paint)

        // === Cabecera de la tabla ===
        titlePaint.textSize = 22f
        var startY = 400f
        val colY = startY

        // Definir posiciones X para cada columna (más espaciado porque es horizontal)
        val colX = arrayOf(40f, 300f, 600f, 1000f, 1300f, 1700f)

        canvas.drawText("FECHA", colX[0], colY, titlePaint)
        canvas.drawText("HORA INICIO", colX[1], colY, titlePaint)
        canvas.drawText("UBICACIÓN INICIO", colX[2], colY, titlePaint)
        canvas.drawText("HORA FIN", colX[3], colY, titlePaint)
        canvas.drawText("UBICACIÓN FIN", colX[4], colY, titlePaint)
        canvas.drawText("TOTAL HORAS", colX[5], colY, titlePaint)

        // === Filas de jornadas ===
        var y = startY + 50
        paint.textSize = 20f

        for (i in 0 until response.length()) {
            val jornada = response.getJSONObject(i)

            canvas.drawText(jornada.getString("fecha_Jornada"), colX[0], y, paint)
            canvas.drawText(jornada.getString("hora_Inicio"), colX[1], y, paint)
            canvas.drawText(jornada.getString("ubicacion_Inicio"), colX[2], y, paint)
            canvas.drawText(jornada.getString("hora_Fin"), colX[3], y, paint)
            canvas.drawText(jornada.getString("ubicacion_Fin"), colX[4], y, paint)
            canvas.drawText(jornada.getString("total_Horas"), colX[5], y, paint)

            y += 40
        }

        // === Resumen ===
        y += 80
        titlePaint.textSize = 26f
        canvas.drawText("Resumen del Periodo", 40f, y, titlePaint)
        y += 40
        paint.textSize = 22f
        canvas.drawText("Total de horas trabajadas: 0", 40f, y, paint)
        y += 30
        canvas.drawText("Promedio de horas trabajadas: 0", 40f, y, paint)
        y += 30
        canvas.drawText("Días trabajados: 0", 40f, y, paint)

        pdfDocument.finishPage(page)

        // Guardar en carpeta Descargas
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val fechaInicioSafe = fechaInicio.replace("/", "-")
        val fechaFinSafe = fechaFin.replace("/", "-")

        val nombreBase = "Reporte de Jornada_${fechaInicioSafe}_${fechaFinSafe}.pdf"
        val file = obtenerArchivoUnico(downloadsDir, nombreBase,"pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(this, "PDF guardado en Descargas: ${file.name}", Toast.LENGTH_LONG).show()

        // ✅ Abrir con FileProvider
        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se encontró una app para abrir PDF", Toast.LENGTH_LONG).show()
        }
    }

    fun obtenerArchivoUnico(baseDir: File, baseName: String, extension: String): File {
        var file = File(baseDir, "$baseName.$extension")
        var index = 1
        while (file.exists()) {
            file = File(baseDir, "$baseName($index).$extension")
            index++
        }
        return file
    }
    class MisJornadasAdapter(
        private val context: Context, // Agrega el contexto como parámetro
        private val listaMisJornadas: List<MiJornada>
    ) : RecyclerView.Adapter<MisJornadasAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val fechaTextView: TextView = itemView.findViewById(R.id.fechaJornada)
            val idJTextView: TextView = itemView.findViewById(R.id.idJornada)
            val horaInicioTextView: TextView = itemView.findViewById(R.id.horaInicioJ)
            val horaFinTextView: TextView = itemView.findViewById(R.id.horaFinJ)
            val ubiInicioTextView: TextView = itemView.findViewById(R.id.UbiIncioJ)
            val ubiFinTextView: TextView = itemView.findViewById(R.id.UbiFinJ)

            init {
                // Configura un OnClickListener para ubiInicioTextView
                ubiInicioTextView.setOnClickListener {
                    val ubicacionInicio = ubiInicioTextView.text.toString()
                    if (!ubicacionInicio.isNullOrEmpty()) {
                        abrirGoogleMapsConCoordenadas(ubicacionInicio)
                    }
                }

                // Configura un OnClickListener para ubiFinTextView
                ubiFinTextView.setOnClickListener {
                    val ubicacionFin = ubiFinTextView.text.toString()
                    if (!ubicacionFin.isNullOrEmpty()) {
                        abrirGoogleMapsConCoordenadas(ubicacionFin)
                    }
                }
            }
        }
        private fun abrirGoogleMapsConCoordenadas(coordenadas: String) {
            // Crea un Intent para abrir Google Maps con las coordenadas
            val gmmIntentUri = Uri.parse("https://maps.google.com/?q=$coordenadas&layer=c&cbll=$coordenadas")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

            // Especifica que deseas abrir Google Maps
            mapIntent.setPackage("com.google.android.apps.maps")

            // Verifica si hay una aplicación que pueda manejar el intent
            context.startActivity(mapIntent)
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_mi_jornada, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val usuario = listaMisJornadas[position]
            holder.fechaTextView.text = usuario.fecha
            holder.idJTextView.text = usuario.idJ
            holder.horaInicioTextView.text = usuario.horaInicio
            holder.horaFinTextView.text = usuario.horaFin

            // Verifica si las Horas son nulas o vacías antes de mostrarlas
            if (!usuario.horaInicio.isNullOrEmpty()) {
                holder.horaInicioTextView.text = usuario.horaInicio
                holder.horaInicioTextView.isEnabled = true
            } else {
                holder.horaInicioTextView.text = "No hay marcación"
                holder.horaInicioTextView.isEnabled = false
            }

            if (!usuario.horaFin.isNullOrEmpty()) {
                holder.horaFinTextView.text = usuario.horaFin
                holder.horaFinTextView.isEnabled = true
            } else {
                holder.horaFinTextView.text = "No hay marcación"
                holder.horaFinTextView.isEnabled = false
            }

            // Verifica si las ubicaciones son nulas o vacías antes de mostrarlas
            if (!usuario.ubiInicio.isNullOrEmpty()) {
                holder.ubiInicioTextView.text = usuario.ubiInicio
                holder.ubiInicioTextView.isEnabled = true
            } else {
                holder.ubiInicioTextView.text = "No hay marcación"
                holder.ubiInicioTextView.isEnabled = false
            }

            if (!usuario.ubiFin.isNullOrEmpty()) {
                holder.ubiFinTextView.text = usuario.ubiFin
                holder.ubiFinTextView.isEnabled = true
            } else {
                holder.ubiFinTextView.text = "No hay marcación"
                holder.ubiFinTextView.isEnabled = false
            }

            holder.itemView.setOnClickListener {
                onItemClickListener?.onItemClick(usuario)
            }
        }

        override fun getItemCount(): Int {
            return listaMisJornadas.size
        }

        private var onItemClickListener: OnItemClickListener? = null

        interface OnItemClickListener {
            fun onItemClick(mijornada: MiJornada)
        }

        fun setOnItemClickListener(listener: OnItemClickListener) {
            onItemClickListener = listener
        }

    }

    data class MiJornada(
        val fecha: String,
        val idJ: String,
        val reporteJ: String,
        val horaInicio: String,
        val horaFin: String,
        val ubiInicio: String,
        val ubiFin: String,
        val horaIniRef: String,
        val horaFinRef: String,
        val ubiIniRef: String,
        val ubiFinRef: String,
        val listaPausas: MutableList<MiPausa>
    )
    data class MiPausa(
        val idP: String,
        val reporteP: String,
        val horaPausa: String,
        val horaContinuar: String,
        val ubiPausa: String,
        val ubiContinuar: String
    )

    private val listaMisJornadas = mutableListOf<MiJornada>()

    private fun obtenerMisJornadasDesdeServidor(idUsuario: String) {
        // Construir la URL con el parámetro idUsuario
        val url = "${BaseApi.BaseURL}obtenerJornadas.php?idUsuario=$idUsuario"

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response: JSONArray ->
                // Procesar la respuesta JSON para obtener datos de Jornadas
                procesarRespuestaJSON(response)
            },
            { error ->
                // Manejar errores de la solicitud Volley
                error.printStackTrace()
            }
        )

        requestQueue.add(jsonArrayRequest)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun procesarRespuestaJSON(response: JSONArray) {
        listaMisJornadas.clear()

        for (i in 0 until response.length()) {
            val jornadaJSON: JSONObject = response.getJSONObject(i)

            // Obtener datos de la jornada
            val fecha = jornadaJSON.getString("fecha_Jornada")
            val idJ = jornadaJSON.getString("id_Jornada")
            val reporteJ = jornadaJSON.getString("reporte_Jornada")
            val horaInicio = jornadaJSON.getString("hora_Inicio")
            val horaFin = jornadaJSON.getString("hora_Fin")
            val ubiInicio = jornadaJSON.getString("ubicacion_Inicio")
            val ubiFin = jornadaJSON.getString("ubicacion_Fin")
            val horaIniRef = jornadaJSON.getString("hora_IniRefri")
            val horaFinRef = jornadaJSON.getString("hora_FinRefri")
            val ubiIniRef = jornadaJSON.getString("ubicacion_IniRefri")
            val ubiFinRef = jornadaJSON.getString("ubicacion_FinRefri")

            // Obtener el array de pausas de la jornada actual
            val pausasJSON = jornadaJSON.getJSONArray("pausas")
            val listaPausas = mutableListOf<MiPausa>()

            for (j in 0 until pausasJSON.length()) {
                val pausaJSON: JSONObject = pausasJSON.getJSONObject(j)

                // Obtener datos de la pausa
                val idP = pausaJSON.getString("id_Pausa")
                val reporteP = pausaJSON.getString("reporte_Pausa")
                val horaPausa = pausaJSON.getString("hora_Pausa")
                val horaContinuar = pausaJSON.getString("hora_Continuar")
                val ubiPausa = pausaJSON.getString("ubicacion_Pausa")
                val ubiContinuar = pausaJSON.getString("ubicacion_Continuar")

                // Crear objeto MiPausa y agregarlo a la lista de pausas
                val pausa = MiPausa(idP, reporteP, horaPausa, horaContinuar, ubiPausa, ubiContinuar)
                listaPausas.add(pausa)
            }

            // Crear objeto MiJornada y agregarlo a la lista de jornadas
            val jornada = MiJornada(
                fecha, idJ, reporteJ, horaInicio, horaFin, ubiInicio, ubiFin, horaIniRef, horaFinRef, ubiIniRef,ubiFinRef, listaPausas)
            listaMisJornadas.add(jornada)

            Log.d("Jornadas", "Jornada en posición $i - ID: $idJ, Fecha: $fecha, Hora Inicio: " +
                    "$horaInicio y Ubicacion Inicio: $ubiInicio")
        }

        Log.d("Jornadas", "Total de jornadas: ${listaMisJornadas.size}")
        misJornadasAdapter.notifyDataSetChanged()
        swipeRefreshLayout.isRefreshing = false
    }


}