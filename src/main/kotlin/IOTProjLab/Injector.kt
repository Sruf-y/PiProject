package com.botofan.IOTProjLab

import com.pi4j.Pi4J
import com.pi4j.context.Context
import com.pi4j.plugin.ffm.providers.gpio.FFMDigitalInputProviderImpl
import com.pi4j.plugin.ffm.providers.gpio.FFMDigitalOutputProviderImpl
import com.pi4j.util.Console

object Injector {
   fun getPi4Console(): Console = Console()
   fun getPi4Context(): Context = Pi4J.newContextBuilder()
      .add(FFMDigitalOutputProviderImpl())
      .add(FFMDigitalInputProviderImpl())
      .build()
   
}