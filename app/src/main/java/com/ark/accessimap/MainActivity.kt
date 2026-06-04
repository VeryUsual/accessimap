package com.ark.accessimap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat
import com.ark.accessimap.ui.theme.AccessimapTheme
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.location.LocationTrackingEffect
import org.maplibre.compose.location.mostAccurateBearing
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberDefaultOrientationProvider
import org.maplibre.compose.location.rememberUserLocationState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import kotlin.system.exitProcess


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                4132
            )
        }

        val creds = getSharedPreferences("creds", MODE_PRIVATE)
        val username: String? = creds.getString("username", "")

        setContent {
            AccessimapTheme {
                AccessimapApp(username)
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun AccessimapApp(username: String?) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.MAP -> Map(modifier = Modifier.padding(innerPadding))
                AppDestinations.EXPLORE -> Explore(modifier = Modifier.padding(innerPadding))
                AppDestinations.PROFILE -> Profile(username, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    MAP("Map", Icons.Default.LocationOn),
    EXPLORE("Explore", Icons.Default.List),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Map(modifier: Modifier = Modifier) {
    var poisJson by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var selectedPoi by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(searchText) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("http://192.168.1.88:5000/api/places?filter=$searchText")
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("request error, response code was ${response.code}")
                response.body?.string() ?: error("empty body")
            }
        }.onSuccess { json ->
            poisJson = json
        }.onFailure { e ->
            Log.e("am", "FAIL", e)
        }
    }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(
                latitude = 43.1600424,
                longitude = -79.24372139
            ),
            zoom = 11.5
        )
    )

    val locationProvider = rememberDefaultLocationProvider()
    val orientationProvider = rememberDefaultOrientationProvider()
    val locationState = rememberUserLocationState(locationProvider, orientationProvider)

    val locationMarkerIcon = rememberVectorPainter(Icons.Default.LocationOn)

    Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            cameraState = cameraState,
            options = MapOptions(
                ornamentOptions = OrnamentOptions(
                    isLogoEnabled = false,
                    isAttributionEnabled = true,
                    isScaleBarEnabled = false,
                )
            )
        ) {
            LocationPuck(
                idPrefix = "user",
                location = locationState.location,
                bearing = locationState.mostAccurateBearing(),
                cameraState = cameraState,
            )

            LocationTrackingEffect(locationState = locationState) {
                val position = currentLocation.location?.position?.value
                if (position != null) {
                    cameraState.animateTo(CameraPosition(target = position, zoom = 15.0))
                }
            }

            val poisSource = rememberGeoJsonSource(
                GeoJsonData.JsonString(
                    poisJson ?: "{\"type\":\"FeatureCollection\",\"features\":[]}"
                )
            )

            SymbolLayer(
                id = "pois-layer",
                source = poisSource,
                iconImage = image(locationMarkerIcon, drawAsSdf = true),
                iconAnchor = const(SymbolAnchor.Center),
                iconAllowOverlap = const(true),
                onClick = { features ->
                    if (features.isNotEmpty()) {
                        selectedPoi = features[0].properties.toString()
                        showBottomSheet = true
                    }

                    ClickResult.Consume
                }
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(30.dp)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(24.dp)
        ) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search places...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                singleLine = true
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                selectedPoi = ""
            },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val poi = JSONObject(selectedPoi)
                // TODO: gonna do network request and all here to get all the reviews for a certain id

                Row {
                    repeat(5) {
                        Icon(
                            imageVector = Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Spacer(
                    modifier = Modifier.height(24.dp)
                )
                Text(poi["name"].toString(), fontSize = 30.sp)
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                Text(poi["amenity"].toString().replaceFirstChar { it.uppercase() }.replace("_", " "), fontSize = 20.sp)
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                Text(poi["address"].toString(), fontSize = 17.sp)
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Spacer(
                    modifier = Modifier.height(14.dp)
                )
                Text("Reviews", fontSize = 25.sp)
                Spacer(
                    modifier = Modifier.height(14.dp)
                )
                Text("Ratings:\nBlindness-friendly: 5\nMobility: 5", fontSize = 15.sp)
                Spacer(
                    modifier = Modifier.height(14.dp)
                )
                Text("John Doe (Blind, Wheelchair Bound)", fontSize = 14.sp)
                Text("4 stars for Blindness | 2 months ago", fontSize = 14.sp)
                Text("Very good place has braille for every sign! Sadly the washroom didn't have them!!")

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

//                Button(
//                    onClick = {
//                        scope.launch {
//                            sheetState.hide()
//                        }.invokeOnCompletion {
//                            showBottomSheet = false
//                        }
//                    }
//                ) {
//                    Text("Close")
//                }v
            }
        }
    }
}

@Composable
fun Explore(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(20.dp)) {
        Column() {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column() {
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("All (24)")
                    }
                }

                Column() {
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Parks (4)")
                    }
                }

                Column() {
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Cafes (4)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .border(
                        width = 2.dp,
                        brush = SolidColor(Color.LightGray),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(8.dp)
                    .height(100.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    Text("Tim Hortons", fontSize = 25.sp)
                    Text("28 Example Street")
                    Text("2.6 overall | 28 reviews")
                    Text("1.3 for blindness | 4 reviews")
                }
            }
        }
    }
}

@Composable
fun Profile(username: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 20.dp)) {

        Text(
            text = "Profile",
            modifier = modifier,
            fontSize = 29.sp
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Text(
            text = "$username",
            fontSize = 20.sp
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Text(
            text = "Joined June 2026"
        )

        Text(
            text = "28 reviews contributed"
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Button(
            onClick = { exitProcess(-1); }
        ) {
            Text("Sign out")
        }

    }
}