package com.botofan

import io.ktor.http.ContentType
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


object ServerUtils {
   fun serverMain(){
      
      runBlocking {
         
         val updates = MutableSharedFlow<String>(extraBufferCapacity = 1)
         
         val server = embeddedServer(CIO, host = "0.0.0.0", port = 8080) {
            install(SSE)
            routing {
               get("/") {
                  call.respondText(
                     """
                <html><body>
                <h3>Counter</h3>
                <p id="n">waiting…</p>
                <script>
                  const es = new EventSource('/events');
                  es.onmessage = e => document.getElementById('n').textContent = JSON.parse(e.data).count;
                </script>
                </body></html>
                """.trimIndent(),
                     ContentType.Text.Html
                  )
               }
               sse("/events") { updates.collect { send(ServerSentEvent(it)) } }
            }
         }
         
         val serverJob = launch(Dispatchers.IO) { server.start(wait = true) }
         
         Runtime.getRuntime().addShutdownHook(Thread {
            server.stop(0, 1_000)
            serverJob.cancel()
         })
         
         var t = 0L
         while (isActive) {
            updates.tryEmit("""{"count":$t}""")
            t += 1
            delay(1000)
         }
         
         server.stop(0, 1_000)
         serverJob.join()
      }
   }
}