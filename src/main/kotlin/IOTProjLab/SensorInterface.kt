package com.botofan.IOTProjLab

interface SensorInterface<T> {
   fun onRead():T
   
   val bucket:ArrayDeque<T>
}