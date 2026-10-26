package com.botofan

import com.pi4j.Pi4J
import com.pi4j.context.Context
import com.pi4j.io.gpio.analog.Analog
import com.pi4j.io.gpio.analog.AnalogInput
import com.pi4j.io.gpio.analog.AnalogInputConfigBuilder
import com.pi4j.io.gpio.analog.AnalogInputProvider
import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalInputConfig
import com.pi4j.io.gpio.digital.DigitalInputConfigBuilder
import com.pi4j.io.gpio.digital.DigitalInputProvider
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder
import com.pi4j.io.gpio.digital.DigitalOutputProvider
import com.pi4j.io.gpio.digital.DigitalState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import com.pi4j.io.gpio.digital.DigitalState.LOW
import com.pi4j.io.gpio.digital.DigitalState.HIGH
import com.pi4j.plugin.ffm.api.Pi4JApi
import com.pi4j.util.Console

lateinit var pi: Context
lateinit var console: Console

lateinit var led: DigitalOutput
lateinit var sensor1: AnalogInput
lateinit var sensor2: AnalogInput
fun DigitalInput.read(): Boolean{
   return this.isHigh
}
fun AnalogInput.read(): Int{
   return this.value()
}

val bucketSize: Int = 10
lateinit var bucket1: ArrayDeque<Int>
lateinit var bucket2: ArrayDeque<Int>
fun ArrayDeque<Int>.isFull(): Boolean {
   return this.size >= 10
}
fun ArrayDeque<Int>.empty(): Array<Int> {
   val array: Array<Int> = this.toTypedArray()
   this.clear()
   return array
}

val historySize:Int = 500
lateinit var bucketHistory1: ArrayDeque<Double>
lateinit var bucketHistory2: ArrayDeque<Double>
/**
 * Potentially add ignorance for outliers of magnitude 1.5 or 2.0 of the average value
 * */
fun ArrayDeque<Int>.addToHistory(history: ArrayDeque<Double>){
   
   if(history.size>=historySize && historySize>0) {
      while(history.size>= historySize) {
         history.removeLastOrNull()
      }
   }
   
   this.clear()
}


val scope = CoroutineScope(Dispatchers.Default)

fun main() {
   
   try {
      init()
      
      //println("\nUsing provider: ${led.provider()}\n")
      
      runBlocking {
      
         receiveInputs()
         
         proccessInputs()
         
      }
      
   } catch (e: Throwable) {
      e.printStackTrace()
   } finally {
      pi.shutdown()
   }
}

/**
 * Setup pins and initialize objects. And buckets
 * */
fun init() {
   pi = Injector.getPi4Context()
   console = Injector.getPi4Console()
   
   // ################## Pins and info #####################
   val LED_PIN = 27
   
   val SENSOR1_PIN = -1
   val SENSOR1_NAME = "Sensor1"
   val SENSOR1_ID = "PI_WIRED_SENSOR_2"
   
   val SENSOR2_PIN = -1
   val SENSOR2_NAME = "Sensor2"
   val SENSOR2_ID = "PI_WIRED_SENSOR_2"
   
   when{
      LED_PIN < 0 -> throw InvalidPinException(LED_PIN,"Change LED pin!")
      SENSOR1_PIN < 0 -> throw InvalidPinException(SENSOR1_PIN,"Change sensor 1 pin!")
      SENSOR2_PIN < 0 -> throw InvalidPinException(SENSOR2_PIN,"Change sensor 2 pin!")
   }
   
   // ########################## Set outputs ###########################
   val ledConfig = DigitalOutputConfigBuilder.newInstance(pi)
      .bcm(LED_PIN)
      .initial(LOW)
      .shutdown(LOW)
      .build()
   led = pi.digitalOutput<DigitalOutputProvider>().create(ledConfig)
   
   // ########################### Set Inputs ##################################
   val sensor1Config = AnalogInputConfigBuilder.newInstance(pi)
      .bcm(SENSOR1_PIN)
      .id(SENSOR1_ID)
      .name(SENSOR1_NAME)
      .build()
   sensor1 = pi.analogInput<AnalogInputProvider>().create(sensor1Config)
   
   val sensor2Config = AnalogInputConfigBuilder.newInstance(pi)
      .bcm(SENSOR2_PIN)
      .id(SENSOR2_ID)
      .name(SENSOR2_NAME)
      .build()
   sensor2 = pi.analogInput<AnalogInputProvider>().create(sensor2Config)
}
/**
 * Receive inputs.
 * ## Requirements
 * * Read new inputs
 * * Store new inputs in a bucket
 * * Coagulate full bucket
 * */
fun receiveInputs(){
   val s1 = sensor1.read()
   if(bucket1.isFull()){
      bucket1.addToHistory(bucketHistory1)
   }
   bucket1.add(s1)
   
   
   val s2 = sensor2.read()
   if(bucket2.isFull()){
      bucket2.addToHistory(bucketHistory2)
   }
   bucket2.add(s2)
}
/**
 * Proccess new received inputs.
 *
 * ## Requirements
 * * Proccess current bucket of inputs
 * */
fun proccessInputs(){

}
/**
 * ## Requirements:
 * * Receive client request for a socket connection and create it if there is a request that doesn't match an existing socket.
 * */
fun receiveAPIrequest(){

}
/**
 * ## Requirements:
 * * Send an api update through a socket.
 * */
fun sendAPIupdate(){

}
/**
 * ## Requirements:
 * * Verify if sockets are still active and if not, deactivate them
 * */
fun verifyAPIsockets(){
   // make this affect a socket instead of this empty code
   sendAPIupdate()
}


