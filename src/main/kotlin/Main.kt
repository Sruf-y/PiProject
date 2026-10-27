package com.botofan

import com.pi4j.context.Context
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder
import com.pi4j.io.gpio.digital.DigitalOutputProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.pi4j.io.gpio.digital.DigitalState.LOW
import com.pi4j.util.Console
import kotlinx.coroutines.delay
import java.util.Locale

lateinit var pi: Context
lateinit var console: Console

lateinit var led: DigitalOutput
lateinit var sensor1: SensorInterface<Double>
lateinit var sensor2: SensorInterface<Double>

var dataFormat: String = "%.2f"
var delayTime: Long = 500

var bucketSize: Int = 5
lateinit var bucket1: ArrayDeque<Double>
lateinit var bucket2: ArrayDeque<Double>
fun ArrayDeque<Double>.isFull(): Boolean {
   return this.size >= bucketSize
}

fun ArrayDeque<Double>.empty(): Array<Double> {
   val array = this.toTypedArray()
   this.clear()
   return array
}

var historySize: Int = 20
lateinit var bucketHistory1: ArrayDeque<Double>
lateinit var bucketHistory2: ArrayDeque<Double>

/**
 * Potentially add ignorance for outliers of magnitude 1.5 or 2.0 of the average value
 * */
fun ArrayDeque<Double>.addToHistory(history: ArrayDeque<Double>) {
   
   if (history.size >= historySize && historySize >= 0) {
      while (history.size >= historySize) {
         history.removeLastOrNull()
      }
   }
   history.add(this.average())
   
   this.clear()
}


val scope = CoroutineScope(Dispatchers.Default)

fun displayArgs() {
   println(
      """
      =====================================================
      Arguments:
      -h,-help : show help
      -bucketSize,-bs : set the bucketing size, default is ${bucketSize}
      -historySize,-hs : set the history max size, default is ${historySize}
      -format : set the format for the readings. Ex: %1.f or %.f or %2.2f. Default is ${dataFormat}
      -delay : set the loop delay. Default is ${delayTime}
      =====================================================
   """.trimIndent()
   )
}

fun ArrayDeque<Double>.format(format:String): String{
   return "["+this.joinToString(", ") { String.format(format,it) }+"]"
}

fun main(args: Array<String>) {
   
   if (args.isNotEmpty()) {
      if (args.first() == "-h" || args.first() == "-help") {
         displayArgs()
         return
      }
      var argRet = -1
      
      fun tryRet(vararg names: String, onFound: (index: Int, name: String) -> Unit): Boolean {
         for (name in names) {
            val i = args.indexOf(name)
            if (i != -1) {
               onFound(i, name)
               return true
            }
         }
         return false
      }
      
      
      tryRet("-bucketSize","-bs"){index,_->
         try {
            bucketSize = args[index + 1].toInt()
         } catch (ex: Exception) {
            throw Exception("Expected integer after \"-bucketSize\"")
         }
      }
      tryRet("-historySize","-hs"){index,_->
         try {
            historySize = args[index + 1].toInt()
         } catch (ex: Exception) {
            throw Exception("Expected integer after \"-historySize\"")
         }
      }
      tryRet("-format"){index,_->
         try {
            val newformat = args[index + 1]
            String.format(newformat, 1.0) // try to see if the format is good or not
            dataFormat = newformat
            
         } catch (ex: Exception) {
            throw Exception("Invalid format provided after \"-format\"")
         }
      }
      tryRet("-delayTime"){index,_->
         try {
            delayTime = args[index + 1].toLong()
         } catch (ex: Exception) {
            throw Exception("Expected long after \"-delayTime\"")
         }
      }
      
   }
   
   
   try {
      println("=============== Program Start ===============")
      init()
      
      //println("\nUsing provider: ${led.provider()}\n")
      
      runBlocking {
         while (true) {
            
            println("=========================================================")
            receiveInputs()
            
            proccessInputs()
            
            delay(delayTime)
            
         }
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
   
   // ################## Bucket setup
   bucket1 = ArrayDeque(bucketSize)
   bucket2 = ArrayDeque(bucketSize)
   
   bucketHistory1 = ArrayDeque(historySize)
   bucketHistory2 = ArrayDeque(historySize)
   
   // ################## Pins and info #####################
   val LED_PIN = 27
   
   when {
      LED_PIN < 0 -> throw InvalidPinException(LED_PIN, "Change LED pin!")
   }
   
   // ########################## Set outputs ###########################
   val ledConfig = DigitalOutputConfigBuilder.newInstance(pi)
      .bcm(LED_PIN)
      .initial(LOW)
      .shutdown(LOW)
      .build()
   led = pi.digitalOutput<DigitalOutputProvider>().create(ledConfig)
   
   // ########################### Set Inputs ##################################
   
   val dallases = Dallas18B20.sensors()
   println("Found ${dallases.size} sensors")
   
   if (dallases.size < 2) {
      throw Exception("Not all sensors have been detected")
   }
   
   sensor1 = object : SensorInterface<Double> {
      override fun onRead(): Double {
         try {
            val r = Dallas18B20.readTemp(dallases[0].id)
            println("Read from sensor ${dallases[0].id} value $r")
            return r
         } catch (ex: Exception) {
            return -1.0
         }
      }
   }
   
   sensor2 = object : SensorInterface<Double> {
      override fun onRead(): Double {
         try {
            val r = Dallas18B20.readTemp(dallases[1].id)
            println("Read from sensor ${dallases[1].id} value $r")
            return r
         } catch (ex: Exception) {
            return -1.0
         }
      }
   }
}

/**
 * Receive inputs.
 * ## Requirements
 * * Read new inputs
 * * Store new inputs in a bucket
 * * Coagulate full bucket
 * */
fun receiveInputs() {
   val s1 = sensor1.onRead()
   if (bucket1.isFull()) {
      bucket1.addToHistory(bucketHistory1)
   }
   bucket1.add(s1)
   
   
   val s2 = sensor2.onRead()
   if (bucket2.isFull()) {
      bucket2.addToHistory(bucketHistory2)
   }
   bucket2.add(s2)
   
   println(
      """
      Bucket1: ${bucket1.format(dataFormat)}
      History1: ${bucketHistory1.format(dataFormat)}
      Bucket2: ${bucket2.format(dataFormat)}
      History2: ${bucketHistory2.format(dataFormat)}
   """.trimIndent()
   )
}

/**
 * Proccess new received inputs.
 *
 * ## Requirements
 * * Proccess current bucket of inputs
 * */
fun proccessInputs() {

}

/**
 * ## Requirements:
 * * Receive client request for a socket connection and create it if there is a request that doesn't match an existing socket.
 * */
fun receiveAPIrequest() {

}

/**
 * ## Requirements:
 * * Send an api update through a socket.
 * */
fun sendAPIupdate() {

}

/**
 * ## Requirements:
 * * Verify if sockets are still active and if not, deactivate them
 * */
fun verifyAPIsockets() {
   // make this affect a socket instead of this empty code
   sendAPIupdate()
}


