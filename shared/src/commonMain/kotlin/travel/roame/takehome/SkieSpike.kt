package travel.roame.takehome

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Temporary: replaced by FlightExplorerStore in PLAN step 6.
sealed interface SpikeStatus {
    data object Idle : SpikeStatus
    data class Ticked(val count: Int) : SpikeStatus
}

class SkieSpike {
    private val _message = MutableStateFlow(Greeting().message())
    val message: StateFlow<String> = _message.asStateFlow()

    private val _status = MutableStateFlow<SpikeStatus>(SpikeStatus.Idle)
    val status: StateFlow<SpikeStatus> = _status.asStateFlow()

    fun tick() {
        val count = when (val current = _status.value) {
            SpikeStatus.Idle -> 1
            is SpikeStatus.Ticked -> current.count + 1
        }
        _status.value = SpikeStatus.Ticked(count)
        _message.value = "${Greeting().message()} (ticks: $count)"
    }
}
