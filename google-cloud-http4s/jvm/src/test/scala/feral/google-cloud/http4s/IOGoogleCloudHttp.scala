/*
 * Copyright 2021 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feral.googlecloud

import cats.effect.IO
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import feral.googlecloud.IOGoogleCloudHttp._
import munit.CatsEffectSuite
import org.http4s.Headers
import org.http4s.Method
import org.http4s.Response
import org.http4s.Uri
import org.http4s.syntax.all._
import scodec.bits.ByteVector

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.{util => ju}

class TestIOGoogleCloudHttp extends CatsEffectSuite {

  def googleRequest(method: Method, uri: Uri, headers: Headers, body: ByteVector) =
    new HttpRequest {
      def getMethod(): String = method.name
      def getUri(): String = uri.renderString
      def getHeaders(): ju.Map[String, ju.List[String]] = {
        val juHeaders = new ju.LinkedHashMap[String, ju.List[String]]
        headers.foreach { header =>
          juHeaders
            .computeIfAbsent(header.name.toString, _ => new ju.LinkedList[String]())
            .add(header.value)
          ()
        }
        juHeaders
      }
      def getInputStream(): InputStream = body.toInputStream
      def getContentType(): ju.Optional[String] = ???
      def getContentLength(): Long = ???
      def getCharacterEncoding(): ju.Optional[String] = ???
      def getReader(): BufferedReader = ???
      def getPath(): String = ???
      def getQuery(): ju.Optional[String] = ???
      def getQueryParameters(): ju.Map[String, ju.List[String]] = ???
      def getParts(): ju.Map[String, HttpRequest.HttpPart] = ???
    }

  def googleResponse() = new HttpResponse {
    var statusCode: Option[(Int, String)] = None
    val headers = new ju.HashMap[String, ju.List[String]]
    val body = new ByteArrayOutputStream
    def setStatusCode(code: Int): Unit = ???
    def setStatusCode(x: Int, y: String): Unit = statusCode = Some((x, y))
    def appendHeader(header: String, value: String): Unit = {
      headers.computeIfAbsent(header, _ => new ju.LinkedList[String]()).add(value)
      ()
    }
    def getOutputStream(): OutputStream = body
    def getHeaders(): ju.Map[String, ju.List[String]] = headers
    def getStatusCode(): (Int, String) = statusCode.get
    def getContentType(): ju.Optional[String] = ???
    def getWriter(): BufferedWriter = ???
    def setContentType(contentType: String): Unit = ???
  }

  var http4sResponse = Response[IO]()

  test("decode request") {
    for {
      request <- fromHttpRequest(
        googleRequest(
          Method.GET,
          uri"/default/nodejs-apig-function-1G3XMPLZXVXYI?",
          expectedHeaders,
          ByteVector.empty))
      _ <- IO(assertEquals(request.method, Method.GET))
      _ <- IO(assertEquals(request.uri, uri"/default/nodejs-apig-function-1G3XMPLZXVXYI?"))
      _ <- IO(assertEquals(request.headers, expectedHeaders))
      _ <- request.body.compile.to(ByteVector).assertEquals(ByteVector.empty)
    } yield ()
  }

  test("encode response") {
    for {
      gResponse <- IO(googleResponse())
      _ <- writeResponse(http4sResponse, gResponse)
      _ <- IO(assertEquals(gResponse.getStatusCode(), (200, "OK")))
      _ <- IO(assertEquals(gResponse.getHeaders(), new ju.HashMap[String, ju.List[String]]()))
      _ <- IO(assertEquals(ByteVector(gResponse.body.toByteArray()), ByteVector.empty))
    } yield ()
  }

  def expectedHeaders = Headers(
    "content-length" -> "0",
    "accept-language" -> "en-US,en;q=0.9",
    "sec-fetch-dest" -> "document",
    "sec-fetch-user" -> "?1",
    "x-amzn-trace-id" -> "Root=1-5e6722a7-cc56xmpl46db7ae02d4da47e",
    "host" -> "r3pmxmplak.execute-api.us-east-2.amazonaws.com",
    "sec-fetch-mode" -> "navigate",
    "accept-encoding" -> "gzip, deflate, br",
    "accept" ->
      "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
    "sec-fetch-site" -> "cross-site",
    "x-forwarded-port" -> "443",
    "x-forwarded-proto" -> "https",
    "upgrade-insecure-requests" -> "1",
    "user-agent" ->
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36",
    "x-forwarded-for" -> "205.255.255.176",
    "cookie" -> "s_fid=7AABXMPL1AFD9BBF-0643XMPL09956DE2; regStatus=pre-register"
  )

}
