import MapKit
import SwiftUI
@preconcurrency import Shared

/// One pill per destination airport (HND and NRT are separate), merged by the shared
/// `clusterMarkers` where pills would overlap at the current zoom. The selected flight is a
/// geodesic polyline, which MapKit draws correctly across the antimeridian.
struct FlightMapView: View {
    let model: ExplorerModel
    /// Height covered by the flight card, so the framed route stays above it.
    let bottomInset: CGFloat
    @State private var position: MapCameraPosition = .automatic
    /// The whole world's width in points at the current zoom; 0 (merge nothing) until it's known.
    /// Set straight away when this view moves the camera, and after every gesture.
    @State private var worldWidth: Double = 0
    @State private var viewSize: CGSize = .zero

    var body: some View {
        let state = model.state
        let pills = MarkerClusteringKt.clusterMarkers(
            markers: state.markers, worldWidth: worldWidth, pillWidth: 104, pillHeight: 24
        )
        GeometryReader { geometry in
            Map(position: $position) {
                ForEach(pills, id: \.id) { pill in
                    Annotation(pill.title, coordinate: pill.center.coordinate) {
                        Button { tap(pill) } label: {
                            MarkerPill(pill: pill)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("\(pill.title) \(pill.label)")
                        .accessibilityValue(pill.isSingle ? "" : pill.markers.map(\.airport).joined(separator: " "))
                        .accessibilityIdentifier(
                            pill.isSingle ? TestIds.shared.marker(airport: pill.id) : TestIds.shared.markerCluster(id: pill.id)
                        )
                    }
                    .annotationTitles(.hidden)
                }
                if let flight = state.selected {
                    MapPolyline(
                        coordinates: [flight.originPoint.coordinate, flight.destinationPoint.coordinate],
                        contourStyle: .geodesic
                    )
                    .stroke(Color.red, lineWidth: 3)
                }
            }
            .mapStyle(.standard(pointsOfInterest: .excludingAll))
            .safeAreaPadding(.bottom, bottomInset)
            .onMapCameraChange(frequency: .onEnd) { context in
                worldWidth = geometry.size.width * MKMapSize.world.width / context.rect.size.width
            }
            // The keyboard or a rotation resizes the map. Frame again for the new shape: a frame
            // chosen while the keyboard was up would stay zoomed out once it's gone.
            .onChange(of: geometry.size, initial: true) { _, size in
                viewSize = size
                frame(model.state.selected)
            }
            .onChange(of: state.selected?.id, initial: true) {
                frame(model.state.selected)
            }
            .onChange(of: state.markers.map(\.airport)) {
                if model.state.selected == nil { frameMarkers() }
            }
        }
    }

    /// A destination opens its flights; a cluster zooms in until its airports separate.
    private func tap(_ pill: MarkerCluster) {
        if pill.isSingle {
            model.store.focusDestination(airportCode: pill.id)
            return
        }
        let rect = pill.markers
            .map { MKMapRect(origin: MKMapPoint($0.point.coordinate), size: MKMapSize(width: 0, height: 0)) }
            .reduce(MKMapRect.null) { $0.union($1) }
        let margin = max(max(rect.width, rect.height) * 0.4, 100_000)
        show(rect.insetBy(dx: -margin, dy: -margin), animated: true)
    }

    /// Moves the camera and re-clusters for the new zoom at once. MapKit shows at least [rect],
    /// widened to the view's shape.
    private func show(_ rect: MKMapRect, animated: Bool = false) {
        // Setting an equal position doesn't move the camera, so fitting the same area again (after
        // the map is resized) goes through the equivalent region, which MapKit fits afresh.
        let target: MapCameraPosition = position == .rect(rect) ? .region(MKCoordinateRegion(rect)) : .rect(rect)
        if animated {
            withAnimation { position = target }
        } else {
            position = target
        }
        guard viewSize.width > 0, viewSize.height > 0, rect.size.width > 0 else { return }
        let visibleWidth = max(rect.size.width, rect.size.height * viewSize.width / viewSize.height)
        worldWidth = viewSize.width * MKMapSize.world.width / visibleWidth
    }

    /// Fits the selected route, or every marker when nothing is selected.
    private func frame(_ flight: Flight?) {
        guard let flight else {
            frameMarkers()
            return
        }
        let route = MKGeodesicPolyline(
            coordinates: [flight.originPoint.coordinate, flight.destinationPoint.coordinate],
            count: 2
        )
        let rect = route.boundingMapRect
        show(rect.insetBy(dx: -rect.width * 0.2, dy: -rect.height * 0.2))
    }

    /// `.automatic` zooms to street level around a single airport, so markers that all sit within
    /// a couple of degrees (one airport, or HND + NRT) get a 2.5° square instead.
    private func frameMarkers() {
        let points = model.state.markers.map(\.point)
        guard let minLat = points.map(\.latitude).min(), let maxLat = points.map(\.latitude).max(),
              let minLon = points.map(\.longitude).min(), let maxLon = points.map(\.longitude).max(),
              maxLat - minLat < 2, maxLon - minLon < 2 else {
            position = .automatic
            return
        }
        let half = 1.25
        let midLat = (minLat + maxLat) / 2
        let midLon = (minLon + maxLon) / 2
        let topLeft = MKMapPoint(CLLocationCoordinate2D(latitude: midLat + half, longitude: midLon - half))
        let bottomRight = MKMapPoint(CLLocationCoordinate2D(latitude: midLat - half, longitude: midLon + half))
        show(MKMapRect(x: topLeft.x, y: topLeft.y, width: bottomRight.x - topLeft.x, height: bottomRight.y - topLeft.y))
    }
}

private struct MarkerPill: View {
    let pill: MarkerCluster

    var body: some View {
        HStack(spacing: 4) {
            Text(pill.title).font(.caption2.weight(.bold))
            Text(pill.label).font(.caption2)
        }
        .foregroundStyle(.white)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Capsule().fill(pill.hasSaved ? Color.orange : (pill.isSingle ? Color.blue : Color.indigo)))
        .shadow(radius: 1, y: 1)
    }
}
