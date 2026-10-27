package com.botofan

interface SensorInterface<T> {
   fun onRead():T
}