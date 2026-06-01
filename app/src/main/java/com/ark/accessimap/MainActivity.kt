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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
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

        setContent {
            AccessimapTheme {
                AccessimapApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun AccessimapApp() {
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
                AppDestinations.PROFILE -> Profile(modifier = Modifier.padding(innerPadding))
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

    LaunchedEffect(Unit) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("http://192.168.1.88:5000/api/places")
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
            GeoJsonData.JsonString(poisJson ?: "{\"type\":\"FeatureCollection\",\"features\":[]}")
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
                Text("Ratings:\nBlindness-friendly: 5\nMobility: 5", fontSize = 15.sp)
                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Button(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            showBottomSheet = false
                        }
                    }
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun Explore(modifier: Modifier = Modifier) {
    Text(
        text = "Explore screen",
        modifier = modifier
    )
}

@Composable
fun Profile(modifier: Modifier = Modifier) {
    Text(
        text = "Profile screen",
        modifier = modifier
    )
}