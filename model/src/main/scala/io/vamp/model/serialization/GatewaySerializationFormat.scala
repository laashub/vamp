package io.vamp.model.serialization

import io.vamp.model.artifact._
import org.json4s.JsonAST.JString
import org.json4s._

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

object GatewaySerializationFormat extends io.vamp.common.json.SerializationFormat {

  override def customSerializers = super.customSerializers :+
    new GatewaySerializer() :+
    new ClusterGatewaySerializer() :+
    new RoutingStickySerializer() :+
    new RouteSerializer() :+
    new FilterSerializer()
}

class GatewaySerializer() extends ArtifactSerializer[Gateway] with AbstractGatewaySerializer {
  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = serializeGateway
}

class ClusterGatewaySerializer() extends ArtifactSerializer[ClusterGateway] with AbstractGatewaySerializer {
  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = serializeGateway
}

trait AbstractGatewaySerializer extends ReferenceSerialization {

  def serializeGateway(implicit format: Formats): PartialFunction[Any, JValue] = {
    case gateway: AbstractGateway ⇒
      val list = new ArrayBuffer[JField]

      gateway match {
        case defaultGateway: Gateway ⇒
          list += JField("name", JString(defaultGateway.name))
          list += JField("port", JString(defaultGateway.port.value.get))
        case _ ⇒
      }

      list += JField("sticky", Extraction.decompose(gateway.sticky))
      list += JField("routes", Extraction.decompose {
        gateway.routes.map { route ⇒
          route.path -> route
        } toMap
      })

      new JObject(list.toList)
  }
}

class RoutingStickySerializer extends CustomSerializer[AbstractGateway.Sticky.Value](format ⇒ ({
  case JString(sticky) ⇒ AbstractGateway.Sticky.byName(sticky).getOrElse(throw new UnsupportedOperationException(s"Cannot deserialize sticky value: $sticky"))
}, {
  case sticky: AbstractGateway.Sticky.Value ⇒ JString(sticky.toString.toLowerCase)
}))

class RouteSerializer extends ArtifactSerializer[Route] with ReferenceSerialization {
  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case routing: RouteReference ⇒ serializeReference(routing)
    case routing: DefaultRoute ⇒
      val list = new ArrayBuffer[JField]
      if (routing.name.nonEmpty)
        list += JField("name", JString(routing.name))
      if (routing.weight.nonEmpty)
        list += JField("weight", JInt(routing.weight.get))
      list += JField("filters", Extraction.decompose(routing.filters))
      new JObject(list.toList)
  }
}

class FilterSerializer extends ArtifactSerializer[Filter] with ReferenceSerialization {
  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case filter: FilterReference ⇒ serializeReference(filter)
    case filter: DefaultFilter ⇒
      val list = new ArrayBuffer[JField]
      if (filter.name.nonEmpty)
        list += JField("name", JString(filter.name))
      list += JField("condition", JString(filter.condition))
      new JObject(list.toList)
  }
}