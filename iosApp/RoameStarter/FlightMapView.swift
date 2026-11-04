import MapKit
import SwiftUI
@preconcurrency import Shared

/// One annotation per destination airport (HND and NRT are separate); the selected flight is a
/// geodesic polyline, which MapKit draws correctly across the antimeridian.
struct FlightMapView: View {
    let model: ExplorerModel
    /// Height covered by the flight card, so the framed route stays above it.
    let bottomInset: CGFloat
    @State private var position: MapCameraPosition = .automatic

    var body: some View {
        let state = model.state
        Map(position: $position) {
            ForEach(state.markers, id: \.airport) { marker in
                Annotation(marker.city, coordinate: marker.point.coordinate) {
                    Button { model.store.focusDestination(airportCode: marker.airport) } label: {
                        MarkerPill(marker: marker)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("\(marker.airport) \(marker.label)")
                    .accessibilityIdentifier(TestIds.shared.marker(airport: marker.airport))
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
        .onChange(of: state.selected?.id, initial: true) {
            frame(model.state.selected)
        }
        .onChange(of: state.markers.map(\.airport)) {
            if model.state.selected == nil { frameMarkers() }
        }
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
        position = .rect(rect.insetBy(dx: -rect.width * 0.2, dy: -rect.height * 0.2))
    }

    /// `.automatic` zooms to street level around a single airport, so markers that all sit within
    /// a couple of degrees (one airport, or HND + NRT) get a regional view instead.
    private func frameMarkers() {
        let points = model.state.markers.map(\.point)
        guard let minLat = points.map(\.latitude).min(), let maxLat = points.map(\.latitude).max(),
              let minLon = points.map(\.longitude).min(), let maxLon = points.map(\.longitude).max(),
              maxLat - minLat < 2, maxLon - minLon < 2 else {
            position = .automatic
            return
        }
        position = .region(MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: (minLat + maxLat) / 2, longitude: (minLon + maxLon) / 2),
            span: MKCoordinateSpan(latitudeDelta: 2.5, longitudeDelta: 2.5)
        ))
    }
}

private struct MarkerPill: View {
    let marker: DestinationMarker

    var body: some View {
        HStack(spacing: 4) {
            Text(marker.airport).font(.caption2.weight(.bold))
            Text(marker.label).font(.caption2)
        }
        .foregroundStyle(.white)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Capsule().fill(marker.hasSaved ? Color.orange : Color.blue))
        .shadow(radius: 1, y: 1)
    }
}
