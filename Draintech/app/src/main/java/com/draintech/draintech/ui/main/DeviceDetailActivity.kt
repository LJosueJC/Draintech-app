package com.draintech.draintech.ui.main

import android.graphics.Paint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.draintech.draintech.ui.theme.DraintechTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Clase de datos para las gráficas
data class PuntoHistorial(val valor: Float, val timestamp: Long)

class DeviceDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mac = intent.getStringExtra("mac") ?: ""
        val nombre = intent.getStringExtra("nombre") ?: ""
        setContent {
            DraintechTheme { DeviceDetailScreen(mac, nombre) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(mac: String, nombre: String) {
    // --- Variables de estado (Sensores) ---
    var lluvia by remember { mutableStateOf(false) }
    var caudal by remember { mutableStateOf(0.0) }
    var obstruccion by remember { mutableStateOf(false) }
    var canastilla by remember { mutableStateOf(0) }
    var tapaAbierta by remember { mutableStateOf(false) }

    // Estado del registro (Control)
    var registroAbierto by remember { mutableStateOf(false) }

    // Estados de UI
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAlert by remember { mutableStateOf(false) }

    var showCloseRegisterDialog by remember { mutableStateOf(false) }
    var showOpenRegisterDialog by remember { mutableStateOf(false) }

    // Estados de Historial/Gráfica
    var showHistoryDialog by remember { mutableStateOf(false) }
    var selectedSensorName by remember { mutableStateOf("") }
    var isBooleanSensor by remember { mutableStateOf(false) }
    var historyData by remember { mutableStateOf(listOf<PuntoHistorial>()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- REFERENCIAS FIREBASE ---
    val refHistorial = FirebaseDatabase.getInstance().getReference("historial/$mac")
    val refControl = FirebaseDatabase.getInstance().getReference("control/$mac")

    // -----------------------------------------------------------------------
    //  NUEVA FUNCIÓN PRINCIPAL: Actualiza Control y Guarda en Historial
    // -----------------------------------------------------------------------
    fun cambiarEstadoRegistro(abierto: Boolean) {
        // 1. Actualizar CONTROL (Esto manda la orden al ESP32)
        refControl.child("registroAbierto").setValue(abierto)

        // 2. Guardar en HISTORIAL
        // Necesitamos guardar TAMBIÉN el estado actual de los otros sensores
        // para que las gráficas no se rompan (evitar que bajen a 0).
        val timestampSeconds = System.currentTimeMillis() / 1000.0 // Python usa segundos (float)

        val nuevoRegistro = mapOf(
            "timestamp" to timestampSeconds,
            "registroAbierto" to (if (abierto) 1 else 0),
            "lluvia" to (if (lluvia) 1 else 0),
            "caudal" to caudal,
            "obstruccion" to (if (obstruccion) 1 else 0),
            "canastilla" to canastilla,
            "tapaAbierta" to (if (tapaAbierta) 1 else 0)
        )

        refHistorial.push().setValue(nuevoRegistro).addOnFailureListener {
            Toast.makeText(context, "Error guardando en historial", Toast.LENGTH_SHORT).show()
        }
    }

    // --- FUNCIÓN FETCH HISTORIAL (Para leer datos de Gráficas) ---
    fun fetchHistory(sensorKey: String, sensorTitle: String) {
        isBooleanSensor = (sensorKey == "lluvia" || sensorKey == "obstruccion" ||
                sensorKey == "tapaAbierta" || sensorKey == "registroAbierto")

        refHistorial.orderByChild("timestamp").limitToLast(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val points = mutableListOf<PuntoHistorial>()
                    for (child in snapshot.children) {
                        val rawValue = child.child(sensorKey).value
                        val floatVal = when (rawValue) {
                            is Double -> rawValue.toFloat()
                            is Long -> rawValue.toFloat()
                            is Boolean -> if (rawValue) 1f else 0f
                            else -> 0f
                        }
                        val rawTime = child.child("timestamp").getValue(Double::class.java)
                        val timeMillis = if (rawTime != null) (rawTime * 1000).toLong() else 0L
                        points.add(PuntoHistorial(floatVal, timeMillis))
                    }
                    historyData = points
                    selectedSensorName = sensorTitle
                    showHistoryDialog = true
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error historial", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Suscripción a notificaciones
    LaunchedEffect(mac) {
        if (mac.isNotEmpty()) {
            val topic = mac.replace(":", "_")
            Firebase.messaging.subscribeToTopic(topic)
        }
    }

    // --- LISTENER 1: SENSORES (Desde Historial) ---
    val historialListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                for (child in snapshot.children) {
                    val lluviaVal = child.child("lluvia").getValue(Int::class.java) ?: 0
                    lluvia = (lluviaVal == 1)

                    caudal = child.child("caudal").getValue(Double::class.java) ?: 0.0

                    val obsVal = child.child("obstruccion").getValue(Int::class.java) ?: 0
                    obstruccion = (obsVal == 1)

                    canastilla = child.child("canastilla").getValue(Int::class.java) ?: 0

                    val tapaVal = child.child("tapaAbierta").getValue(Int::class.java) ?: 0
                    tapaAbierta = (tapaVal == 1)
                }
                errorMessage = null
            }
            isLoading = false
            isRefreshing = false
        }
        override fun onCancelled(error: DatabaseError) {
            errorMessage = "Error Historial: ${error.message}"
            isLoading = false
            isRefreshing = false
        }
    }

    // --- LISTENER 2: CONTROL (Desde Control) ---
    val controlListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                val rawReg = snapshot.child("registroAbierto").value
                registroAbierto = when (rawReg) {
                    is Boolean -> rawReg
                    is Long -> rawReg == 1L
                    else -> false
                }
            }
        }
        override fun onCancelled(error: DatabaseError) { }
    }

    // Activar Listeners
    DisposableEffect(mac) {
        val queryHistorial = refHistorial.orderByKey().limitToLast(1)
        queryHistorial.addValueEventListener(historialListener)
        refControl.addValueEventListener(controlListener)

        onDispose {
            queryHistorial.removeEventListener(historialListener)
            refControl.removeEventListener(controlListener)
        }
    }

    // Lógica de Alerta Automática
    LaunchedEffect(canastilla, registroAbierto, isLoading) {
        if (canastilla >= 70 && !isLoading && !registroAbierto) showAlert = true
        if (registroAbierto) showAlert = false
    }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombre) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A), titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFF0D1B2A))) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { isRefreshing = true; scope.launch { Toast.makeText(context, "Actualizando...", Toast.LENGTH_SHORT).show() } }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp), color = Color.White)
                    } else if (errorMessage != null) {
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    } else {
                        // BOTONES DE ACCIÓN
                        if (registroAbierto) {
                            RegisterStatusCard("Registro Abierto", "Cerrar Registro", Color(0xFFD32F2F)) { showCloseRegisterDialog = true }
                        } else {
                            RegisterStatusCard("Registro Cerrado", "Abrir Registro", Color(0xFFD32F2F)) { showOpenRegisterDialog = true }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("MAC: $mac", style = MaterialTheme.typography.titleMedium, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))

                        val sensorsConfig = listOf(
                            Triple("🌧️ Lluvia", if (lluvia) "Sí" else "No", "lluvia"),
                            Triple("💧 Caudal", "$caudal L/s", "caudal"),
                            Triple("🚫 Obstrucción", if (obstruccion) "Sí" else "No", "obstruccion"),
                            Triple("🗑️ Canastilla", "$canastilla %", "canastilla"),
                            Triple("🔓 Tapa abierta", if (tapaAbierta) "Sí" else "No", "tapaAbierta"),
                            Triple("📭 Registro abierto", if (registroAbierto) "Sí" else "No", "registroAbierto")
                        )

                        sensorsConfig.chunked(2).forEach { rowData ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                rowData.forEach { (title, value, key) ->
                                    SensorDataCard(modifier = Modifier.weight(1f), title = title, value = value, onClick = { fetchHistory(key, title) })
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS (Aquí usamos la función corregida) ---

    if (showHistoryDialog) {
        HistoryDialog(sensorName = selectedSensorName, dataPoints = historyData, isBoolean = isBooleanSensor, onDismiss = { showHistoryDialog = false })
    }

    // Alerta de llenado (Automática)
    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text("⚠️ Alerta de llenado") },
            text = { Text("La canastilla está casi llena. ¿Abrir registro?") },
            confirmButton = {
                Button(onClick = {
                    cambiarEstadoRegistro(true) // <--- CORREGIDO: Guarda en Historial y Control
                    showAlert = false
                }) { Text("Sí") }
            },
            dismissButton = { OutlinedButton(onClick = { showAlert = false }) { Text("No") } }
        )
    }

    // Diálogo Cerrar Registro
    if (showCloseRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showCloseRegisterDialog = false },
            title = { Text("Cerrar Registro") },
            text = { Text("¿Desea cerrar el registro?") },
            confirmButton = {
                Button(
                    onClick = {
                        cambiarEstadoRegistro(false) // <--- CORREGIDO
                        showCloseRegisterDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Cerrar") }
            },
            dismissButton = { OutlinedButton(onClick = { showCloseRegisterDialog = false }) { Text("Cancelar") } }
        )
    }

    // Diálogo Abrir Registro
    if (showOpenRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showOpenRegisterDialog = false },
            title = { Text("Abrir Registro") },
            text = { Text("¿Desea abrir el registro?") },
            confirmButton = {
                Button(
                    onClick = {
                        cambiarEstadoRegistro(true) // <--- CORREGIDO
                        showOpenRegisterDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Abrir") }
            },
            dismissButton = { OutlinedButton(onClick = { showOpenRegisterDialog = false }) { Text("Cancelar") } }
        )
    }
}

// --- COMPONENTES UI (Iguales que antes) ---

@Composable
fun RegisterStatusCard(title: String, buttonText: String, buttonColor: Color, onButtonClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), border = BorderStroke(2.dp, Color(0xFFB0D0FF)), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Button(onClick = onButtonClick, colors = ButtonDefaults.buttonColors(containerColor = buttonColor)) { Text(buttonText) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDataCard(modifier: Modifier = Modifier, title: String, value: String, onClick: () -> Unit) {
    Card(modifier = modifier.aspectRatio(1f), border = BorderStroke(2.dp, Color(0xFFB0D0FF)), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)), onClick = onClick) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Ver historial", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun HistoryDialog(sensorName: String, dataPoints: List<PuntoHistorial>, isBoolean: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        title = { Text("Historial: $sensorName") },
        text = {
            Column {
                Text("Últimos 10 registros", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                if (dataPoints.isNotEmpty()) {
                    SimpleLineChart(dataPoints = dataPoints, isBoolean = isBoolean, modifier = Modifier.fillMaxWidth().height(300.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    Text("No hay datos suficientes.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0D1B2A))
            ) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun SimpleLineChart(dataPoints: List<PuntoHistorial>, isBoolean: Boolean, modifier: Modifier = Modifier, lineColor: Color = Color(0xFF4CAF50)) {
    if (dataPoints.isEmpty()) return
    val values = dataPoints.map { it.valor }
    val maxVal = if (isBoolean) 1.1f else (values.maxOrNull() ?: 1f)
    val minVal = if (isBoolean) -0.1f else (values.minOrNull() ?: 0f)
    val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal
    val dateFormatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingBottom = 160f
        val paddingTop = 50f
        val paddingHorizontal = 40f
        val graphHeight = height - paddingBottom - paddingTop
        val graphWidth = width - (paddingHorizontal * 2)
        val stepX = graphWidth / (dataPoints.size - 1).coerceAtLeast(1)

        drawRect(color = Color(0xFF233044), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(width, height - paddingBottom + 20f))

        val points = dataPoints.mapIndexed { index, punto ->
            val x = paddingHorizontal + (index * stepX)
            val normalizedY = (punto.valor - minVal) / range
            val y = (paddingTop + graphHeight) - (normalizedY * graphHeight)
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(color = lineColor, start = points[i], end = points[i + 1], strokeWidth = 4.dp.toPx())
        }
        points.forEachIndexed { index, point ->
            drawCircle(color = Color.White, center = point, radius = 4.dp.toPx())
            val paintText = Paint().apply { color = android.graphics.Color.WHITE; textSize = 28f; textAlign = Paint.Align.CENTER }
            val paintDate = Paint().apply { color = android.graphics.Color.BLACK; textSize = 26f; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
            drawIntoCanvas { canvas ->
                val etiquetaValor = if (isBoolean) { if (dataPoints[index].valor > 0.5f) "Sí" else "No" } else { String.format("%.1f", dataPoints[index].valor) }
                canvas.nativeCanvas.drawText(etiquetaValor, point.x, point.y - 20f, paintText)
                val fechaStr = dateFormatter.format(Date(dataPoints[index].timestamp))
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(point.x, height - 10f)
                canvas.nativeCanvas.rotate(-90f)
                canvas.nativeCanvas.drawText(fechaStr, 0f, 0f, paintDate)
                canvas.nativeCanvas.restore()
            }
        }
    }
}