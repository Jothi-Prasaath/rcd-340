package com.budz.vwpolo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val DarkBg = Color(0xFF080B10)
private val DarkPanel = Color(0xFF121821)
private val DarkPanel2 = Color(0xFF19222E)
private val LightBg = Color(0xFFF3F5F8)
private val LightPanel = Color(0xFFFFFFFF)
private val MutedDark = Color(0xFF9BA8B8)
private val MutedLight = Color(0xFF596575)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PoloApp(this) }
    }
}

private fun prefs(context: Context) = context.getSharedPreferences("polo_companion", Context.MODE_PRIVATE)
private fun saved(context: Context, key: String, fallback: String) = prefs(context).getString(key, fallback) ?: fallback
private fun savedLong(context: Context, key: String, fallback: Long = 0L) = prefs(context).getLong(key, fallback)

@Composable
private fun PoloApp(activity: ComponentActivity) {
    val context = activity
    var page by remember { mutableStateOf("Drive") }
    var refresh by remember { mutableIntStateOf(0) }
    var vehicleName by remember { mutableStateOf(saved(context, "vehicle_name", "My Polo")) }
    var model by remember { mutableStateOf(saved(context, "vehicle_model", "2019 Volkswagen Polo 1.5 TDI Highline Plus")) }
    var subtitle by remember { mutableStateOf(saved(context, "vehicle_subtitle", "1.5 TDI · Highline Plus")) }
    var theme by remember { mutableStateOf(saved(context, "theme", "Dark")) }
    var accentName by remember { mutableStateOf(saved(context, "accent", "Neon Red")) }
    var mode by remember { mutableStateOf(saved(context, "drive_mode", "NORMAL")) }
    var toast by remember { mutableStateOf("") }
    val isDark = theme != "Light"
    val bg = if (isDark) DarkBg else LightBg
    val panel = if (isDark) DarkPanel else LightPanel
    val text = if (isDark) Color(0xFFF4F7FB) else Color(0xFF111820)
    val muted = if (isDark) MutedDark else MutedLight
    val accent = when (accentName) {
        "Neon Blue" -> Color(0xFF00B7FF)
        "Neon Green" -> Color(0xFF39E58C)
        "Neon Purple" -> Color(0xFFB388FF)
        "Neon Orange" -> Color(0xFFFF9D42)
        else -> Color(0xFFFF3B45)
    }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); refresh++ }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            ContextCompat.startForegroundService(context, Intent(context, TripTrackingService::class.java).setAction(TripTrackingService.ACTION_START))
            toast = "GPS trip started"
        } else toast = "Location permission is needed for GPS trips"
    }

    MaterialTheme(colorScheme = if (isDark) darkColorScheme(primary = accent, background = bg, surface = panel, onSurface = text, onBackground = text) else lightColorScheme(primary = accent, background = bg, surface = panel, onSurface = text, onBackground = text)) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("POLO / COMPANION", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(vehicleName.ifBlank { "My Polo" }, color = text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.size(42.dp).background(panel, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = accent, modifier = Modifier.size(25.dp)) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Drive", "Trips", "Customize").forEach { item ->
                        val selected = page == item
                        Box(Modifier.weight(1f).background(if (selected) accent.copy(alpha = .15f) else panel, RoundedCornerShape(12.dp)).clickable { page = item }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(item, color = if (selected) accent else muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
                when (page) {
                    "Drive" -> DrivePage(context, vehicleName, model, subtitle, mode, panel, bg, text, muted, accent, refresh,
                        onMode = { mode = it; prefs(context).edit().putString("drive_mode", it).apply() },
                        onStartTrip = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (granted) { ContextCompat.startForegroundService(context, Intent(context, TripTrackingService::class.java).setAction(TripTrackingService.ACTION_START)); toast = "GPS trip started" }
                            else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS).filter { it != Manifest.permission.POST_NOTIFICATIONS || Build.VERSION.SDK_INT >= 33 }.toTypedArray())
                        }, onStopTrip = { context.stopService(Intent(context, TripTrackingService::class.java)); toast = "Trip stopped and saved" }, onMaps = { openMaps(context) }, onMusic = { openMusic(context) }, onCall = { openDialer(context) }, onSettings = { page = "Customize" })
                    "Trips" -> TripsPage(context, panel, text, muted, accent, refresh, onStart = { page = "Drive" })
                    else -> CustomizePage(vehicleName, { vehicleName = it; prefs(context).edit().putString("vehicle_name", it).apply() }, model, { model = it; prefs(context).edit().putString("vehicle_model", it).apply() }, subtitle, { subtitle = it; prefs(context).edit().putString("vehicle_subtitle", it).apply() }, theme, { theme = it; prefs(context).edit().putString("theme", it).apply() }, accentName, { accentName = it; prefs(context).edit().putString("accent", it).apply() }, panel, text, muted, accent, context)
                }
                if (toast.isNotBlank()) {
                    Text(toast, color = accent, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp).clickable { toast = "" })
                }
            }
        }
    }
}

@Composable
private fun DrivePage(context: Context, name: String, model: String, subtitle: String, mode: String, panel: Color, bg: Color, text: Color, muted: Color, accent: Color, refresh: Int, onMode: (String) -> Unit, onStartTrip: () -> Unit, onStopTrip: () -> Unit, onMaps: () -> Unit, onMusic: () -> Unit, onCall: () -> Unit, onSettings: () -> Unit) {
    val active = remember(refresh) { prefs(context).getBoolean("trip_active", false) }
    val distance = remember(refresh) { saved(context, "trip_distance_km", "0.00").toDoubleOrNull() ?: 0.0 }
    val speed = remember(refresh) { saved(context, "gps_speed_kmh", "0").toDoubleOrNull() ?: 0.0 }
    val duration = if (active) ((System.currentTimeMillis() - savedLong(context, "trip_start_ms")).coerceAtLeast(0L) / 1000L) else 0L
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(model, color = text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, color = muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.Speed, null, tint = accent, modifier = Modifier.size(38.dp))
                }
                HorizontalDivider(color = muted.copy(alpha = .25f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(if (active) speed.toInt().toString() else "--", color = accent, fontSize = 62.sp, fontWeight = FontWeight.Light)
                    Spacer(Modifier.width(8.dp)); Text("km/h\nGPS", color = muted, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("DISTANCE", "%.2f km".format(distance), panel, text, muted, accent, Modifier.weight(1f))
                    MetricCard("TRIP TIME", "%02d:%02d".format(duration / 3600, (duration % 3600) / 60), panel, text, muted, accent, Modifier.weight(1f))
                }
                Button(onClick = if (active) onStopTrip else onStartTrip, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
                    Icon(if (active) Icons.Default.Stop else Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (active) "END AND SAVE TRIP" else "START GPS TRIP", fontWeight = FontWeight.Bold)
                }
                Text("GPS readings are estimates, not ECU data. OBD-II is not connected yet.", color = muted, fontSize = 11.sp)
            }
        }
        item { Text("QUICK ACCESS", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Navigate", "Google Maps", Icons.Default.Navigation, panel, text, muted, accent, Modifier.weight(1f), onMaps)
            Tile("Music", "Open Spotify", Icons.Default.MusicNote, panel, text, muted, accent, Modifier.weight(1f), onMusic)
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Phone", "Open dialer", Icons.Default.Call, panel, text, muted, accent, Modifier.weight(1f), onCall)
            Tile("Settings", "Themes and car", Icons.Default.Tune, panel, text, muted, accent, Modifier.weight(1f), onSettings)
        } }
        item { Text("DRIVE MODE", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("NORMAL", "NIGHT", "PERFORMANCE").forEach { m ->
            Box(Modifier.weight(1f).background(if (mode == m) accent.copy(alpha = .18f) else panel, RoundedCornerShape(12.dp)).clickable { onMode(m) }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) { Text(m, color = if (mode == m) accent else muted, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        } } }
    }
}

@Composable
private fun MetricCard(label: String, value: String, panel: Color, text: Color, muted: Color, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(panel, RoundedCornerShape(14.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(label, color = muted, fontSize = 9.sp, letterSpacing = 1.sp); Text(value, color = text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun Tile(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, panel: Color, text: Color, muted: Color, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.height(96.dp).background(panel, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, title, tint = accent); Column { Text(title, color = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(sub, color = muted, fontSize = 10.sp) } }
}

@Composable
private fun TripsPage(context: Context, panel: Color, text: Color, muted: Color, accent: Color, refresh: Int, onStart: () -> Unit) {
    val distance = saved(context, "trip_distance_km", "0.00")
    val total = saved(context, "trip_total_km", "0.00")
    val count = saved(context, "trip_count", "0")
    val last = saved(context, "trip_last_summary", "No completed trip yet")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("TRIP LOG", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
        item { MetricCard("LAST TRIP DISTANCE", "$distance km", panel, text, muted, accent, Modifier.fillMaxWidth()) }
        item { MetricCard("TOTAL RECORDED DISTANCE", "$total km", panel, text, muted, accent, Modifier.fillMaxWidth()) }
        item { MetricCard("TRIPS SAVED", count, panel, text, muted, accent, Modifier.fillMaxWidth()) }
        item { Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(16.dp)).padding(16.dp)) { Text("LATEST TRIP", color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp); Spacer(Modifier.height(8.dp)); Text(last, color = text, fontSize = 14.sp) } }
        item { Text("Trip data is stored on this phone. Route geometry and export/backup will be added in a later milestone.", color = muted, fontSize = 12.sp) }
        item { Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) { Text("Back to dashboard") } }
    }
}

@Composable
private fun CustomizePage(name: String, setName: (String) -> Unit, model: String, setModel: (String) -> Unit, subtitle: String, setSubtitle: (String) -> Unit, theme: String, setTheme: (String) -> Unit, accentName: String, setAccentName: (String) -> Unit, panel: Color, text: Color, muted: Color, accent: Color, context: Context) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("VEHICLE PROFILE", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
        item { OutlinedField("Vehicle display name", name, setName, "My Polo") }
        item { OutlinedField("Model and variant", model, setModel, "Volkswagen Polo") }
        item { OutlinedField("Subtitle", subtitle, setSubtitle, "1.5 TDI · Highline Plus") }
        item { Text("APPEARANCE", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
        item { ChoiceRow("Theme", listOf("Dark", "Light"), theme, setTheme, text, muted, panel, accent) }
        item { ChoiceRow("Accent colour", listOf("Neon Red", "Neon Blue", "Neon Green", "Neon Purple", "Neon Orange"), accentName, setAccentName, text, muted, panel, accent) }
        item { Text("Changes save automatically on this phone. The vehicle name and subtitle are configurable, not hardcoded.", color = muted, fontSize = 12.sp) }
        item { OutlinedButton(onClick = { val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }, modifier = Modifier.fillMaxWidth()) { Text("App permissions and battery settings") } }
    }
}

@Composable
private fun OutlinedField(label: String, value: String, onChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, placeholder = { Text(placeholder) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
}

@Composable
private fun ChoiceRow(label: String, choices: List<String>, selected: String, onSelect: (String) -> Unit, text: Color, muted: Color, panel: Color, accent: Color) {
    Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = text, fontWeight = FontWeight.SemiBold)
        choices.forEach { choice -> Row(Modifier.fillMaxWidth().clickable { onSelect(choice) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected == choice, onClick = { onSelect(choice) }, colors = RadioButtonDefaults.colors(selectedColor = accent)); Text(choice, color = if (selected == choice) accent else muted, modifier = Modifier.padding(start = 6.dp)) } }
    }
}

private fun openMaps(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" )).setPackage("com.google.android.apps.maps").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(intent) } catch (_: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
private fun openMusic(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage("com.spotify.music") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com"))
    try { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) } catch (_: Exception) {}
}
private fun openDialer(context: Context) { try { context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {} }
