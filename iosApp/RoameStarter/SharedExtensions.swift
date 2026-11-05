import CoreLocation
import Foundation
@preconcurrency import Shared

extension GeoPoint {
    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

/// DatePicker works with local midnight in the device's time zone; the shared conversions (tested
/// in `PickerDateTest`) map that instant to the flight's calendar day and back.
extension CalendarDate {
    var date: Date {
        Date(timeIntervalSince1970: Double(startOfDayMillis(timeZoneId: TimeZone.current.identifier)) / 1000)
    }

    static func from(_ date: Date) -> CalendarDate {
        CalendarDate.companion.fromEpochMillis(
            epochMillis: Int64((date.timeIntervalSince1970 * 1000).rounded(.down)),
            timeZoneId: TimeZone.current.identifier
        )
    }
}
