package feral.lambda.events

import java.time.{Instant, OffsetDateTime}
import java.time.format.DateTimeFormatter

import io.circe.literal._

import munit.FunSuite

class ScheduledEventSuite extends FunSuite {

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  test("Decoding of alarm status changes based on a single metric") {
    assertEquals(singleMetricEvent.as[ScheduledEvent].toTry.get, singleMetricResult)
  }

  test("Decoding of alarm state changes based on metrics formulas") {
    assertEquals(metricsFormulaEvent.as[ScheduledEvent].toTry.get, metricsFormulaResult)
  }

  test("Decoding of change of state of abnormality detection alarms") {
    assertEquals(
      abnormalityDetectionEvent.as[ScheduledEvent].toTry.get,
      abnormalityDetectionResult
    )
  }

  test("Decoding change of state of compound alarm with suppressor alarm") {
    assertEquals(
      compoundAlarmSuppressorEvent.as[ScheduledEvent].toTry.get,
      compoundAlarmSuppressorResult
    )
  }

  // Alarm status changes based on a single metric
  private def singleMetricEvent = json"""
    {
        "version": "0",
        "id": "c4c1c1c9-6542-e61b-6ef0-8c4d36933a92",
        "detail-type": "CloudWatch Alarm State Change",
        "source": "aws.cloudwatch",
        "account": "123456789012",
        "time": "2019-10-02T17:04:40Z",
        "region": "us-east-1",
        "resources": [
            "arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServerCpuTooHigh"
        ],
        "detail": {
            "alarmName": "ServerCpuTooHigh",
            "configuration": {
                "description": "Goes into alarm when server CPU utilization is too high!",
                "metrics": [
                    {
                        "id": "30b6c6b2-a864-43a2-4877-c09a1afc3b87",
                        "metricStat": {
                            "metric": {
                                "dimensions": {
                                    "InstanceId": "i-12345678901234567"
                                },
                                "name": "CPUUtilization",
                                "namespace": "AWS/EC2"
                            },
                            "period": 300,
                            "stat": "Average"
                        },
                        "returnData": true
                    }
                ]
            },
            "previousState": {
                "reason": "Threshold Crossed: 1 out of the last 1 datapoints [0.0666851903306472 (01/10/19 13:46:00)] was not greater than the threshold (50.0) (minimum 1 datapoint for ALARM -> OK transition).",
                "reasonData": "{\"version\":\"1.0\",\"queryDate\":\"2019-10-01T13:56:40.985+0000\",\"startDate\":\"2019-10-01T13:46:00.000+0000\",\"statistic\":\"Average\",\"period\":300,\"recentDatapoints\":[0.0666851903306472],\"threshold\":50.0}",
                "timestamp": "2019-10-01T13:56:40.987+0000",
                "value": "OK"
            },
            "state": {
                "reason": "Threshold Crossed: 1 out of the last 1 datapoints [99.50160229693434 (02/10/19 16:59:00)] was greater than the threshold (50.0) (minimum 1 datapoint for OK -> ALARM transition).",
                "reasonData": "{\"version\":\"1.0\",\"queryDate\":\"2019-10-02T17:04:40.985+0000\",\"startDate\":\"2019-10-02T16:59:00.000+0000\",\"statistic\":\"Average\",\"period\":300,\"recentDatapoints\":[99.50160229693434],\"threshold\":50.0}",
                "timestamp": "2019-10-02T17:04:40.989+0000",
                "value": "ALARM"
            }
        }
    }
    """

  private def singleMetricResult: ScheduledEvent = ScheduledEvent(
    id = "c4c1c1c9-6542-e61b-6ef0-8c4d36933a92",
    version = "0",
    account = "123456789012",
    time = Instant.parse("2019-10-02T17:04:40Z"),
    region = "us-east-1",
    resources = List("arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServerCpuTooHigh"),
    source = "aws.cloudwatch",
    `detail-type` = "CloudWatch Alarm State Change",
    detail = ScheduledEventDetail(
      alarmName = "ServerCpuTooHigh",
      operation = None,
      configuration = ScheduledEventConfiguration(
        description = Some("Goes into alarm when server CPU utilization is too high!"),
        metrics = Some(
          List(
            ScheduledEventMetric(
              id = "30b6c6b2-a864-43a2-4877-c09a1afc3b87",
              metricStat = Some(
                ScheduledEventMetricStat(
                  metric = Metric(
                    dimensions = Map("InstanceId" -> "i-12345678901234567"),
                    name = "CPUUtilization",
                    namespace = "AWS/EC2"
                  ),
                  period = 300,
                  stat = "Average"
                )),
              returnData = true,
              expression = None,
              label = None
            )
          )),
        alarmRule = None,
        actionsSuppressor = None,
        actionsSuppressorWaitPeriod = None,
        actionsSuppressorExtensionPeriod = None
      ),
      previousState = Some(
        ScheduledEventPreviousState(
          reason =
            "Threshold Crossed: 1 out of the last 1 datapoints [0.0666851903306472 (01/10/19 13:46:00)] was not greater than the threshold (50.0) (minimum 1 datapoint for ALARM -> OK transition).",
          reasonData = Some(
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-01T13:56:40.985+0000\",\"startDate\":\"2019-10-01T13:46:00.000+0000\",\"statistic\":\"Average\",\"period\":300,\"recentDatapoints\":[0.0666851903306472],\"threshold\":50.0}"),
          timestamp = OffsetDateTime.parse("2019-10-01T13:56:40.987+0000", formatter),
          value = "OK"
        )),
      state = Some(
        ScheduledEventState(
          reason =
            "Threshold Crossed: 1 out of the last 1 datapoints [99.50160229693434 (02/10/19 16:59:00)] was greater than the threshold (50.0) (minimum 1 datapoint for OK -> ALARM transition).",
          reasonData =
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-02T17:04:40.985+0000\",\"startDate\":\"2019-10-02T16:59:00.000+0000\",\"statistic\":\"Average\",\"period\":300,\"recentDatapoints\":[99.50160229693434],\"threshold\":50.0}",
          timestamp = OffsetDateTime.parse("2019-10-02T17:04:40.989+0000", formatter),
          value = "ALARM",
          actionsSuppressedBy = None,
          actionsSuppressedReason = None
        ))
    ),
    `replay-name` = None
  )

  // Decoding of alarm state changes based on metrics formulas
  private def metricsFormulaEvent = json"""
    {
        "version": "0",
        "id": "2dde0eb1-528b-d2d5-9ca6-6d590caf2329",
        "detail-type": "CloudWatch Alarm State Change",
        "source": "aws.cloudwatch",
        "account": "123456789012",
        "time": "2019-10-02T17:20:48Z",
        "region": "us-east-1",
        "resources": [
            "arn:aws:cloudwatch:us-east-1:123456789012:alarm:TotalNetworkTrafficTooHigh"
        ],
        "detail": {
            "alarmName": "TotalNetworkTrafficTooHigh",
            "configuration": {
                "description": "Goes into alarm if total network traffic exceeds 10Kb",
                "metrics": [
                    {
                        "expression": "SUM(METRICS())",
                        "id": "e1",
                        "label": "Total Network Traffic",
                        "returnData": true
                    },
                    {
                        "id": "m1",
                        "metricStat": {
                            "metric": {
                                "dimensions": {
                                    "InstanceId": "i-12345678901234567"
                                },
                                "name": "NetworkIn",
                                "namespace": "AWS/EC2"
                            },
                            "period": 300,
                            "stat": "Maximum"
                        },
                        "returnData": false
                    },
                    {
                        "id": "m2",
                        "metricStat": {
                            "metric": {
                                "dimensions": {
                                    "InstanceId": "i-12345678901234567"
                                },
                                "name": "NetworkOut",
                                "namespace": "AWS/EC2"
                            },
                            "period": 300,
                            "stat": "Maximum"
                        },
                        "returnData": false
                    }
                ]
            },
            "previousState": {
                "reason": "Unchecked: Initial alarm creation",
                "timestamp": "2019-10-02T17:20:03.642+0000",
                "value": "INSUFFICIENT_DATA"
            },
            "state": {
                "reason": "Threshold Crossed: 1 out of the last 1 datapoints [45628.0 (02/10/19 17:10:00)] was greater than the threshold (10000.0) (minimum 1 datapoint for OK -> ALARM transition).",
                "reasonData": "{\"version\":\"1.0\",\"queryDate\":\"2019-10-02T17:20:48.551+0000\",\"startDate\":\"2019-10-02T17:10:00.000+0000\",\"period\":300,\"recentDatapoints\":[45628.0],\"threshold\":10000.0}",
                "timestamp": "2019-10-02T17:20:48.554+0000",
                "value": "ALARM"
            }
        }
    }
  """

  private def metricsFormulaResult: ScheduledEvent = ScheduledEvent(
    id = "2dde0eb1-528b-d2d5-9ca6-6d590caf2329",
    version = "0",
    account = "123456789012",
    time = Instant.parse("2019-10-02T17:20:48Z"),
    region = "us-east-1",
    resources =
      List("arn:aws:cloudwatch:us-east-1:123456789012:alarm:TotalNetworkTrafficTooHigh"),
    source = "aws.cloudwatch",
    `detail-type` = "CloudWatch Alarm State Change",
    detail = ScheduledEventDetail(
      alarmName = "TotalNetworkTrafficTooHigh",
      operation = None,
      configuration = ScheduledEventConfiguration(
        description = Some("Goes into alarm if total network traffic exceeds 10Kb"),
        metrics = Some(
          List(
            ScheduledEventMetric(
              expression = Some("SUM(METRICS())"),
              id = "e1",
              label = Some("Total Network Traffic"),
              returnData = true,
              metricStat = None
            ),
            ScheduledEventMetric(
              id = "m1",
              metricStat = Some(
                ScheduledEventMetricStat(
                  metric = Metric(
                    dimensions = Map("InstanceId" -> "i-12345678901234567"),
                    name = "NetworkIn",
                    namespace = "AWS/EC2"
                  ),
                  period = 300,
                  stat = "Maximum"
                )),
              returnData = false,
              expression = None,
              label = None
            ),
            ScheduledEventMetric(
              id = "m2",
              metricStat = Some(
                ScheduledEventMetricStat(
                  metric = Metric(
                    dimensions = Map("InstanceId" -> "i-12345678901234567"),
                    name = "NetworkOut",
                    namespace = "AWS/EC2"
                  ),
                  period = 300,
                  stat = "Maximum"
                )),
              returnData = false,
              expression = None,
              label = None
            )
          )),
        alarmRule = None,
        actionsSuppressor = None,
        actionsSuppressorWaitPeriod = None,
        actionsSuppressorExtensionPeriod = None
      ),
      previousState = Some(
        ScheduledEventPreviousState(
          reason = "Unchecked: Initial alarm creation",
          reasonData = None,
          timestamp = OffsetDateTime.parse("2019-10-02T17:20:03.642+0000", formatter),
          value = "INSUFFICIENT_DATA"
        )),
      state = Some(
        ScheduledEventState(
          reason =
            "Threshold Crossed: 1 out of the last 1 datapoints [45628.0 (02/10/19 17:10:00)] was greater than the threshold (10000.0) (minimum 1 datapoint for OK -> ALARM transition).",
          reasonData =
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-02T17:20:48.551+0000\",\"startDate\":\"2019-10-02T17:10:00.000+0000\",\"period\":300,\"recentDatapoints\":[45628.0],\"threshold\":10000.0}",
          timestamp = OffsetDateTime.parse("2019-10-02T17:20:48.554+0000", formatter),
          value = "ALARM",
          actionsSuppressedBy = None,
          actionsSuppressedReason = None
        ))
    ),
    `replay-name` = None
  )

  // Abnormality detection alarm status change
  private def abnormalityDetectionEvent = json"""
    {
        "version": "0",
        "id": "daafc9f1-bddd-c6c9-83af-74971fcfc4ef",
        "detail-type": "CloudWatch Alarm State Change",
        "source": "aws.cloudwatch",
        "account": "123456789012",
        "time": "2019-10-03T16:00:04Z",
        "region": "us-east-1",
        "resources": ["arn:aws:cloudwatch:us-east-1:123456789012:alarm:EC2 CPU Utilization Anomaly"],
        "detail": {
            "alarmName": "EC2 CPU Utilization Anomaly",
            "state": {
                "value": "ALARM",
                "reason": "Thresholds Crossed: 1 out of the last 1 datapoints [0.0 (03/10/19 15:58:00)] was less than the lower thresholds [0.020599444741798756] or greater than the upper thresholds [0.3006915352732461] (minimum 1 datapoint for OK -> ALARM transition).",
                "reasonData": "{\"version\":\"1.0\",\"queryDate\":\"2019-10-03T16:00:04.650+0000\",\"startDate\":\"2019-10-03T15:58:00.000+0000\",\"period\":60,\"recentDatapoints\":[0.0],\"recentLowerThresholds\":[0.020599444741798756],\"recentUpperThresholds\":[0.3006915352732461]}",
                "timestamp": "2019-10-03T16:00:04.653+0000"
            },
            "previousState": {
                "value": "OK",
                "reason": "Thresholds Crossed: 1 out of the last 1 datapoints [0.166666666664241 (03/10/19 15:57:00)] was not less than the lower thresholds [0.0206719426210418] or not greater than the upper thresholds [0.30076870222143803] (minimum 1 datapoint for ALARM -> OK transition).",
                "reasonData": "{\"version\":\"1.0\",\"queryDate\":\"2019-10-03T15:59:04.670+0000\",\"startDate\":\"2019-10-03T15:57:00.000+0000\",\"period\":60,\"recentDatapoints\":[0.166666666664241],\"recentLowerThresholds\":[0.0206719426210418],\"recentUpperThresholds\":[0.30076870222143803]}",
                "timestamp": "2019-10-03T15:59:04.672+0000"
            },
            "configuration": {
                "description": "Goes into alarm if CPU Utilization is out of band",
                "metrics": [{
                    "id": "m1",
                    "metricStat": {
                        "metric": {
                            "namespace": "AWS/EC2",
                            "name": "CPUUtilization",
                            "dimensions": {
                                "InstanceId": "i-12345678901234567"
                            }
                        },
                        "period": 60,
                        "stat": "Average"
                    },
                    "returnData": true
                }, {
                    "id": "ad1",
                    "expression": "ANOMALY_DETECTION_BAND(m1, 0.8)",
                    "label": "CPUUtilization (expected)",
                    "returnData": true
                }]
            }
        }
    }
  """

  private def abnormalityDetectionResult: ScheduledEvent = ScheduledEvent(
    id = "daafc9f1-bddd-c6c9-83af-74971fcfc4ef",
    version = "0",
    account = "123456789012",
    time = Instant.parse("2019-10-03T16:00:04Z"),
    region = "us-east-1",
    resources =
      List("arn:aws:cloudwatch:us-east-1:123456789012:alarm:EC2 CPU Utilization Anomaly"),
    source = "aws.cloudwatch",
    `detail-type` = "CloudWatch Alarm State Change",
    detail = ScheduledEventDetail(
      alarmName = "EC2 CPU Utilization Anomaly",
      operation = None,
      configuration = ScheduledEventConfiguration(
        description = Some("Goes into alarm if CPU Utilization is out of band"),
        metrics = Some(
          List(
            ScheduledEventMetric(
              id = "m1",
              metricStat = Some(
                ScheduledEventMetricStat(
                  metric = Metric(
                    dimensions = Map("InstanceId" -> "i-12345678901234567"),
                    name = "CPUUtilization",
                    namespace = "AWS/EC2"
                  ),
                  period = 60,
                  stat = "Average"
                )),
              returnData = true,
              expression = None,
              label = None
            ),
            ScheduledEventMetric(
              id = "ad1",
              expression = Some("ANOMALY_DETECTION_BAND(m1, 0.8)"),
              label = Some("CPUUtilization (expected)"),
              returnData = true,
              metricStat = None
            )
          )),
        alarmRule = None,
        actionsSuppressor = None,
        actionsSuppressorWaitPeriod = None,
        actionsSuppressorExtensionPeriod = None
      ),
      state = Some(
        ScheduledEventState(
          reason =
            "Thresholds Crossed: 1 out of the last 1 datapoints [0.0 (03/10/19 15:58:00)] was less than the lower thresholds [0.020599444741798756] or greater than the upper thresholds [0.3006915352732461] (minimum 1 datapoint for OK -> ALARM transition).",
          reasonData =
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-03T16:00:04.650+0000\",\"startDate\":\"2019-10-03T15:58:00.000+0000\",\"period\":60,\"recentDatapoints\":[0.0],\"recentLowerThresholds\":[0.020599444741798756],\"recentUpperThresholds\":[0.3006915352732461]}",
          timestamp = OffsetDateTime.parse("2019-10-03T16:00:04.653+0000", formatter),
          value = "ALARM",
          actionsSuppressedBy = None,
          actionsSuppressedReason = None
        )),
      previousState = Some(
        ScheduledEventPreviousState(
          reason =
            "Thresholds Crossed: 1 out of the last 1 datapoints [0.166666666664241 (03/10/19 15:57:00)] was not less than the lower thresholds [0.0206719426210418] or not greater than the upper thresholds [0.30076870222143803] (minimum 1 datapoint for ALARM -> OK transition).",
          reasonData = Some(
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-03T15:59:04.670+0000\",\"startDate\":\"2019-10-03T15:57:00.000+0000\",\"period\":60,\"recentDatapoints\":[0.166666666664241],\"recentLowerThresholds\":[0.0206719426210418],\"recentUpperThresholds\":[0.30076870222143803]}"),
          timestamp = OffsetDateTime.parse("2019-10-03T15:59:04.672+0000", formatter),
          value = "OK"
        ))
    ),
    `replay-name` = None
  )

  // Change of state of combined alarm with suppressor alarm
  private def compoundAlarmSuppressorEvent = json"""
    {
      "version": "0",
      "id": "d3dfc86d-384d-24c8-0345-9f7986db0b80",
      "detail-type": "CloudWatch Alarm State Change",
      "source": "aws.cloudwatch",
      "account": "123456789012",
      "time": "2022-07-22T15:57:45Z",
      "region": "us-east-1",
      "resources": [
          "arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServiceAggregatedAlarm"
      ],
      "detail": {
          "alarmName": "ServiceAggregatedAlarm",
          "state": {
              "actionsSuppressedBy": "WaitPeriod",
              "actionsSuppressedReason": "Actions suppressed by WaitPeriod",
              "value": "ALARM",
              "reason": "arn:aws:cloudwatch:us-east-1:123456789012:alarm:SuppressionDemo.EventBridge.FirstChild transitioned to ALARM at Friday 22 July, 2022 15:57:45 UTC",
              "reasonData": "{\"triggeringAlarms\":[{\"arn\":\"arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServerCpuTooHigh\",\"state\":{\"value\":\"ALARM\",\"timestamp\":\"2022-07-22T15:57:45.394+0000\"}}]}",
              "timestamp": "2022-07-22T15:57:45.394+0000"
          },
          "previousState": {
              "value": "OK",
              "reason": "arn:aws:cloudwatch:us-east-1:123456789012:alarm:SuppressionDemo.EventBridge.Main was created and its alarm rule evaluates to OK",
              "reasonData": "{\"triggeringAlarms\":[{\"arn\":\"arn:aws:cloudwatch:us-east-1:123456789012:alarm:TotalNetworkTrafficTooHigh\",\"state\":{\"value\":\"OK\",\"timestamp\":\"2022-07-14T16:28:57.770+0000\"}},{\"arn\":\"arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServerCpuTooHigh\",\"state\":{\"value\":\"OK\",\"timestamp\":\"2022-07-14T16:28:54.191+0000\"}}]}",
              "timestamp": "2022-07-22T15:56:14.552+0000"
          },
          "configuration": {
              "alarmRule": "ALARM(ServerCpuTooHigh) OR ALARM(TotalNetworkTrafficTooHigh)",
              "actionsSuppressor": "ServiceMaintenanceAlarm",
              "actionsSuppressorWaitPeriod": 120,
              "actionsSuppressorExtensionPeriod": 180
          }
      }
  }
  """

  private def compoundAlarmSuppressorResult: ScheduledEvent = ScheduledEvent(
    id = "d3dfc86d-384d-24c8-0345-9f7986db0b80",
    version = "0",
    account = "123456789012",
    time = Instant.parse("2022-07-22T15:57:45Z"),
    region = "us-east-1",
    resources = List("arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServiceAggregatedAlarm"),
    source = "aws.cloudwatch",
    `detail-type` = "CloudWatch Alarm State Change",
    detail = ScheduledEventDetail(
      alarmName = "ServiceAggregatedAlarm",
      operation = None,
      state = Some(
        ScheduledEventState(
          actionsSuppressedBy = Some("WaitPeriod"),
          actionsSuppressedReason = Some("Actions suppressed by WaitPeriod"),
          value = "ALARM",
          reason =
            "arn:aws:cloudwatch:us-east-1:123456789012:alarm:SuppressionDemo.EventBridge.FirstChild transitioned to ALARM at Friday 22 July, 2022 15:57:45 UTC",
          reasonData =
            "{\"triggeringAlarms\":[{\"arn\":\"arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServerCpuTooHigh\",\"state\":{\"value\":\"ALARM\",\"timestamp\":\"2022-07-22T15:57:45.394+0000\"}}]}",
          timestamp = OffsetDateTime.parse("2022-07-22T15:57:45.394+0000", formatter)
        )
      ),
      previousState = Some(
        ScheduledEventPreviousState(
          value = "OK",
          reason =
            "arn:aws:cloudwatch:us-east-1:123456789012:alarm:SuppressionDemo.EventBridge.Main was created and its alarm rule evaluates to OK",
          reasonData = Some(
            "{\"triggeringAlarms\":[{\"arn\":\"arn:aws:cloudwatch:us-east-1:123456789012:alarm:TotalNetworkTrafficTooHigh\",\"state\":{\"value\":\"OK\",\"timestamp\":\"2022-07-14T16:28:57.770+0000\"}},{\"arn\":\"arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServerCpuTooHigh\",\"state\":{\"value\":\"OK\",\"timestamp\":\"2022-07-14T16:28:54.191+0000\"}}]}"),
          timestamp = OffsetDateTime.parse("2022-07-22T15:56:14.552+0000", formatter)
        )
      ),
      configuration = ScheduledEventConfiguration(
        alarmRule = Some("ALARM(ServerCpuTooHigh) OR ALARM(TotalNetworkTrafficTooHigh)"),
        actionsSuppressor = Some("ServiceMaintenanceAlarm"),
        actionsSuppressorWaitPeriod = Some(120),
        actionsSuppressorExtensionPeriod = Some(180),
        metrics = None,
        description = None
      )
    ),
    `replay-name` = None
  )
}
