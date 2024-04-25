package org.lunatech.controller

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import org.apache.commons.math3.stat.regression.SimpleRegression
import org.apache.pekko.NotUsed
import org.apache.pekko.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.stream.connectors.csv.scaladsl.CsvParsing
import org.apache.pekko.stream.scaladsl.{Flow, Source}
import org.apache.pekko.util.ByteString
import org.lunatech.model.ResponseChunk

class UserRoutes()() {

  val parseCsv: Flow[ByteString, List[ByteString], NotUsed] = CsvParsing.lineScanner()

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport
    .json()
    .withFramingRenderer(Flow[ByteString].intersperse(ByteString("\n")))

  // curl -v -F data=@dataset.csv http://localhost:8080/process

  val userRoutes: Route =
      path ("process") {
        withoutSizeLimit {
          fileUpload("data") {
            case (metadata, byteSource) =>
              println(s"[pekko-stream] /process ${metadata.fileName}")
              val counter = new AtomicInteger(0)
              val skipped = new AtomicInteger(0)

              val source = byteSource.via(parseCsv)
                .drop(1)
                .intersperse(List.empty, List.empty, List(ByteString("last")))
                .filter(_.nonEmpty)
                .statefulMapConcat { () =>
                  val localSr = new SimpleRegression()

                  { elements =>
                    if (elements.isEmpty) {
                      Nil
                    } else {
                      val name = elements(0).utf8String
                      if (name == "last") {
                        val predictions = runPredictions(localSr)
                        ResponseChunk(predictions, -1, -1) :: Nil
                      } else {
                        val rawDistance = elements(19).utf8String
                        val rawArrDelay = elements(15).utf8String
                        if (rawDistance.isBlank || rawArrDelay.isBlank) {
                          ResponseChunk(name, counter.incrementAndGet, skipped.incrementAndGet) :: Nil
                        } else {
                          val distance = rawDistance.toDouble
                          val arrDelay = rawArrDelay.toDouble
                          localSr.addData(distance, arrDelay)
                          ResponseChunk(name, counter.incrementAndGet, skipped.get) :: Nil
                        }
                      }
                    }
                  }
                }
              complete(source)
          }
        }
      }

  private def runPredictions(sr: SimpleRegression) = {
      val sb = new StringBuilder
      // Display the intercept of the regression
      sb.append("Intercept: " + sr.getIntercept)
      sb.append("\n")
      // Display the slope of the regression.
      sb.append("Slope: " + sr.getSlope)
      sb.append("\n")
      // Display the slope standard error
      sb.append("Standard Error: " + sr.getSlopeStdErr)
      sb.append("\n")
      // Display adjusted R2 value
      sb.append("Adjusted R2 value: " + sr.getRSquare)
      sb.append("\n")
      sb.append("*************************************************")
      sb.append("\n")
      sb.append("Running random predictions......")
      sb.append("\n")
      sb.append("")
      val inputs = Array[Double](10, 300, 500, 850, 1500, 3000, 5500, 9000, 15000, 25000)
      for (i <- inputs.indices) {
        val rn = inputs(i)
        sb.append("Input score: " + rn + " prediction: " + sr.predict(rn).round)
        sb.append("\n")
      }
      sb.toString
  }
}
