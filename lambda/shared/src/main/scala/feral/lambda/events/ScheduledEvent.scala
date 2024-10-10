package feral.lambda.events

import java.time.{Instant, OffsetDateTime}

import io.circe.Decoder

import codecs.decodeOffsetDateTime

// https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/aws-lambda/trigger/eventbridge.d.ts
// https://github.com/aws/aws-lambda-java-libs/blob/main/aws-lambda-java-events/src/main/java/com/amazonaws/services/lambda/runtime/events/ScheduledEvent.java
sealed abstract class ScheduledEvent {
  def id: String
  def version: String
  def account: String
  def time: Instant
  def region: String
  def resources: List[String]
  def source: String
  def `detail-type`: String
  def detail: ScheduledEventDetail
  def `replay-name`: Option[String]
}

object ScheduledEvent {

  def apply(
             id: String,
               version: String,
  account: String,
  time: Instant,
  region: String,
  resources: List[String],
  source: String,
  `detail-type`: String,
  detail: ScheduledEventDetail,
             `replay-name`: Option[String]
  ): ScheduledEvent =
    new Impl(
      id,
      version,
      account,
      time,
      region,
      resources,
      source,
      `detail-type`,
      detail,
      `replay-name`
    )

  implicit val decoder: Decoder[ScheduledEvent] = Decoder.forProduct10(
    "id",
    "version",
    "account",
    "time",
    "region",
    "resources",
    "source",
    "detail-type",
    "detail",
    "replay-name"
  )(ScheduledEvent.apply)

  private final case class Impl(
                                 id: String,
                                 version: String,
                                 account: String,
                                 time: Instant,
                                 region: String,
                                 resources: List[String],
                                 source: String,
                                 `detail-type`: String,
                                 detail: ScheduledEventDetail,
                                 `replay-name`: Option[String]
  ) extends ScheduledEvent {
    override def productPrefix = "ScheduledEvent"
  }
}

sealed abstract class ScheduledEventDetail {
  def alarmName: String
  def operation: Option[String]
  def configuration: ScheduledEventConfiguration
  def previousState: Option[ScheduledEventPreviousState]
  def state: Option[ScheduledEventState]
}

object ScheduledEventDetail {
  def apply(
             alarmName: String,
             operation: Option[String],
             configuration: ScheduledEventConfiguration,
             previousState: Option[ScheduledEventPreviousState],
             state: Option[ScheduledEventState]
           ): ScheduledEventDetail =
    new Impl(alarmName, operation, configuration, previousState, state)

  implicit val decoder: Decoder[ScheduledEventDetail] = Decoder.forProduct5(
    "alarmName",
    "operation",
    "configuration",
    "previousState",
    "state"
  )(ScheduledEventDetail.apply)

  private final case class Impl(
                                 alarmName: String,
                                 operation: Option[String],
                                 configuration: ScheduledEventConfiguration,
                                 previousState: Option[ScheduledEventPreviousState],
                                 state: Option[ScheduledEventState]
  ) extends ScheduledEventDetail {
    override def productPrefix = "ScheduledEventDetail"
  }
}

sealed abstract class ScheduledEventConfiguration {
  def description: String
  def metrics: List[ScheduledEventMetric]
}

object ScheduledEventConfiguration {
  def apply(
             description: String,
             metrics: List[ScheduledEventMetric]
           ): ScheduledEventConfiguration =
    new Impl(description, metrics)

  implicit val decoder: Decoder[ScheduledEventConfiguration] = Decoder.forProduct2(
    "description",
    "metrics"
  )(ScheduledEventConfiguration.apply)

  private final case class Impl(
                                 description: String,
                                 metrics: List[ScheduledEventMetric]
  ) extends ScheduledEventConfiguration {
    override def productPrefix = "ScheduledEventConfiguration"
  }
}

sealed abstract class ScheduledEventMetric {
  def id: String
  def metricStat: ScheduledEventMetricStat
  def returnData: Boolean
}

object ScheduledEventMetric {
  def apply(
             id: String,
             metricStat: ScheduledEventMetricStat,
             returnData: Boolean
           ): ScheduledEventMetric =
    new Impl(id, metricStat, returnData)

  implicit val decoder: Decoder[ScheduledEventMetric] = Decoder.forProduct3(
    "id",
    "metricStat",
    "returnData"
  )(ScheduledEventMetric.apply)

  private final case class Impl(
                                 id: String,
                                 metricStat: ScheduledEventMetricStat,
                                 returnData: Boolean
  ) extends ScheduledEventMetric {
    override def productPrefix = "ScheduledEventMetric"
  }
}

sealed abstract class ScheduledEventMetricStat {
  def metric: Metric
  def period: Int
  def stat: String
}

object ScheduledEventMetricStat {
  def apply(
             metric: Metric,
             period: Int,
             stat: String
           ): ScheduledEventMetricStat =
    new Impl(metric, period, stat)

  implicit val decoder: Decoder[ScheduledEventMetricStat] = Decoder.forProduct3(
    "metric",
    "period",
    "stat"
  )(ScheduledEventMetricStat.apply)

  private final case class Impl(
                                 metric: Metric,
                                 period: Int,
                                 stat: String
  ) extends ScheduledEventMetricStat {
    override def productPrefix = "ScheduledEventMetricStat"
  }
}

sealed abstract class Metric {
  def dimensions: Map[String, String]
  def name: String
  def namespace: String
}

object Metric {
  def apply(
             dimensions: Map[String, String],
             name: String,
             namespace: String
           ): Metric =
    new Impl(dimensions, name, namespace)

  implicit val decoder: Decoder[Metric] = Decoder.forProduct3(
    "dimensions",
    "name",
    "namespace"
  )(Metric.apply)

  private final case class Impl(
                                 dimensions: Map[String, String],
                                 name: String,
                                 namespace: String
  ) extends Metric {
    override def productPrefix = "Metric"
  }
}

sealed abstract class ScheduledEventState {
  def reason: String
  def reasonData: String
  def timestamp: OffsetDateTime
  def value: String
}

object ScheduledEventState {
  def apply(
             reason: String,
             reasonData: String,
             timestamp: OffsetDateTime,
             value: String
           ): ScheduledEventState =
    new Impl(reason, reasonData, timestamp, value)

  implicit val decoder: Decoder[ScheduledEventState] = Decoder.forProduct4(
    "reason",
    "reasonData",
    "timestamp",
    "value"
  )(ScheduledEventState.apply)

  private final case class Impl(
                                 reason: String,
                                 reasonData: String,
                                 timestamp: OffsetDateTime,
                                 value: String
  ) extends ScheduledEventState {
    override def productPrefix = "ScheduledEventState"
  }
}

sealed abstract class ScheduledEventPreviousState {
  def reason: String
  def reasonData: Option[String]
  def timestamp: OffsetDateTime
  def value: String
}

object ScheduledEventPreviousState {
  def apply(
             reason: String,
             reasonData: Option[String],
             timestamp: OffsetDateTime,
             value: String
           ): ScheduledEventPreviousState =
    new Impl(reason, reasonData, timestamp, value)

  implicit val decoder: Decoder[ScheduledEventPreviousState] = Decoder.forProduct4(
    "reason",
    "reasonData",
    "timestamp",
    "value"
  )(ScheduledEventPreviousState.apply)

  private final case class Impl(
                                 reason: String,
                                 reasonData: Option[String],
                                 timestamp: OffsetDateTime,
                                 value: String
  ) extends ScheduledEventPreviousState {
    override def productPrefix = "ScheduledEventPreviousState"
  }
}
