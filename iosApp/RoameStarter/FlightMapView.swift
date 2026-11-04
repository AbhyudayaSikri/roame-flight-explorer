import MapKit
import SwiftUI
@preconcurrency import Shared

/// One annotation per destination airport (HND and NRT are separate); the selected flight is a
/// geodesic polyline, which MapKit draws correctly across the antimeridian.
struct FlightMapView: View {
    let model: ExplorerModel
    @State private var position: MapCameraPosition = .region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 25, longitude: 15),
            span: MKCoordinateSpan(latitudeDelta: 140, longitudeDelta: 300)
        )
    )

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
