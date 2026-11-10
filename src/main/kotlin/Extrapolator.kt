package com.botofan

import javax.xml.crypto.Data
import kotlin.math.abs
import kotlin.math.max

object Extrapolator {
   
   fun ArrayDeque<Double>.lastNAverage_(n: Int): Double {
      require(isNotEmpty()) { "empty deque" }
      val k = minOf(n, size)
      var sum = 0.0
      // ArrayDeque iterates from head; takeLast/iterator order — safer to use takeLast
      for (v in this.takeLast(k)) sum += v
      return sum / k
   }
   fun List<DataPoint>.lastNAverage(n: Int): Double {
      require(isNotEmpty()) { "empty deque" }
      val k = minOf(n, size)
      var sum = 0.0
      // ArrayDeque iterates from head; takeLast/iterator order — safer to use takeLast
      for (v in this.takeLast(k)) sum += v.value
      return sum / k
   }
   /**
    * Get the index of the bad sensor
    * */
   fun getBadSensorIndex(
      sensors: List<SensorInterface<DataPoint>>,
      sensorLeeway: Double,
      recentN: Int
   ): Int? {
      if (sensors.size < 3) return null
      if (sensors.any { it.bucket.isEmpty() }) return null
      
      val avgs = sensors.map { it.bucket.lastNAverage(recentN) }  // no .sorted()
      
      if (avgs.max() - avgs.min() <= sensorLeeway) return null
      
      var worstIndex = -1
      var worstDist = -1.0
      for (i in avgs.indices) {
         val others = avgs.filterIndexed { j, _ -> j != i }
         val dist = abs(avgs[i] - others.average())
         if (dist > worstDist) {
            worstDist = dist
            worstIndex = i
         }
      }
      return worstIndex
   }
   
   fun processDegradationCurve(
      bucket: ArrayDeque<DataPoint>,
      t_min_distance: Long = 1000,
      max_window_size: Int,
      maxPoints: Int
   ): List<DataPoint> {
      if(bucket.size<3){
         return emptyList()
      }
      
      val win = bucket.toList().takeLast(max_window_size)
      val deltaT = maxOf(t_min_distance, bucket[1].timestamp - bucket.first().timestamp) // just the average T between points
      
      // map the window taken into consideration to a collection of increases and decreases then average them for a steady increase/decrease (best method i could think of)
      val avgStep = win.zipWithNext { a, b -> b.value - a.value }.average()
      
      val lastT = bucket.last().timestamp
      val lastV = bucket.last().value
      
      return (1..maxPoints).map { k ->
         DataPoint(
            lastT + k * deltaT,
            lastV + k * avgStep // i*1 magnitude change * average change size
         )
      }
   }
}