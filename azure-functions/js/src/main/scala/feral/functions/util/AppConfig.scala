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

package feral.functions.util

import AppConfig._

import scala.scalajs.js

import feral.functions.facade.JSRequest
import feral.functions.facade.InvocationContext

final case class AppConfig(
    methods: List[HttpMethod],
    authLevel: AuthLevel,
    route: AzureRoute,
    handlerFn: HandlerFnT) {
  private[functions] def toJS: js.Object = {
    js.Dynamic
      .literal(
        methods = methodsToJS(methods),
        authLevel = authLevel.value,
        route = route.value,
        handler = handlerFn
      )
  }
}

object AppConfig {
  sealed trait HttpMethod {
    private[functions] def value: String = {
      this match {
        case Get => "GET"
        case Post => "POST"
        case Put => "PUT"
        case Patch => "PATCH"
        case Delete => "DELETE"
      }
    }
  }
  case object Get extends HttpMethod
  case object Post extends HttpMethod
  case object Put extends HttpMethod
  case object Patch extends HttpMethod
  case object Delete extends HttpMethod

  private def methodsToJS(list: List[HttpMethod]): js.Array[String] = {
    val dList = list.distinct.map(_.value)
    js.Array(dList: _*)
  }

  case class AuthLevel(value: String)
  val anonymous: AuthLevel = AuthLevel("anonymous")
  // add more?

  case class AzureRoute(value: String)
  val catchAll: AzureRoute = AzureRoute("{*path}")
  // add more?

  type HandlerFnT = js.Function2[JSRequest, InvocationContext, js.Promise[js.UndefOr[js.Any]]]

  private[functions] def buildDefaultConfig(handlerFn: HandlerFnT) =
    AppConfig(List(Get, Post, Put, Patch, Delete), anonymous, catchAll, handlerFn)
}
