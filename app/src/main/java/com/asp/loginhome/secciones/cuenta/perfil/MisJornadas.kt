@file:Suppress("DEPRECATION")

package com.asp.loginhome.secciones.cuenta.perfil

import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import com.itextpdf.kernel.pdf.action.PdfAction
import java.text.SimpleDateFormat
import java.util.Locale
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.graphics.Paint
import java.io.File
import android.os.Environment
import java.io.FileOutputStream
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
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
import com.itextpdf.kernel.geom.PageSize
import org.json.JSONArray
import org.json.JSONObject
class MisJornadas : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var misJornadasAdapter: MisJornadasAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_jornadas)

        sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
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

        // Convertir fechas a un formato comparable (dd/MM/yyyy → yyyyMMdd)
        val formato = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val inicioDate = formato.parse(fechaInicio)
        val finDate = formato.parse(fechaFin)

        // Filtrar lista local
        val jornadasFiltradas = listaMisJornadas.filter { jornada ->
            val formatoTexto = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            val fechaJornada = formatoTexto.parse(jornada.fecha)
            fechaJornada != null && fechaJornada >= inicioDate && fechaJornada <= finDate
        }

        // Convertir a JSONArray para no tocar tu función generarPDF
        val response = JSONArray()
        for (jornada in jornadasFiltradas) {
            val obj = JSONObject().apply {
                put("fecha_Jornada", jornada.fecha)
                put("id_Jornada", jornada.idJ)
                put("reporte_Jornada", jornada.reporteJ)
                put("hora_Inicio", jornada.horaInicio)
                put("hora_Fin", jornada.horaFin)
                put("ubicacion_Inicio", jornada.ubiInicio)
                put("ubicacion_Fin", jornada.ubiFin)
                put("hora_IniRefri", jornada.horaIniRef)
                put("hora_FinRefri", jornada.horaFinRef)
                put("ubicacion_IniRefri", jornada.ubiIniRef)
                put("ubicacion_FinRefri", jornada.ubiFinRef)
                put("total_Horas", "0") // Calcula si lo necesitas
            }
            response.put(obj)
        }

        progressDialog.dismiss()
        generarPDF(response, fechaInicio, fechaFin)
    }
    @SuppressLint("DefaultLocale")
    private fun generarPDF(response: JSONArray, fechaInicio: String, fechaFin: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val formatoEntrada = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val formatoSalida = SimpleDateFormat("EEEE dd/MM/yy", Locale("es", "ES"))

        val fechaInicioSafe = fechaInicio.replace("/", "-")
        val fechaFinSafe = fechaFin.replace("/", "-")
        val nombreBase = "Reporte_de_Jornada_${fechaInicioSafe}_${fechaFinSafe}.pdf"
        val file = obtenerArchivoUnico(downloadsDir, nombreBase, "pdf")

        val pdfWriter = PdfWriter(FileOutputStream(file))
        val pdfDoc = PdfDocument(pdfWriter)
        val document = Document(pdfDoc, PageSize.A4.rotate())

        // === Encabezado ===
        document.add(
            Paragraph("Reporte de Jornadas")
                .setBold()
                .setFontSize(20f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        val nombre = sharedPreferences.getString("nombre", "") ?: ""
        val apellidoP = sharedPreferences.getString("apellidoP", "") ?: ""
        val apellidoM = sharedPreferences.getString("apellidoM", "") ?: ""

        document.add(Paragraph("Nombre de la Empresa: ASP Control S.A.C"))
        document.add(Paragraph("RUC: 20508841397"))
        document.add(Paragraph("Nombre del Trabajador: $nombre $apellidoP $apellidoM"))
        document.add(Paragraph("Documento: DNI 12345678"))
        document.add(Paragraph("Periodo: $fechaInicio - $fechaFin"))
        document.add(Paragraph("\n"))

        // === Tabla con bordes ===
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 2f, 3f, 2f, 3f, 2f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        val headers = listOf("FECHA", "HORA INICIO", "UBICACIÓN INICIO", "HORA FIN", "UBICACIÓN FIN", "TOTAL HORAS")
        headers.forEach { h ->
            table.addHeaderCell(Cell().add(Paragraph(h).setBold()))
        }

        // === Variables para resumen ===
        val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        var totalMinutos = 0
        var diasTrabajados = 0

        // === Filas dinámicas ===
        for (i in 0 until response.length()) {
            val jornada = response.getJSONObject(i)

            val fechaOriginal = jornada.getString("fecha_Jornada")
            var fechaFormateada = fechaOriginal
            val horaInicio = jornada.optString("hora_Inicio", "")
            val horaFin = jornada.optString("hora_Fin", "")
            val ubiInicio = jornada.optString("ubicacion_Inicio", "")
            val ubiFin = jornada.optString("ubicacion_Fin", "")
            try {
                val date = formatoEntrada.parse(fechaOriginal)
                if (date != null) {
                    fechaFormateada = formatoSalida.format(date)
                    // Primera letra mayúscula en el día de la semana
                    fechaFormateada = fechaFormateada.replaceFirstChar { it.uppercase() }
                }
            } catch (_: Exception) {}

            // Calcular horas trabajadas
            var horasMinTexto = "-"
            if (horaInicio.isNotEmpty() && horaFin.isNotEmpty()) {
                try {
                    val ini = formatoHora.parse(horaInicio)
                    val fin = formatoHora.parse(horaFin)
                    if (ini != null && fin != null) {
                        val diff = fin.time - ini.time
                        if (diff > 0) {
                            val horas = (diff / (1000 * 60 * 60)).toInt()
                            val minutos = ((diff / (1000 * 60)) % 60).toInt()
                            horasMinTexto = String.format("%02d:%02d", horas, minutos)

                            totalMinutos += (horas * 60) + minutos
                            diasTrabajados++
                        }
                    }
                } catch (_: Exception) {}
            }

            // === Celdas ===
            table.addCell(fechaFormateada)
            table.addCell(horaInicio)

            // Coordenadas Inicio clickeables
            if (ubiInicio.isNotEmpty()) {
                val linkInicio = Link(ubiInicio,
                    PdfAction.createURI("https://maps.google.com/?q=$ubiInicio&layer=c&cbll=$ubiInicio"))
                table.addCell(Paragraph(linkInicio))
            } else {
                table.addCell("-")
            }

            table.addCell(horaFin)

            // Coordenadas Fin clickeables
            if (ubiFin.isNotEmpty()) {
                val linkFin = Link(ubiFin,
                    PdfAction.createURI("https://maps.google.com/?q=$ubiFin&layer=c&cbll=$ubiFin"))
                table.addCell(Paragraph(linkFin))
            } else {
                table.addCell("-")
            }

            // Mostrar horas en formato HH:mm
            table.addCell(horasMinTexto)
        }

        document.add(table)

        // === Resumen ===
        document.add(Paragraph("\nResumen del Periodo").setBold().setFontSize(14f))
// Total en HH:mm
        val totalHorasInt = totalMinutos / 60
        val totalMinsInt = totalMinutos % 60
        val totalTexto = String.format("%02d:%02d", totalHorasInt, totalMinsInt)
        document.add(Paragraph("Total de horas trabajadas: $totalTexto"))
        // Promedio en HH:mm
        val promedioTexto = if (diasTrabajados > 0) {
            val promedioMin = totalMinutos / diasTrabajados
            val promHorasInt = promedioMin / 60
            val promMinsInt = promedioMin % 60
            String.format("%02d:%02d", promHorasInt, promMinsInt)
        } else {
            "00:00"
        }
        document.add(Paragraph("Promedio de horas trabajadas: $promedioTexto"))
        document.add(Paragraph("Días trabajados: $diasTrabajados"))

        document.close()

        Toast.makeText(this, "PDF guardado en Descargas: ${file.name}", Toast.LENGTH_LONG).show()

        // Abrir PDF
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        try {
            startActivity(intent)
        } catch (_: Exception) {
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