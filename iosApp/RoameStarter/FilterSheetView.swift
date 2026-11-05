import SwiftUI
@preconcurrency import Shared

struct FilterSheetView: View {
    let model: ExplorerModel
    let onReset: () -> Void
    @Environment(\.dismiss) private var dismiss
    /// The thumb's value from the start of a drag until the store catches up. The store's value
    /// arrives a moment after each step, and following it mid-drag would pull the thumb back.
    @State private var draggedPoints: Double?
    @State private var isDragging = false

    private var filters: FiltersUi { model.filters }
    private var store: FlightExplorerStore { model.store }

    var body: some View {
        NavigationStack {
            Form {
                if let bounds = filters.bounds {
                    let range = filters.dateRange ?? bounds.dateRange
                    let allowed = bounds.firstDate.date...bounds.lastDate.date
                    Section("Departure dates") {
                        DatePicker("From", selection: dateBinding(start: true, range: range), in: allowed, displayedComponents: .date)
                            .accessibilityIdentifier(TestIds.shared.filterDateStart)
                        DatePicker("To", selection: dateBinding(start: false, range: range), in: allowed, displayedComponents: .date)
                            .accessibilityIdentifier(TestIds.shared.filterDateEnd)
                    }
                    // A row per cabin rather than a segmented control, which cut "Premium Economy" short.
                    Section("Cabin") {
                        ForEach([String?.none] + bounds.cabins.map { String?.some($0) }, id: \.self) { cabin in
                            let selected = filters.cabin == cabin
                            Button { store.setCabin(cabin: cabin) } label: {
                                HStack {
                                    Text(cabin ?? "Any").foregroundStyle(.primary)
                                    Spacer()
                                    if selected {
                                        Image(systemName: "checkmark").foregroundStyle(Color.accentColor)
                                    }
                                }
                                .contentShape(Rectangle())
                            }
                            .tint(.primary)
                            .accessibilityAddTraits(selected ? .isSelected : [])
                            .accessibilityIdentifier(TestIds.shared.cabinOption(cabin: cabin))
                        }
                    }
                    Section("Maximum points") {
                        let points = Int(draggedPoints ?? Double(filters.maxPoints))
                        Text(verbatim: points >= Int(bounds.maxPoints) ? "No limit" : "Up to \(FormattingKt.formatPoints(points: Int32(points)))")
                            .accessibilityIdentifier(TestIds.shared.filterPointsValue)
                        Slider(
                            value: pointsBinding,
                            in: Double(bounds.minPoints)...Double(bounds.maxPoints),
                            step: Double(filters.pointsStep),
                            onEditingChanged: { editing in
                                isDragging = editing
                                if !editing, let dragged = draggedPoints, Int32(dragged.rounded()) == model.filters.maxPoints {
                                    draggedPoints = nil
                                }
                            }
                        )
                        .accessibilityIdentifier(TestIds.shared.filterPoints)
                        // After the drag, the store is the source again: its next value, or Reset.
                        .onChange(of: model.filters.maxPoints) {
                            if !isDragging { draggedPoints = nil }
                        }
                    }
                    Section {
                        ForEach(bounds.programs, id: \.self) { program in
                            Toggle(program, isOn: programBinding(program))
                                .accessibilityIdentifier(TestIds.shared.programOption(program: program))
                        }
                    } header: {
                        Text("Loyalty programs")
                    } footer: {
                        Text(filters.programs.isEmpty ? "Any program" : "Flights from any selected program")
                    }
                }
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Reset all", action: onReset)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button { dismiss() } label: {
                        Text(verbatim: "Show \(FormattingKt.formatFlightCount(count: model.resultCount))")
                    }
                    .accessibilityIdentifier(TestIds.shared.filtersDone)
                }
            }
        }
    }

    private func dateBinding(start: Bool, range: DateRange) -> Binding<Date> {
        Binding(
            get: { (start ? range.start : range.end).date },
            set: { picked in
                let day = CalendarDate.from(picked)
                if start {
                    store.setDateRange(start: day, end: range.end)
                } else {
                    store.setDateRange(start: range.start, end: day)
                }
            }
        )
    }

    private var pointsBinding: Binding<Double> {
        Binding(
            get: { draggedPoints ?? Double(model.filters.maxPoints) },
            set: {
                draggedPoints = $0
                store.setMaxPoints(points: Int32($0.rounded()))
            }
        )
    }

    private func programBinding(_ program: String) -> Binding<Bool> {
        Binding(
            get: { model.filters.programs.contains(program) },
            set: { _ in store.toggleProgram(program: program) }
        )
    }
}
