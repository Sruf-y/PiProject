package com.botofan.IOTProjLab

import kotlinx.serialization.Serializable

@Serializable
data class DataPoint(val timestamp:Long,val value: Double)