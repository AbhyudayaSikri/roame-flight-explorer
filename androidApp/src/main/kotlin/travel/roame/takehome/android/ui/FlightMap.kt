package travel.roame.takehome.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import travel.roame.takehome.DestinationMarker
import travel.roame.takehome.GeoPoint
import travel.roame.takehome.MarkerCluster
import travel.roame.takehome.clusterMarkers
import travel.roame.takehome.fitCamera
import kotlin.math.max
import kotlin.math.pow

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val MARKER_SOURCE = "destination-markers"
private const val MARKER_LAYER = "destination-markers-layer"
private const val ROUTE_SOURCE = "selected-route"
private const val ROUTE_LAYER = "selected-route-layer"
private const val INITIAL_ZOOM = 0.6

/** A style with nothing to download: just a sea-coloured background for the pills and route. */
private const val OFFLINE_STYLE =
    """{"version":8,"name":"offline","sources":{},"layers":[{"id":"background","type":"background","paint":{"background-color":"#D6E4EF"}}]}"""

/** MapLibre's world is 512 dp wide at zoom 0. */
private const val WORLD_DP_AT_ZOOM_0 = 512.0

/** About the size of the widest single-airport pill ("HND  55 · 16.5k"). */
private const val PILL_WIDTH_DP = 130.0
private const val PILL_HEIGHT_DP = 28.0

/**
 * MapLibre in an AndroidView. Pills are bitmaps, so the labels need no font download. The shared
 * `clusterMarkers` merges pills that would overlap at the current zoom; tapping a merged pill
 * zooms in until its airports separate (HND and NRT do once you're over Japan). The view follows
 * the screen's lifecycle and is destroyed when it leaves the composition. A newly selected route
 * is framed above the card covering the bottom [bottomInset] of the map.
 */
@Composable
fun FlightMap(
    markers: List<DestinationMarker>,
    route: List<GeoPoint>,
    onMarkerTap: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnMarkerTap by rememberUpdatedState(onMarkerTap)
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { it.onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var zoom by remember { mutableDoubleStateOf(INITIAL_ZOOM) }
    val pills = remember(markers, zoom) {
        clusterMarkers(markers, worldWidth = WORLD_DP_AT_ZOOM_0 * 2.0.pow(zoom), pillWidth = PILL_WIDTH_DP, pillHeight = PILL_HEIGHT_DP)
    }
    val currentPills by rememberUpdatedState(pills)
    val markerImages = remember { mutableSetOf<String>() }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    DisposableEffect(mapView) {
        // Offline, or with the tile server down, the style itself can't load, and without a style
        // nothing is drawn, not even our pills (QA O8). Fall back to a plain background then.
        var fellBack = false
        val onStyleFailed = MapView.OnDidFailLoadingMapListener {
            val loadedMap = map
            if (style == null && loadedMap != null && !fellBack) {
                fellBack = true
                loadedMap.setStyle(Style.Builder().fromJson(OFFLINE_STYLE)) { loaded ->
                    addFlightLayers(loaded)
                    style = loaded
                }
            }
        }
        mapView.addOnDidFailLoadingMapListener(onStyleFailed)
        mapView.getMapAsync { loadedMap ->
            map = loadedMap
            loadedMap.cameraPosition = CameraPosition.Builder().target(LatLng(25.0, 15.0)).zoom(INITIAL_ZOOM).build()
            loadedMap.uiSettings.isRotateGesturesEnabled = false
            loadedMap.addOnCameraIdleListener { zoom = loadedMap.cameraPosition.zoom }
            loadedMap.setStyle(Style.Builder().fromUri(STYLE_URL)) { loaded ->
                addFlightLayers(loaded)
                style = loaded
            }
            loadedMap.addOnMapClickListener { point ->
                val screen = loadedMap.projection.toScreenLocation(point)
                val id = loadedMap.queryRenderedFeatures(screen, MARKER_LAYER).firstOrNull()?.getStringProperty("pill")
                val pill = currentPills.firstOrNull { it.id == id } ?: return@addOnMapClickListener false
                if (pill.isSingle) currentOnMarkerTap(pill.id) else zoomInto(loadedMap, mapView, pill)
                true
            }
        }
        onDispose {
            mapView.removeOnDidFailLoadingMapListener(onStyleFailed)
            style = null
            map = null
        }
    }

    LaunchedEffect(style, pills) {
        style?.let { showPills(context, it, pills, markerImages) }
    }
    LaunchedEffect(style, route) {
        style?.let { showRoute(it, route) }
    }
    LaunchedEffect(style, route, bottomInset) {
        val loadedMap = map
        if (style != null && loadedMap != null) frameRoute(loadedMap, mapView, route, bottomInset)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** Animates to the shared [fitCamera] for [route]; nothing to frame until a flight is selected. */
private fun frameRoute(map: MapLibreMap, view: MapView, route: List<GeoPoint>, bottomInset: Dp) {
    if (route.size < 2 || view.width == 0 || view.height == 0) return
    val density = view.resources.displayMetrics.density.toDouble()
    val fit = fitCamera(
        points = route,
        viewWidth = view.width / density,
        viewHeight = view.height / density,
        bottomInset = bottomInset.value.toDouble(),
    ) ?: return
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fit.center.latitude, fit.center.longitude), fit.zoom), 600)
}

/** The selected route under the pills; both use only our own data, so they work offline. */
private fun addFlightLayers(style: Style) {
    style.addSource(GeoJsonSource(ROUTE_SOURCE))
    style.addLayer(
        LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
            PropertyFactory.lineColor("#B3261E"),
            PropertyFactory.lineWidth(3f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        ),
    )
    style.addSource(GeoJsonSource(MARKER_SOURCE))
    style.addLayer(
        SymbolLayer(MARKER_LAYER, MARKER_SOURCE).withProperties(
            PropertyFactory.iconImage(Expression.get("icon")),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
        ),
    )
}

/** Frames a merged pill's airports, at least one zoom level closer than now so they split. */
private fun zoomInto(map: MapLibreMap, view: MapView, pill: MarkerCluster) {
    if (view.width == 0 || view.height == 0) return
    val density = view.resources.displayMetrics.density.toDouble()
    val fit = fitCamera(
        points = pill.markers.map { it.point },
        viewWidth = view.width / density,
        viewHeight = view.height / density,
        padding = 64.0,
    ) ?: return
    val zoom = max(fit.zoom, map.cameraPosition.zoom + 1)
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fit.center.latitude, fit.center.longitude), zoom), 600)
}

private fun showPills(context: Context, style: Style, pills: List<MarkerCluster>, images: MutableSet<String>) {
    val wanted = pills.associateBy { imageId(it) }
    (images - wanted.keys).forEach { style.removeImage(it) }
    wanted.forEach { (id, pill) -> if (id !in images) style.addImage(id, pillBitmap(context, pill)) }
    images.clear()
    images.addAll(wanted.keys)

    val features = pills.map { pill ->
        Feature.fromGeometry(Point.fromLngLat(pill.center.longitude, pill.center.latitude)).apply {
            addStringProperty("pill", pill.id)
            addStringProperty("icon", imageId(pill))
        }
    }
    style.getSourceAs<GeoJsonSource>(MARKER_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun showRoute(style: Style, route: List<GeoPoint>) {
    val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE) ?: return
    if (route.size < 2) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    } else {
        source.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(route.map { Point.fromLngLat(it.longitude, it.latitude) })))
    }
}

private fun imageId(pill: MarkerCluster) = "pill-${pill.title}-${pill.label}-${pill.hasSaved}"

/**
 * "HND  55 · 16.5k" for one airport, "6 airports  280 · 13k" for a merged pill. Saved
 * destinations and merged pills get their own colours.
 */
private fun pillBitmap(context: Context, pill: MarkerCluster): Bitmap {
    val density = context.resources.displayMetrics.density
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 13 * density
        typeface = Typeface.DEFAULT_BOLD
    }
    val label = "${pill.title}  ${pill.label}"
    val padX = 9 * density
    val padY = 5 * density
    val width = (text.measureText(label) + padX * 2).toInt()
    val height = (text.fontSpacing + padY * 2).toInt()
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            pill.hasSaved -> 0xFFB3541E.toInt()
            pill.isSingle -> 0xFF1B4F8A.toInt()
            else -> 0xFF3F3D8F.toInt()
        }
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), height / 2f, height / 2f, background)
        canvas.drawText(label, padX, padY - text.ascent(), text)
    }
}
