package com.botofan

import kotlin.math.abs
import kotlin.math.max

object Extrapolator {
   
   fun ArrayDeque<Double>.lastNAverage(n: Int): Double {
      require(isNotEmpty()) { "empty deque" }
      val k = minOf(n, size)
      var sum = 0.0
      // ArrayDeque iterates from head; takeLast/iterator order — safer to use takeLast
      for (v in this.takeLast(k)) sum += v
      return sum / k
   }
   /**
    * Get the index of the bad sensor
    * */
   fun getBadSensorIndex(
      sensors: List<SensorInterface<Double>>,
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
   
   fun isUnusable(
      avgs: List<Double>,
      i: Int,
      sensorLeeway: Double,
      maxDeviation: Double
   ): Boolean {
      val others = avgs.filterIndexed { j, _ -> j != i }
      val othersAgree = (others.max() - others.min()) <= sensorLeeway
      if (!othersAgree) return false          // can't judge — the "good" pair isn't consistent
      val consensus = others.average()
      return abs(avgs[i] - consensus) > maxDeviation
   }
   
}