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

package feral.lambda

import _root_.natchez.Kernel
import feral.lambda.events.ApiGatewayProxyEvent
import feral.lambda.events.ApiGatewayProxyEventV2
import feral.lambda.events.DynamoDbStreamEvent
import feral.lambda.events.KinesisStreamEvent
import feral.lambda.events.S3BatchEvent
import feral.lambda.events.SqsRecordAttributes
import feral.lambda.natchez.KernelSource
import org.typelevel.ci._

protected trait KernelSources {
  private[this] val `X-Amzn-Trace-Id` = ci"X-Amzn-Trace-Id"

  implicit def apiGatewayProxyEvent: KernelSource[ApiGatewayProxyEvent] =
    e => Kernel(e.headers.getOrElse(Map.empty))

  implicit def apiGatewayProxyEventV2: KernelSource[ApiGatewayProxyEventV2] =
    e => Kernel(e.headers)

  implicit def sqsRecordAttributes: KernelSource[SqsRecordAttributes] =
    a => Kernel(a.awsTraceHeader.map(`X-Amzn-Trace-Id` -> _).toMap)

  implicit def s3BatchEvent: KernelSource[S3BatchEvent] = KernelSource.emptyKernelSource

  @deprecated(
    "See feral.lambda.events.KinesisStreamEvent deprecation",
    since = "0.3.0"
  )
  implicit def kinesisStreamEvent: KernelSource[KinesisStreamEvent] =
    KernelSource.emptyKernelSource

  implicit def dynamoDbStreamEvent: KernelSource[DynamoDbStreamEvent] =
    KernelSource.emptyKernelSource
}

package object natchez extends KernelSources
