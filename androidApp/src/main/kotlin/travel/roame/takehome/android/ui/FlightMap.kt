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
import travel.roame.takehome.fitCamera

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val MARKER_SOURCE = "destination-markers"
private const val MARKER_LAYER = "destination-markers-layer"
private const val ROUTE_SOURCE = "selected-route"
private const val ROUTE_LAYER = "selected-route-layer"

/**
 * MapLibre in an AndroidView. Markers are bitmaps with overlap allowed, so HND and NRT are both
 * always shown and the labels need no font download. The view follows the screen's lifecycle and
 * is destroyed when it leaves the composition. A newly selected route is framed above the card
 * covering the bottom [bottomInset] of the map.
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
        mapView.getMapAsync { loadedMap ->
            map = loadedMap
            loadedMap.cameraPosition = CameraPosition.Builder().target(LatLng(25.0, 15.0)).zoom(0.6).build()
            loadedMap.uiSettings.isRotateGesturesEnabled = false
            loadedMap.setStyle(Style.Builder().fromUri(STYLE_URL)) { loaded ->
                loaded.addSource(GeoJsonSource(ROUTE_SOURCE))
                loaded.addLayer(
                    LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                        PropertyFactory.lineColor("#B3261E"),
                        PropertyFactory.lineWidth(3f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    ),
                )
                loaded.addSource(GeoJsonSource(MARKER_SOURCE))
                loaded.addLayer(
                    SymbolLayer(MARKER_LAYER, MARKER_SOURCE).withProperties(
                        PropertyFactory.iconImage(Expression.get("icon")),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                    ),
                )
                style = loaded
            }
            loadedMap.addOnMapClickListener { point ->
                val screen = loadedMap.projection.toScreenLocation(point)
                val airport = loadedMap.queryRenderedFeatures(screen, MARKER_LAYER).firstOrNull()?.getStringProperty("airport")
                if (airport != null) currentOnMarkerTap(airport)
                airport != null
            }
        }
        onDispose {
            style = null
            map = null
        }
    }

    LaunchedEffect(style, markers) {
        style?.let { showMarkers(context, it, markers, markerImages) }
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

private fun showMarkers(context: Context, style: Style, markers: List<DestinationMarker>, images: MutableSet<String>) {
    val wanted = markers.associateBy { imageId(it) }
    (images - wanted.keys).forEach { style.removeImage(it) }
    wanted.forEach { (id, marker) -> if (id !in images) style.addImage(id, markerBitmap(context, marker)) }
    images.clear()
    images.addAll(wanted.keys)

    val features = markers.map { marker ->
        Feature.fromGeometry(Point.fromLngLat(marker.point.longitude, marker.point.latitude)).apply {
            addStringProperty("airport", marker.airport)
            addStringProperty("icon", imageId(marker))
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

private fun imageId(marker: DestinationMarker) = "marker-${marker.airport}-${marker.label}-${marker.hasSaved}"

/** A pill with "count · lowest points"; saved destinations get a different colour. */
private fun markerBitmap(context: Context, marker: DestinationMarker): Bitmap {
    val density = context.resources.displayMetrics.density
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 13 * density
        typeface = Typeface.DEFAULT_BOLD
    }
    val label = "${marker.airport}  ${marker.label}"
    val padX = 9 * density
    val padY = 5 * density
    val width = (text.measureText(label) + padX * 2).toInt()
    val height = (text.fontSpacing + padY * 2).toInt()
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (marker.hasSaved) 0xFFB3541E.toInt() else 0xFF1B4F8A.toInt()
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), height / 2f, height / 2f, background)
        canvas.drawText(label, padX, padY - text.ascent(), text)
    }
}
