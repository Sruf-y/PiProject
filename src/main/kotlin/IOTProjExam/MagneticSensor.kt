package com.botofan.IOTProjExam

import com.pi4j.Pi4J
import com.pi4j.context.Context
import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent
import com.pi4j.io.gpio.digital.DigitalStateChangeListener
import com.pi4j.io.gpio.digital.PullResistance

object MagneticSensor {
   
   val detect_yes = "DOOR CLOSED"
   val detect_no = "door open"
   
   fun startReading(
      pi: Context,
      onInitialState:(Boolean)-> Unit = {
         println("Reed initial state is : "+ if(it) detect_yes else detect_no)
      },
      onStateChange:(Boolean)-> Unit = {
         val state = if(it) detect_yes else detect_no
         println(state)
      }
   ){
      
      val reed = pi.create(
         DigitalInput.newConfigBuilder(pi)
            .id("reed-ky025")
            .name("KY-025 Reed")
            .bcm(23) // BCM GPIO23 = physical pin 16
            .pull(PullResistance.PULL_UP)             // KY-025 DO idles high
            .build()
      )
      
      onInitialState.invoke(reed.state().isHigh)
      
      reed.addListener(object: DigitalStateChangeListener{
         override fun onDigitalStateChange(p0: DigitalStateChangeEvent<*>?) {
            if(p0 == null){
               println("Magnetic sensor returned null.")
               return
            }
            onStateChange.invoke(p0.state().isHigh)
         }
      })
      
      //TODO: remove this for the actual project and instead MOVE it after the code this is embedded in so it doesn't use full cpu time like a while(true)
      Thread.sleep(Long.MAX_VALUE)
   }
}