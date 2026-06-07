package com.ark.accessimap

import android.Manifest
import android.R.attr.text
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
import com.airbnb.lottie.compose.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.IOException
import org.json.JSONArray
import org.json.JSONTokener
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import com.google.android.gms.location.LocationServices

class Geo(private val lat: Double, private val lon: Double) {

    companion object {
        const val earthRadiusKm: Double = 6372.8
    }

    fun haversine(destination: Geo): Double {
        val dLat = Math.toRadians(destination.lat - this.lat);
        val dLon = Math.toRadians(destination.lon - this.lon);
        val originLat = Math.toRadians(this.lat);
        val destinationLat = Math.toRadians(destination.lat);

        val a = Math.pow(Math.sin(dLat / 2), 2.toDouble()) + Math.pow(Math.sin(dLon / 2), 2.toDouble()) * Math.cos(originLat) * Math.cos(destinationLat);
        val c = 2 * Math.asin(Math.sqrt(a));
        return earthRadiusKm * c;
    }

}

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
                AppDestinations.MAP -> Map(username, modifier = Modifier.padding(innerPadding))
                AppDestinations.EXPLORE -> Explore(username = username, modifier = Modifier.padding(innerPadding))
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
fun Map(username: String?, modifier: Modifier = Modifier) {
    var poisJson by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var selectedPoi by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(searchText) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://accessimap.pythonanywhere.com/api/places?filter=$searchText")
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
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val poi = JSONObject(selectedPoi)
                val poiId = poi["id"].toString()

                var blindnessAvg by remember { mutableStateOf(0.0) }
                var mobilityAvg by remember { mutableStateOf(0.0) }

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

                var reviews: JSONArray
                var totalBlindness = 0
                var totalMobility = 0
                var count = 0

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://accessimap.pythonanywhere.com/api/reviews/$poiId")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("the server replyed with unsuccessful code $response when querying for reviews")

                    val jsonData = response.body.string()
                    val data = JSONTokener(jsonData).nextValue() as JSONArray

                    reviews = data

                    for (i in 0 until reviews.length()) {
                        val review = reviews.getJSONObject(i)
                        val blindnessRating = review["blindness_rating"]
                        val wheelchairRating = review["wheelchair_rating"]
                        totalBlindness += blindnessRating as Int
                        totalMobility += wheelchairRating as Int
                        count++
                    }
                }

                if (count > 0) {
                    blindnessAvg = totalBlindness / count.toDouble()
                    mobilityAvg = totalMobility / count.toDouble()
                }

                val blindnessAvgString = String.format(Locale.CANADA, "%.1f", blindnessAvg)
                val mobilityAvgString = String.format(Locale.CANADA, "%.1f", mobilityAvg)

                Text("Blindness-friendly: $blindnessAvgString\nMobility: $mobilityAvgString", fontSize = 15.sp)

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Button(
                        onClick = {
                            showPopup = true
                            showBottomSheet = false
                        },
                    ) {
                        Text("Write a review")
                    }

                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                for (i in 0 until reviews.length()) {
                    val review = reviews.getJSONObject(i)

                    val blindnessRating = review["blindness_rating"]
                    val wheelchairRating = review["wheelchair_rating"]
                    val username = review["username"]
                    val reviewText = review["review_text"]

                    Text("$username (Blind, Wheelchair Bound)", fontSize = 14.sp)
                    Text("$blindnessRating stars for blindness, $wheelchairRating stars for wheelchair | Today", fontSize = 14.sp)
                    Text("$reviewText")

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    totalBlindness += blindnessRating as Int
                    totalMobility += wheelchairRating as Int
                    count++
                }
            }
        }
    }

    var showSuccessAnim by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(
        targetValue = if (showSuccessAnim) 15.dp else 0.dp,
        animationSpec = tween(2000)
    )

    var wheelchairRating by remember { mutableStateOf(0) }
    var blindnessRating by remember { mutableStateOf(0) }
    var reviewText by remember { mutableStateOf("") }

    if (showPopup) {
        Popup(
            alignment = Alignment.Center,
            onDismissRequest = { showPopup = false },
            properties = PopupProperties(focusable = true)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .padding(60.dp)
                    .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            ) {
                Column {
                    Text("Write your review:")
                    Spacer(modifier = Modifier.height(15.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text("Wheelchair: ")
                        StarRatingChooser(rating = wheelchairRating, onRatingChange = { wheelchairRating = it })
                    }
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text("Blindness: ")
                        StarRatingChooser(rating = blindnessRating, onRatingChange = { blindnessRating = it })
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                    TextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    Button(onClick = {
                        Log.d("accessimap", "$wheelchairRating $blindnessRating $reviewText")

                        val poi = JSONObject(selectedPoi)

                        val poiId = poi["id"].toString()

                        val encodedReviewText = URLEncoder.encode(reviewText, StandardCharsets.UTF_8)

                        val client = OkHttpClient()
                        val request = Request.Builder()
                            .url(
                                "https://accessimap.pythonanywhere.com/api/review/submit?wheelchair_rating=$wheelchairRating&blindness_rating=$blindnessRating&username=$username&review_text=$encodedReviewText&place_id=$poiId"
                            )
                            .build()
                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                Log.e("accessimap", e.toString())
                            }

                            override fun onResponse(call: Call, response: Response) {
                                response.body.string().let { data ->
                                    Log.d("Accessimap", data)
                                }
                            }
                        })
                        showSuccessAnim = true
                    }) {
                        Text("Submit Review")
                    }
                }
            }
        }
    }

    SuccessCheckmarkAnimation(
        isVisible = showSuccessAnim,
        onDismiss = {
            showSuccessAnim = false
            showPopup = false
            focusManager.clearFocus()
            wheelchairRating = 0
            blindnessRating = 0
            reviewText = ""
        }
    )
}

@Composable
fun StarRatingChooser(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.Center) {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$star star",
                modifier = Modifier.clickable { onRatingChange(star) }
            )
        }
    }
}

@Composable
fun SuccessCheckmarkAnimation(isVisible: Boolean, onDismiss: () -> Unit) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            val comp by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.checkmark_animation)
            )

            val progress by animateLottieCompositionAsState(
                composition = comp,
                isPlaying = true,
                iterations = 1,
            )

            LaunchedEffect(progress) {
                if (progress == 1f) {
                    onDismiss()
                }
            }

            Box {
                LottieAnimation(
                    composition = comp,
                    progress = { progress },
                    modifier = Modifier.size(100.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Explore(modifier: Modifier = Modifier, username: String? = null) {
    var fusedLocationClient = LocalActivity.current?.let { LocationServices.getFusedLocationProviderClient(it) }
    var cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
    var poisJson by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var selectedPoi by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var userLocation by remember {
        mutableStateOf<Geo?>(null)
    }

    LaunchedEffect(selectedFilter, searchText) {
        val client = OkHttpClient()
        val filter = if (selectedFilter == "All") "" else selectedFilter.lowercase()
        val request = Request.Builder()
            .url("https://accessimap.pythonanywhere.com/api/places?filter=$searchText&limit=75&category=$filter") // TODO: actually use search text
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("request FAILED")
                response.body.string()
            }
        }.onSuccess { json ->
            poisJson = json
        }.onFailure { e ->
            Log.e("Accessimap Explore Page", "/api/places FAILED")
        }
    }

    LaunchedEffect(Unit) {
        fusedLocationClient?.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )?.addOnSuccessListener { location ->
            if (location != null) {
                userLocation = Geo(
                    location.latitude,
                    location.longitude
                )
            }
        }
    }

    val pois = remember(poisJson) {
        try {
            val root = JSONObject(poisJson ?: "{}")
            val features = root.getJSONArray("features")
            List(features.length()) { i ->
                features.getJSONObject(i).getJSONObject("properties")
            }
        } catch (e: Exception) {
            emptyList<JSONObject>()
        }
    }

    val filteredPois = remember(pois, selectedFilter) {
        if (selectedFilter == "All") pois
        else pois.filter { poi ->
            poi.getString("amenity").replace("parking", "", ignoreCase = true).contains(selectedFilter.lowercase().dropLast(1), ignoreCase = true)
        }
    }

    Box(modifier = modifier.padding(20.dp)) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All", "Parks", "Cafes").forEach { category ->
                    Box(
                        modifier = Modifier
                            .border(
                                2.dp,
                                color = if (selectedFilter == category) Color.Black else Color.LightGray,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable { selectedFilter = category }
                    ) {
                        Text(category)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            filteredPois.forEach { poi ->
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
                        .clickable(
                            onClick = {
                                selectedPoi = poi.toString()
                                showBottomSheet = true
                            }
                        ),
                ) {
                    Column {
                        Text(poi.getString("name"), fontSize = 25.sp)
                        if (poi.getString("address") != "") {
                            Text(poi.getString("address"))
                        }

                        val placegeo = Geo(poi.getDouble("lat"), poi.getDouble("lon"))
                        val distance = userLocation?.haversine(placegeo)

                        Text(
                            distance?.let {
                                String.format("%.1f km away", it)
                            } ?: "Getting location..."
                        )

                        Text(
                            poi.getString("amenity").replace("_", " ").split(" ")
                                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
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
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val poi = JSONObject(selectedPoi)
                val poiId = poi["id"].toString()

                var blindnessAvg by remember { mutableStateOf(0.0) }
                var mobilityAvg by remember { mutableStateOf(0.0) }

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

                var reviews: JSONArray
                var totalBlindness = 0
                var totalMobility = 0
                var count = 0

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://accessimap.pythonanywhere.com/api/reviews/$poiId")
                    .build()
                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("ERROR requesting /api/reviews")

                        val jsonData = response.body.string()
                        val data = JSONTokener(jsonData).nextValue() as JSONArray
                        reviews = data

                        for (i in 0 until reviews.length()) {
                            val review = reviews.getJSONObject(i)
                            val blindnessRating = review["blindness_rating"] as Int
                            val wheelchairRating = review["wheelchair_rating"] as Int

                            totalBlindness += blindnessRating
                            totalMobility += wheelchairRating
                            count++
                        }
                    }
                } catch (e: Exception) {
                    Log.e("accessimap explore page", "failed to get reviews", e)
                    reviews = JSONArray()
                }

                if (count > 0) {
                    blindnessAvg = totalBlindness / count.toDouble()
                    mobilityAvg = totalMobility / count.toDouble()
                }

                val blindnessAvgString = String.format(Locale.CANADA, "%.1f", blindnessAvg)
                val mobilityAvgString = String.format(Locale.CANADA, "%.1f", mobilityAvg)

                Text("Blindness-friendly: $blindnessAvgString\nMobility: $mobilityAvgString")

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Button(
                        onClick = {
                            showPopup = true
                            showBottomSheet = false
                        },
                    ) {
                        Text("Write a review")
                    }

                }

                Spacer(modifier = Modifier.height(20.dp))

                for (i in 0 until reviews.length()) {
                    val review = reviews.getJSONObject(i)
                    val blindnessRating = review["blindness_rating"] as Int
                    val wheelchairRating = review["wheelchair_rating"] as Int
                    val username = review["username"] as String
                    val reviewText = review["review_text"] as String

                    Text("$username (Blind, Wheelchair Bound)", fontSize = 14.sp)
                    Text("$blindnessRating stars for blindness, $wheelchairRating stars for wheelchair-friendliness | Today", fontSize = 14.sp)
                    Text(reviewText)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    var showSuccessAnim by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(
        targetValue = if (showSuccessAnim) 15.dp else 0.dp,
        animationSpec = tween(2000)
    )

    var wheelchairRating by remember { mutableStateOf(0) }
    var blindnessRating by remember { mutableStateOf(0) }
    var reviewText by remember { mutableStateOf("") }

    if (showPopup) {
        Popup(
            alignment = Alignment.Center,
            onDismissRequest = { showPopup = false },
            properties = PopupProperties(focusable = true)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .padding(60.dp)
                    .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            ) {
                Column {
                    Text("Write your review:")
                    Spacer(modifier = Modifier.height(15.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text("Wheelchair: ")
                        StarRatingChooser(rating = wheelchairRating, onRatingChange = { wheelchairRating = it })
                    }
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text("Blindness: ")
                        StarRatingChooser(rating = blindnessRating, onRatingChange = { blindnessRating = it })
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                    TextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    Button(onClick = {
                        val poi = JSONObject(selectedPoi)
                        val poiId = poi["id"].toString()
                        val encodedReviewText = URLEncoder.encode(reviewText, StandardCharsets.UTF_8)

                        val client = OkHttpClient()
                        val request = Request.Builder()
                            .url(
                                "https://accessimap.pythonanywhere.com/api/review/submit?" +
                                        "wheelchair_rating=$wheelchairRating&" +
                                        "blindness_rating=$blindnessRating&" +
                                        "username=$username&" +
                                        "review_text=$encodedReviewText&" +
                                        "place_id=$poiId"
                            )
                            .build()
                        client.newCall(request).enqueue(object: Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                Log.e("accessimap explore page", "submitting review FAILED", e)
                            }

                            override fun onResponse(call: Call, response: Response) {
                                Log.d("accessimap explore page", "review submtited")
                            }
                        })

                        showSuccessAnim = true;
                    }) {
                        Text("Submit Review")
                    }
                }
            }
        }
    }

    SuccessCheckmarkAnimation(
        isVisible = showSuccessAnim,
        onDismiss = {
            showSuccessAnim = false
            showPopup = false
            focusManager.clearFocus()
            wheelchairRating = 0
            blindnessRating = 0
            reviewText = ""
        }
    )
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