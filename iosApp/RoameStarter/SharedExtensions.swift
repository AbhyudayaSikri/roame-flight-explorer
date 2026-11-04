import CoreLocation
import Foundation
@preconcurrency import Shared

extension GeoPoint {
    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

extension CalendarDate {
    /// Midnight of this day in the user's calendar, which is what DatePicker works with.
    var date: Date {
        Calendar.current.date(from: DateComponents(year: Int(year), month: Int(month), day: Int(day))) ?? Date()
    }

    static func from(_ date: Date) -> CalendarDate {
        let parts = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return CalendarDate(year: Int32(parts.year ?? 1970), month: Int32(parts.month ?? 1), day: Int32(parts.day ?? 1))
    }
}
