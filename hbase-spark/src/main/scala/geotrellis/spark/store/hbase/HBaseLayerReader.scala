/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.store.hbase

import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.avro._
import geotrellis.store.hbase._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.util._

import org.apache.spark.SparkContext
import io.circe._

import scala.reflect._

class HBaseLayerReader(val attributeStore: AttributeStore, instance: HBaseInstance)(implicit sc: SparkContext)
    extends FilteringLayerReader[LayerId] {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: AvroRecordCodec: Boundable: Decoder: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: Decoder: Component[*, Bounds[K]]
  ](id: LayerId, tileQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HBaseLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = tileQuery(metadata)
    val layerMetadata = metadata.setComponent[Bounds[K]](queryKeyBounds.foldLeft(EmptyBounds: Bounds[K])(_ combine _))

    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)

    val rdd = HBaseRDDReader.read[K, V](instance, header.tileTable, id, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextRDD(rdd, layerMetadata)
  }
}

object HBaseLayerReader {
  def apply(instance: HBaseInstance)(implicit sc: SparkContext): HBaseLayerReader =
    new HBaseLayerReader(HBaseAttributeStore(instance), instance)(sc)

  def apply(attributeStore: HBaseAttributeStore)(implicit sc: SparkContext): HBaseLayerReader =
    new HBaseLayerReader(attributeStore, attributeStore.instance)
}
