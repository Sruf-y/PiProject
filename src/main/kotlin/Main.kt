package com.botofan

import com.botofan.Extrapolator.lastNAverage
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.xml.crypto.Data
import kotlin.math.abs


lateinit var pi: Context
lateinit var console: Console

lateinit var led: DigitalOutput
val sensors: ArrayList<SensorInterface<DataPoint>> = ArrayList()

var dataFormat: String = "%.2f"
var delayTime: Long = 0

var bucketSize: Int = 10
fun <T>ArrayDeque<T>.isFull(): Boolean {
   return this.size >= bucketSize
}

fun LocalDateTime.toMillis():Long{
   return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Long.toLocalDateTime(): LocalDateTime{
   return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

val scope = CoroutineScope(Dispatchers.Default)

val printLines: ArrayList<String> = ArrayList()

fun printBlock(lines: List<String>, prevLineCount: Int): Int {
   if(printNormal){
      lines.forEach { println(it) }
   }else{
      if (prevLineCount > 0) {
         print("\u001B[${prevLineCount}A")  // cursor up N lines
      }
      print("\u001B[J")                       // clear from cursor to end of screen
      lines.forEach { println(it) }
      System.out.flush()
   }
   
   return lines.size
}
var printNormal = false
fun printlb(message:String){
   printLines.add(message)
}

fun displayArgs() {
   println(
      """
      =====================================================
      Arguments:
      -h,-help : show help
      -bs,-bucketSize : set the bucketing size, default is ${bucketSize}. Minimum is 3
      -format : set the format for the readings. Ex: %1.f or %.f or %2.2f. Default is ${dataFormat}
      -delay : set the loop delay. Default is ${delayTime}
      -pr,-printReadings : print temperature readings. false/true. Default ${printTemperatureReadings}
      -pb,-printBuckets : print bucket values. false/true. Default ${printBucketValues}
      -dps, -dataPoolSize : nr of iterations until the program proccesses data to point out the bad
      -pn, printNormal : uses the normal way of printing instead of resetting the cursor. Default is ${printNormal}
      -takeNValues : how many of the latest values to take into account when proccesing data
      =====================================================
   """.trimIndent()
   )
}

fun List<DataPoint>.format(format:String): String{
   return "["+this.joinToString(", ") { String.format(format,it.value) }+"]"
}

fun List<DataPoint>.average():Double{
   return this.map { it.value }.average()
}

var printTemperatureReadings = false
var printBucketValues = true
var dataProccessingPoolSize = 10
var dataProccesingConsideredN = 5

/**
 * @param requiredIndex if you want that param to be found on a speciffic index, like 0
 * */
fun tryRet(args: Array<String>,vararg names: String,requiredIndex:Int = -1, onFound: (index: Int, name: String) -> Unit): Boolean {
   for (name in names) {
      val i = args.indexOf(name)
      if (i != -1) {
         if(requiredIndex !=-1)
         {
            if(i == requiredIndex){
               onFound(i,name)
               return true
            }
            return false
         }else{
            onFound(i, name)
            return true
         }
      }
   }
   return false
}
fun main(args: Array<String>) {
   
   if (args.isNotEmpty()) {
      
      if(tryRet(args,"-h","-help", requiredIndex = 0) { index,_->
         displayArgs()
      }){
         return
      }
      tryRet(args,"-bucketSize","-bs"){index,_->
         try {
            bucketSize = maxOf(args[index + 1].toInt(),3)
         } catch (ex: Exception) {
            throw Exception("Expected integer after \"-bucketSize\"",ex)
         }
      }
      tryRet(args,"-format"){index,_->
         try {
            val newformat = args[index + 1]
            String.format(newformat, 1.0) // try to see if the format is good or not
            dataFormat = newformat
            
         } catch (ex: Exception) {
            throw Exception("Invalid format provided after \"-format\"",ex)
         }
      }
      tryRet(args,"-delayTime"){index,_->
         try {
            delayTime = args[index + 1].toLong()
         } catch (ex: Exception) {
            throw Exception("Expected long after \"-delayTime\"",ex)
         }
      }
      tryRet(args,"-pr","-printReadings") {index,_->
         try {
            printTemperatureReadings = args[index + 1].toBoolean()
         } catch (ex: Exception) {
            throw Exception("Expected boolean after \"-printReadings\"",ex)
         }
      }
      tryRet(args,"-pb","-printBuckets") {index,_->
         try {
            printBucketValues = args[index + 1].toBoolean()
         } catch (ex: Exception) {
            throw Exception("Expected boolean after \"-printBuckets\"",ex)
         }
      }
      tryRet(args,"-dps", "-dataPoolSize") {index,_->
         try {
            dataProccessingPoolSize = abs(args[index + 1].toInt())
         } catch (ex: Exception) {
            throw Exception("Expected integer after \"-dataPoolSize\"",ex)
         }
      }
      tryRet(args,"-pn","-printNormal") { index,_->
         printNormal = true
      }
      tryRet(args,"takeNValues"){index,_->
         dataProccessingPoolSize = maxOf(args[index + 1].toInt(),3)
      }
   }
   
   
   try {
      println("=============== Program Start ===============")
      init()
      
      var prevLineCount = 0
      
      runBlocking {
         while (true) {
            
            printlb("=========================================================")
            receiveInputs()
            
            proccessInputs()
            
            prevLineCount = printBlock(printLines,prevLineCount)
            printLines.clear()
            
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
   println("Found ${pi.providers().all.size} providers:")
   pi.providers().all.forEach { println("${it.value.id}  ${it.value.type}  ${it.value.javaClass.simpleName}") }
   println("++++++++++++++++++++++++++++++++++++++++++++++")
   // ################## Bucket setup
   
   val dallases = Dallas18B20.sensors()
   println("Found ${dallases.size} sensors")
   
   dallases.forEachIndexed { index,sensor->
      sensors.add(
         object : SensorInterface<DataPoint> {
            override fun onRead(): DataPoint {
               val r = Dallas18B20.readTemp(dallases[index].id)
               if (printTemperatureReadings) {
                  printlb("Read from sensor ${dallases[index].id} value $r")
               }
               return DataPoint(LocalDateTime.now().toMillis(),r)
            }
            
            override val bucket: ArrayDeque<DataPoint> = ArrayDeque(bucketSize)
            
         }
      )
   }
   
   
   // ################## Pins and info #####################
   val LED_PIN = 27
   
   // ########################## Set outputs ###########################
   val ledConfig = DigitalOutputConfigBuilder.newInstance(pi)
      .bcm(LED_PIN)
      .initial(LOW)
      .shutdown(LOW)
      .build()
   led = pi.digitalOutput<DigitalOutputProvider>().create(ledConfig)
   
   // ########################### Set Inputs ##################################
}

/**
 * Receive inputs.
 * ## Requirements
 * * Read new inputs
 * * Store new inputs in a bucket
 * * Coagulate full bucket
 * */
fun receiveInputs() {
   
   sensors.forEachIndexed { index,sensor->
      try {
         val temp = sensor.onRead()
         if (sensor.bucket.isFull()) {
            sensor.bucket.removeFirstOrNull()
            sensor.bucket.add(temp)
         }else{
            sensor.bucket.add(temp)
         }
         
      }catch (ex: Exception){
         ex.printStackTrace()
      }
   }
   
   if(printBucketValues) {
      sensors.forEachIndexed { index, sensor ->
         printlb("Bucket-$index: " + sensor.bucket.format(dataFormat))
      }
   }
}

/**
 * Proccess new received inputs.
 *
 * ## Requirements
 * * Proccess current bucket of inputs and its history
 * */
var proccessInputsPoolCur = 0
var sensorsBucketAverage:Double? = null
var badSensorValue:Double? = null
var badSensor:Int? = null
var badDegradation: List<DataPoint>? = null
fun proccessInputs() {
   
   if(proccessInputsPoolCur>=dataProccessingPoolSize){
      
      val badSensorIndex = Extrapolator.getBadSensorIndex(sensors, sensorLeeway = 1.5, recentN = dataProccesingConsideredN)
      if(badSensorIndex!=null){
         sensorsBucketAverage = sensors.map { it.bucket.average() }.average()
         badSensorValue = sensors[badSensorIndex].bucket.lastNAverage(dataProccesingConsideredN)
         badDegradation = Extrapolator.processDegradationCurve(sensors[badSensorIndex].bucket)
      }else{
         sensorsBucketAverage = null
         badSensorValue = null
         badDegradation = null
      }
      
      
      sendAPIupdate(badSensorIndex)
      
      
      proccessInputsPoolCur = 0
   }else{
      proccessInputsPoolCur +=1
   }
   
   printlb("Bad sensor is ${badSensor}")
   printlb("Value: ${badSensorValue}")
   printlb("SensorsBucketAverage: ${sensorsBucketAverage}")
   printlb("Future ${badSensor}: ${badDegradation?.format(dataFormat)}")
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
fun sendAPIupdate(badSensorIndex: Int?) {
   badSensor = badSensorIndex
}

/**
 * ## Requirements:
 * * Verify if sockets are still active and if not, deactivate them
 * */
fun verifyAPIsockets() {

}


