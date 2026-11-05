package com.botofan

data class DataPoint(val timestamp:Long,val value: Double){
   override fun toString(): String {
      return value.toString()
   }
}