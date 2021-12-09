package feral.lambda

import io.circe.Decoder
import io.circe.Json

final case class AttributeValue(
    b: Option[String],
    bs: Option[String],
    bool: Option[Boolean],
    l: Option[List[AttributeValue]],
    m: Option[Map[String, AttributeValue]],
    n: Option[String],
    ns: Option[List[String]],
    nul: Option[true],
    s: Option[String],
    ss: Option[List[String]]
)

object AttributeValue {
  implicit val decoder: Decoder[AttributeValue] = Decoder.forProduct10(
    "B",
    "BS",
    "BOOL",
    "L",
    "M",
    "N",
    "NS",
    "NULL",
    "S",
    "SS"
  )(AttributeValue.apply)
}

final case class StreamRecord(
    approximateCreationDateTime: Option[Double],
    keys: Option[Map[String, AttributeValue]],
    newImage: Option[Map[String, AttributeValue]],
    oldImage: Option[Map[String, AttributeValue]],
    sequenceNumber: Option[String],
    sizeBytes: Option[Double],
    streamViewType: Option[String]
)

object StreamRecord {
  implicit val decoder: Decoder[StreamRecord] = Decoder.forProduct7(
    "ApproximateCreationDateTime",
    "Keys",
    "NewImage",
    "OldImage",
    "SequenceNumber",
    "SizeBytes",
    "StreamViewType"
  )(StreamRecord.apply)
}

final case class DynamoDBRecord(
    awsRegion: Option[String],
    dynamodb: Option[StreamRecord],
    eventID: Option[String],
    eventName: Option[String],
    eventSource: Option[String],
    eventSourceARN: Option[String],
    eventVersion: Option[String],
    userIdentity: Option[Json]
)

object DynamoDBRecord {
  implicit val decoder: Decoder[DynamoDBRecord] = Decoder.forProduct8(
    "awsRegion",
    "dynamodb",
    "eventID",
    "eventName",
    "eventSource",
    "eventSourceARN",
    "eventVersion",
    "userIdentity"
  )(DynamoDBRecord.apply)
}

final case class DynamoDBStreamEvent(
    Records: List[DynamoDBRecord]
)

object DynamoDBStreamEvent {
  implicit val decoder: Decoder[DynamoDBStreamEvent] =
    Decoder.forProduct1("Records")(DynamoDBStreamEvent.apply)
}
