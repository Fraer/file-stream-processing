package org.lunatech.model

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.RootJsonFormat

case class ResponseChunk(content: String, processed: Int, skipped: Int)

object ResponseChunk extends SprayJsonSupport with spray.json.DefaultJsonProtocol {

  implicit val chunkFormat: RootJsonFormat[ResponseChunk] = jsonFormat3(ResponseChunk.apply)
}