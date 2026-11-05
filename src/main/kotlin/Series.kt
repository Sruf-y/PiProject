package com.botofan

import kotlinx.serialization.Serializable

@Serializable
data class Series(
   val sensor: Int,
   val isBad: Boolean = false,
   val measured: List<DataPoint> = emptyList(),
   val extrapolated: List<DataPoint> = emptyList()
)