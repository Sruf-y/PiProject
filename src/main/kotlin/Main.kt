package com.botofan

import com.pi4j.Pi4J
import com.pi4j.context.Context
import com.pi4j.io.gpio.analog.AnalogInput
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
lateinit var sensor1: DigitalInput
lateinit var sensor2: DigitalInput

val scope = CoroutineScope(Dispatchers.Default)

fun main() {
   
   try {
      init()
      
      //println("\nUsing provider: ${led.provider()}\n")
      
      runBlocking {
      
      }
      
   } catch (e: Throwable) {
      e.printStackTrace()
   } finally {
      pi.shutdown()
   }
}

fun init() {
   pi = Injector.getPi4Context()
   console = Injector.getPi4Console()
   
   // ################## Pins and info #####################
   val LED_PIN = 27
   
   val SENSOR1_PIN = -1
   val SENSOR1_DEBOUNCE = 0L
   val SENSOR1_NAME = "Sensor1"
   val SENSOR1_ID = "PI_WIRED_SENSOR_2"
   
   val SENSOR2_PIN = -1
   val SENSOR2_DEBOUNCE = 0L
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
   val sensor1Config = DigitalInputConfigBuilder.newInstance(pi)
      .bcm(SENSOR1_PIN)
      .debounce(SENSOR1_DEBOUNCE)
      .id(SENSOR1_ID)
      .name(SENSOR1_NAME)
      .build()
   sensor1 = pi.digitalInput<DigitalInputProvider>().create(sensor1Config)
   
   val sensor2Config = DigitalInputConfigBuilder.newInstance(pi)
      .bcm(SENSOR2_PIN)
      .debounce(SENSOR2_DEBOUNCE)
      .id(SENSOR2_ID)
      .name(SENSOR2_NAME)
      .build()
   sensor2 = pi.digitalInput<DigitalInputProvider>().create(sensor2Config)
}