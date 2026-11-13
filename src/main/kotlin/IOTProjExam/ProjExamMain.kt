package com.botofan.IOTProjExam

import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object ProjExamMain {
   
   data class Reading(val tempC: Double, val humidity: Double)
   
   class Dht11Stream(
      private val script: String = "/home/pi3andrei/shared/dht.py",
      private val warmupLinesToSkip: Int = 2,
      private val onReading: (Reading) -> Unit,
      private val onWarn: (String) -> Unit
   ) {
      @Volatile private var running = true
      private var process: Process? = null
      
      fun start(): Thread = thread(isDaemon = true, name = "dht11") {
         while (running) {
            try {
               val proc = ProcessBuilder("python3", "-u", script)
                  .redirectErrorStream(true)
                  .start()
               process = proc
               var lineNo = 0
               
               proc.inputStream.bufferedReader().useLines { lines ->
                  for (raw in lines) {
                     if (!running) return@useLines
                     lineNo++
                     
                     // Skip the first N lines — they're always ERR after startup
                     if (lineNo <= warmupLinesToSkip) {
                        onWarn("warmup skip #$lineNo: $raw")
                        continue
                     }
                     
                     val r = parseLine(raw)
                     if (r != null) onReading(r) else onWarn(raw)
                  }
               }
               
               proc.waitFor(2, TimeUnit.SECONDS)
               if (running) onWarn("python exited, respawning…")
            } catch (e: Exception) {
               if (running) {
                  onWarn("spawn/read failed: ${e.message}")
                  Thread.sleep(1000)
               }
            }
         }
      }
      
      fun stop() {
         running = false
         process?.destroy()
         process?.waitFor(2, TimeUnit.SECONDS)
      }
      
      companion object {
         private val LINE = Regex("""t(-?\d+(?:\.\d+)?)\s*,\s*h(\d+(?:\.\d+)?)\s*%?""")
         
         fun parseLine(raw: String): Reading? {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("ERR")) return null
            val m = LINE.matchEntire(line) ?: return null
            val (t, h) = m.destructured
            return Reading(t.toDouble(), h.toDouble())
         }
      }
   }
   
   // Uses pin BCM 23 in the python file
   fun ProjectExamMain(args:Array<String>) {
      val stream = Dht11Stream(
         onReading = { r ->
            println("Temp: %.1f C    Humidity: %.0f %%".format(r.tempC, r.humidity))
         },
         onWarn = { line -> println("[warn] $line") }
      )
      
      Runtime.getRuntime().addShutdownHook(Thread { stream.stop() })
      val t = stream.start()
      t.join()
   }
   
   
}