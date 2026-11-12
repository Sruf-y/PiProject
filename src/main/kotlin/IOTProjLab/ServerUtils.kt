package com.botofan.IOTProjLab

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

val serverUpdates = MutableSharedFlow<String>(extraBufferCapacity = 1)
object ServerUtils {
   
   private val PAGE_HTML = """
<html><head>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/uplot@1.6.31/dist/uPlot.min.css">
<script src="https://cdn.jsdelivr.net/npm/uplot@1.6.31/dist/uPlot.iife.min.js"></script>
<style>
  .sensor { margin-bottom: 24px; padding: 8px; border-radius: 6px; }
  .sensor.bad { background: #fff0f0; outline: 2px solid #d33; }
  .row { display: flex; gap: 16px; flex-wrap: wrap; }
  #tip {
    position: fixed; pointer-events: none; z-index: 10; display: none;
    background: rgba(0,0,0,0.82); color: #fff; font: 12px monospace;
    padding: 4px 8px; border-radius: 4px; white-space: nowrap;
  }
</style>
</head><body>
<div id="root"></div>
<div id="tip"></div>
<script>
const opts = function(label, stroke, dash) {
  return {
    width: 500, height: 220,
    scales: { x: { time: true }, y: { range: [-15, 50] } },
    cursor: { y: true, points: { size: 8 } },
    series: [ {}, { label: label, stroke: stroke, dash: dash } ]
  };
};

const tip = document.getElementById("tip");

function attachTip(u, seriesIndex, label) {
  u.over.addEventListener("mousemove", function(ev) {
    const idx = u.cursor.idx;
    if (idx == null) { tip.style.display = "none"; return; }
    const yArr = u.data[seriesIndex];
    if (!yArr || yArr[idx] == null) { tip.style.display = "none"; return; }
    const x = u.data[0][idx];           // seconds
    const y = yArr[idx];
    tip.textContent = label + ": " + y.toFixed(2) + " \u00B0C  (" + new Date(x * 1000).toLocaleTimeString() + ")";
    tip.style.display = "block";
    tip.style.left = (ev.clientX + 12) + "px";
    tip.style.top  = (ev.clientY + 12) + "px";
  });
  u.over.addEventListener("mouseleave", function() { tip.style.display = "none"; });
}

const charts = new Map();

function ensureCharts(i) {
  if (charts.has(i)) return charts.get(i);
  const box = document.createElement("div");
  box.className = "sensor";
  box.innerHTML =
    '<h3>Sensor ' + i + '</h3>' +
    '<div class="row">' +
      '<div><div>Measured</div><div id="m' + i + '"></div></div>' +
      '<div><div>Extrapolated</div><div id="e' + i + '"></div></div>' +
    '</div>';
  document.getElementById("root").appendChild(box);
  const p1 = new uPlot(opts("measured", "red", undefined), [[],[]], document.getElementById("m" + i));
  const p2 = new uPlot(opts("extrapolated", "blue", [4,4]), [[],[]], document.getElementById("e" + i));
  attachTip(p1, 1, "Sensor " + i + " measured");
  attachTip(p2, 1, "Sensor " + i + " extrapolated");
  const entry = { box: box, p1: p1, p2: p2 };
  charts.set(i, entry);
  return entry;
}

function render(d) {
  if (typeof d.sensor !== "number") return;
  const entry = ensureCharts(d.sensor);
  entry.box.classList.toggle("bad", !!d.isBad);

  const measured = d.measured || [];
  const extrapolated = d.extrapolated || [];

  entry.p1.setData([
    measured.map(function(p) { return p.timestamp / 1000; }),
    measured.map(function(p) { return p.value; })
  ]);
  entry.p2.setData([
    extrapolated.map(function(p) { return p.timestamp / 1000; }),
    extrapolated.map(function(p) { return p.value; })
  ]);
}

const es = new EventSource('/events');
es.onmessage = function(e) { render(JSON.parse(e.data)); };
</script>
</body></html>
""".trimIndent()
   suspend fun serverMain() {
      coroutineScope {
         val server = embeddedServer(CIO, host = "0.0.0.0", port = 8080) {
            install(SSE)
            routing {
               get("/") { call.respondText(PAGE_HTML, ContentType.Text.Html) }
               sse("/events") { serverUpdates.collect { send(ServerSentEvent(it)) } }
            }
         }
         val serverJob = launch(Dispatchers.IO) { server.start(wait = true) }
         Runtime.getRuntime().addShutdownHook(Thread {
            server.stop(0, 1_000)
            serverJob.cancel()
         })
         // No counter loop. Server just runs.
         serverJob.join()
      }
   }
   
   fun sendAllUpdates(
      sensors: List<SensorInterface<DataPoint>>,
      badSensorIndex: Int?,
      extrapolate: (ArrayDeque<DataPoint>) -> List<DataPoint>
   ) {
      sensors.forEachIndexed { i, s ->
         val measured = s.bucket.toList().takeLast(200)
         val extrapolated = extrapolate(s.bucket)
         serverUpdates.tryEmit(
            json.encodeToString(
               Series(i, i == badSensorIndex, measured, extrapolated)
            )
         )
      }
   }
   
   val json = Json { encodeDefaults = true }
}