package feral.lambda.events

import java.time.Instant

import io.circe.literal._
import io.circe.{Json, JsonObject}

import munit.FunSuite

class ScheduledEventSuite extends FunSuite {

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

  test("Decoding the creation of compound alarms") {
    assertEquals(
      compoundAlarmCreationEvent.as[ScheduledEvent].toTry.get,
      compoundAlarmCreationResult
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
    detail = JsonObject.apply(
      "alarmName" -> Json.fromString("ServerCpuTooHigh"),
      "configuration" -> JsonObject
        .apply(
          "description" -> Json.fromString(
            "Goes into alarm when server CPU utilization is too high!"),
          "metrics" -> Json.arr(
            JsonObject
              .apply(
                "id" -> Json.fromString("30b6c6b2-a864-43a2-4877-c09a1afc3b87"),
                "metricStat" -> JsonObject
                  .apply(
                    "metric" -> JsonObject
                      .apply(
                        "dimensions" -> JsonObject
                          .apply("InstanceId" -> Json.fromString("i-12345678901234567"))
                          .toJson,
                        "name" -> Json.fromString("CPUUtilization"),
                        "namespace" -> Json.fromString("AWS/EC2")
                      )
                      .toJson,
                    "period" -> Json.fromInt(300),
                    "stat" -> Json.fromString("Average")
                  )
                  .toJson,
                "returnData" -> Json.fromBoolean(true)
              )
              .toJson
          )
        )
        .toJson,
      "previousState" -> JsonObject
        .apply(
          "reason" -> Json.fromString(
            "Threshold Crossed: 1 out of the last 1 datapoints [0.0666851903306472 (01/10/19 13:46:00)] was not greater than the threshold (50.0) (minimum 1 datapoint for ALARM -> OK transition)."),
          "reasonData" -> Json.fromString(
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-01T13:56:40.985+0000\",\"startDate\":\"2019-10-01T13:46:00.000+0000\",\"statistic\":\"Average\",\"period\":300,\"recentDatapoints\":[0.0666851903306472],\"threshold\":50.0}"
          ),
          "timestamp" -> Json.fromString("2019-10-01T13:56:40.987+0000"),
          "value" -> Json.fromString("OK")
        )
        .toJson,
      "state" -> JsonObject
        .apply(
          "reason" -> Json.fromString(
            "Threshold Crossed: 1 out of the last 1 datapoints [99.50160229693434 (02/10/19 16:59:00)] was greater than the threshold (50.0) (minimum 1 datapoint for OK -> ALARM transition)."
          ),
          "reasonData" -> Json.fromString(
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-02T17:04:40.985+0000\",\"startDate\":\"2019-10-02T16:59:00.000+0000\",\"statistic\":\"Average\",\"period\":300,\"recentDatapoints\":[99.50160229693434],\"threshold\":50.0}"),
          "timestamp" -> Json.fromString("2019-10-02T17:04:40.989+0000"),
          "value" -> Json.fromString("ALARM")
        )
        .toJson
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
    detail = JsonObject.apply(
      "alarmName" -> Json.fromString("TotalNetworkTrafficTooHigh"),
      "configuration" -> JsonObject
        .apply(
          "description" -> Json.fromString(
            "Goes into alarm if total network traffic exceeds 10Kb"),
          "metrics" -> Json.arr(
            JsonObject
              .apply(
                "expression" -> Json.fromString("SUM(METRICS())"),
                "id" -> Json.fromString("e1"),
                "label" -> Json.fromString("Total Network Traffic"),
                "returnData" -> Json.fromBoolean(true)
              )
              .toJson,
            JsonObject
              .apply(
                "id" -> Json.fromString("m1"),
                "metricStat" -> JsonObject
                  .apply(
                    "metric" -> JsonObject
                      .apply(
                        "dimensions" -> JsonObject
                          .apply("InstanceId" -> Json.fromString("i-12345678901234567"))
                          .toJson,
                        "name" -> Json.fromString("NetworkIn"),
                        "namespace" -> Json.fromString("AWS/EC2")
                      )
                      .toJson,
                    "period" -> Json.fromInt(300),
                    "stat" -> Json.fromString("Maximum")
                  )
                  .toJson,
                "returnData" -> Json.fromBoolean(false)
              )
              .toJson,
            JsonObject
              .apply(
                "id" -> Json.fromString("m2"),
                "metricStat" -> JsonObject
                  .apply(
                    "metric" -> JsonObject
                      .apply(
                        "dimensions" -> JsonObject
                          .apply("InstanceId" -> Json.fromString("i-12345678901234567"))
                          .toJson,
                        "name" -> Json.fromString("NetworkOut"),
                        "namespace" -> Json.fromString("AWS/EC2")
                      )
                      .toJson,
                    "period" -> Json.fromInt(300),
                    "stat" -> Json.fromString("Maximum")
                  )
                  .toJson,
                "returnData" -> Json.fromBoolean(false)
              )
              .toJson
          )
        )
        .toJson,
      "previousState" -> JsonObject
        .apply(
          "reason" -> Json.fromString("Unchecked: Initial alarm creation"),
          "timestamp" -> Json.fromString("2019-10-02T17:20:03.642+0000"),
          "value" -> Json.fromString("INSUFFICIENT_DATA")
        )
        .toJson,
      "state" -> JsonObject
        .apply(
          "reason" -> Json.fromString(
            "Threshold Crossed: 1 out of the last 1 datapoints [45628.0 (02/10/19 17:10:00)] was greater than the threshold (10000.0) (minimum 1 datapoint for OK -> ALARM transition)."
          ),
          "reasonData" -> Json.fromString(
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-02T17:20:48.551+0000\",\"startDate\":\"2019-10-02T17:10:00.000+0000\",\"period\":300,\"recentDatapoints\":[45628.0],\"threshold\":10000.0}"
          ),
          "timestamp" -> Json.fromString("2019-10-02T17:20:48.554+0000"),
          "value" -> Json.fromString("ALARM")
        )
        .toJson
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
    detail = JsonObject.apply(
      "alarmName" -> Json.fromString("EC2 CPU Utilization Anomaly"),
      "state" -> JsonObject
        .apply(
          "value" -> Json.fromString("ALARM"),
          "reason" -> Json.fromString(
            "Thresholds Crossed: 1 out of the last 1 datapoints [0.0 (03/10/19 15:58:00)] was less than the lower thresholds [0.020599444741798756] or greater than the upper thresholds [0.3006915352732461] (minimum 1 datapoint for OK -> ALARM transition)."
          ),
          "reasonData" -> Json.fromString(
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-03T16:00:04.650+0000\",\"startDate\":\"2019-10-03T15:58:00.000+0000\",\"period\":60,\"recentDatapoints\":[0.0],\"recentLowerThresholds\":[0.020599444741798756],\"recentUpperThresholds\":[0.3006915352732461]}"
          ),
          "timestamp" -> Json.fromString("2019-10-03T16:00:04.653+0000")
        )
        .toJson,
      "previousState" -> JsonObject
        .apply(
          "value" -> Json.fromString("OK"),
          "reason" -> Json.fromString(
            "Thresholds Crossed: 1 out of the last 1 datapoints [0.166666666664241 (03/10/19 15:57:00)] was not less than the lower thresholds [0.0206719426210418] or not greater than the upper thresholds [0.30076870222143803] (minimum 1 datapoint for ALARM -> OK transition)."),
          "reasonData" -> Json.fromString(
            "{\"version\":\"1.0\",\"queryDate\":\"2019-10-03T15:59:04.670+0000\",\"startDate\":\"2019-10-03T15:57:00.000+0000\",\"period\":60,\"recentDatapoints\":[0.166666666664241],\"recentLowerThresholds\":[0.0206719426210418],\"recentUpperThresholds\":[0.30076870222143803]}"),
          "timestamp" -> Json.fromString("2019-10-03T15:59:04.672+0000")
        )
        .toJson,
      "configuration" -> JsonObject
        .apply(
          "description" -> Json.fromString("Goes into alarm if CPU Utilization is out of band"),
          "metrics" -> Json.arr(
            JsonObject
              .apply(
                "id" -> Json.fromString("m1"),
                "metricStat" -> JsonObject
                  .apply(
                    "metric" -> JsonObject
                      .apply(
                        "namespace" -> Json.fromString("AWS/EC2"),
                        "name" -> Json.fromString("CPUUtilization"),
                        "dimensions" -> JsonObject
                          .apply("InstanceId" -> Json.fromString("i-12345678901234567"))
                          .toJson
                      )
                      .toJson,
                    "period" -> Json.fromInt(60),
                    "stat" -> Json.fromString("Average")
                  )
                  .toJson,
                "returnData" -> Json.fromBoolean(true)
              )
              .toJson,
            JsonObject
              .apply(
                "id" -> Json.fromString("ad1"),
                "expression" -> Json.fromString("ANOMALY_DETECTION_BAND(m1, 0.8)"),
                "label" -> Json.fromString("CPUUtilization (expected)"),
                "returnData" -> Json.fromBoolean(true)
              )
              .toJson
          )
        )
        .toJson
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
    detail = JsonObject.apply(
      "alarmName" -> Json.fromString("ServiceAggregatedAlarm"),
      "state" -> JsonObject
        .apply(
          "actionsSuppressedBy" -> Json.fromString("WaitPeriod"),
          "actionsSuppressedReason" -> Json.fromString("Actions suppressed by WaitPeriod"),
          "value" -> Json.fromString("ALARM"),
          "reason" -> Json.fromString(
            "arn:aws:cloudwatch:us-east-1:123456789012:alarm:SuppressionDemo.EventBridge.FirstChild transitioned to ALARM at Friday 22 July, 2022 15:57:45 UTC"
          ),
          "reasonData" -> Json.fromString(
            "{\"triggeringAlarms\":[{\"arn\":\"arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServerCpuTooHigh\",\"state\":{\"value\":\"ALARM\",\"timestamp\":\"2022-07-22T15:57:45.394+0000\"}}]}"
          ),
          "timestamp" -> Json.fromString("2022-07-22T15:57:45.394+0000")
        )
        .toJson,
      "previousState" -> JsonObject
        .apply(
          "value" -> Json.fromString("OK"),
          "reason" -> Json.fromString(
            "arn:aws:cloudwatch:us-east-1:123456789012:alarm:SuppressionDemo.EventBridge.Main was created and its alarm rule evaluates to OK"
          ),
          "reasonData" -> Json.fromString(
            "{\"triggeringAlarms\":[{\"arn\":\"arn:aws:cloudwatch:us-east-1:123456789012:alarm:TotalNetworkTrafficTooHigh\",\"state\":{\"value\":\"OK\",\"timestamp\":\"2022-07-14T16:28:57.770+0000\"}},{\"arn\":\"arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServerCpuTooHigh\",\"state\":{\"value\":\"OK\",\"timestamp\":\"2022-07-14T16:28:54.191+0000\"}}]}"
          ),
          "timestamp" -> Json.fromString("2022-07-22T15:56:14.552+0000")
        )
        .toJson,
      "configuration" -> JsonObject
        .apply(
          "alarmRule" -> Json.fromString(
            "ALARM(ServerCpuTooHigh) OR ALARM(TotalNetworkTrafficTooHigh)"),
          "actionsSuppressor" -> Json.fromString("ServiceMaintenanceAlarm"),
          "actionsSuppressorWaitPeriod" -> Json.fromInt(120),
          "actionsSuppressorExtensionPeriod" -> Json.fromInt(180)
        )
        .toJson
    ),
    `replay-name` = None
  )

  // Creating compound alarms
  private def compoundAlarmCreationEvent = json"""
    {
      "version": "0",
      "id": "91535fdd-1e9c-849d-624b-9a9f2b1d09d0",
      "detail-type": "CloudWatch Alarm Configuration Change",
      "source": "aws.cloudwatch",
      "account": "123456789012",
      "time": "2022-03-03T17:06:22Z",
      "region": "us-east-1",
      "resources": [
          "arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServiceAggregatedAlarm"
      ],
      "detail": {
          "alarmName": "ServiceAggregatedAlarm",
          "operation": "create",
          "state": {
              "value": "INSUFFICIENT_DATA",
              "timestamp": "2022-03-03T17:06:22.289+0000"
          },
          "configuration": {
              "alarmRule": "ALARM(ServerCpuTooHigh) OR ALARM(TotalNetworkTrafficTooHigh)",
              "alarmName": "ServiceAggregatedAlarm",
              "description": "Aggregated monitor for instance",
              "actionsEnabled": true,
              "timestamp": "2022-03-03T17:06:22.289+0000",
              "okActions": [],
              "alarmActions": [],
              "insufficientDataActions": []
          }
      }
  }
  """

  private def compoundAlarmCreationResult: ScheduledEvent = ScheduledEvent(
    id = "91535fdd-1e9c-849d-624b-9a9f2b1d09d0",
    version = "0",
    account = "123456789012",
    time = Instant.parse("2022-03-03T17:06:22Z"),
    region = "us-east-1",
    resources = List("arn:aws:cloudwatch:us-east-1:123456789012:alarm:ServiceAggregatedAlarm"),
    source = "aws.cloudwatch",
    `detail-type` = "CloudWatch Alarm Configuration Change",
    detail = JsonObject.apply(
      "alarmName" -> Json.fromString("ServiceAggregatedAlarm"),
      "operation" -> Json.fromString("create"),
      "state" -> JsonObject
        .apply(
          "value" -> Json.fromString("INSUFFICIENT_DATA"),
          "timestamp" -> Json.fromString("2022-03-03T17:06:22.289+0000")
        )
        .toJson,
      "configuration" -> JsonObject
        .apply(
          "alarmRule" -> Json.fromString(
            "ALARM(ServerCpuTooHigh) OR ALARM(TotalNetworkTrafficTooHigh)"),
          "alarmName" -> Json.fromString("ServiceAggregatedAlarm"),
          "description" -> Json.fromString("Aggregated monitor for instance"),
          "actionsEnabled" -> Json.fromBoolean(true),
          "timestamp" -> Json.fromString("2022-03-03T17:06:22.289+0000"),
          "okActions" -> Json.arr(),
          "alarmActions" -> Json.arr(),
          "insufficientDataActions" -> Json.arr()
        )
        .toJson
    ),
    `replay-name` = None
  )
}
