package com.botofan

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// °C

data class Sensor(val id: String, val path: Path)
object Dallas18B20 {
   
   private val baseDir: Path = Paths.get("/sys/bus/w1/devices")
   
   /** All DS18B20 sensors currently present (family code 28). */
   fun sensors(): List<Sensor> =
      Files.list(baseDir).use { stream ->
         stream
            .filter { it.fileName.toString().startsWith("28-") }
            .map { Sensor(it.fileName.toString(), it.resolve("w1_slave")) }
            .toList()
      }
   
   fun readTemp(sensor: Sensor): Double = parseW1Slave(Files.readString(sensor.path))
   
   fun readTemp(deviceId: String): Double {
      val path = baseDir.resolve(deviceId).resolve("w1_slave")
      return parseW1Slave(Files.readString(path))
   }
   
   private fun parseW1Slave(content: String): Double {
      if (!content.contains("YES")) throw java.io.IOException("CRC check failed")
      val idx = content.indexOf("t=")
      if (idx < 0) throw java.io.IOException("No temperature found in w1_slave")
      return content.substring(idx + 2).trim().toDouble() / 1000.0
   }
}

   
