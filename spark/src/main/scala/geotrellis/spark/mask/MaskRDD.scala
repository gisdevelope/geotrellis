/*
 * Copyright 2019 Azavea
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

package geotrellis.spark.mask

import geotrellis.vector._
import geotrellis.layer._
import geotrellis.raster.mask._
import geotrellis.layer.mask.Mask
import geotrellis.layer.mask.Mask.Options
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


object MaskRDD extends Mask {
  private def _mask[
    K: SpatialComponent: ClassTag,
    V,
    M: GetComponent[*, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], masker: (Extent, V) => Option[V]): RDD[(K, V)] with Metadata[M] = {
    val mapTransform = rdd.metadata.getComponent[LayoutDefinition].mapTransform
    val masked =
      rdd.mapPartitions({ partition =>
        partition.flatMap { case (k, tile) =>
          val key = k.getComponent[SpatialKey]
          val tileExtent = mapTransform(key)
          masker(tileExtent, tile).map { result =>
            (k, result)
          }
        }
      }, preservesPartitioning = true)
    ContextRDD(masked, rdd.metadata)
  }

  def apply[
    K: SpatialComponent: ClassTag,
    V: * => TileMaskMethods[V],
    M: GetComponent[*, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], geoms: Traversable[Polygon], options: Options): RDD[(K, V)] with Metadata[M] =
    _mask(rdd, { case (tileExtent, tile) =>
      val tileGeoms = geoms.flatMap { g =>
        val intersections = g.intersectionSafe(tileExtent).toGeometry()
        eliminateNotQualified(intersections)
      }
      if(tileGeoms.isEmpty && options.filterEmptyTiles) { None }
      else {
        Some(tile.mask(tileExtent, tileGeoms, options.rasterizerOptions))
      }
    })

  /** Masks this raster by the given MultiPolygons. */
  def apply[
    K: SpatialComponent: ClassTag,
    V: * => TileMaskMethods[V],
    M: GetComponent[*, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], geoms: Traversable[MultiPolygon], options: Options)(implicit d: DummyImplicit): RDD[(K, V)] with Metadata[M] =
    _mask(rdd, { case (tileExtent, tile) =>
      val tileGeoms = geoms.flatMap { g =>
        val intersections = g.intersectionSafe(tileExtent).toGeometry()
        eliminateNotQualified(intersections)
      }
      if(tileGeoms.isEmpty && options.filterEmptyTiles) { None }
      else {
        Some(tile.mask(tileExtent, tileGeoms, options.rasterizerOptions))
      }
    })

  /** Masks this raster by the given Extent. */
  def apply[
    K: SpatialComponent: ClassTag,
    V: * => TileMaskMethods[V],
    M: GetComponent[*, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], ext: Extent, options: Options): RDD[(K, V)] with Metadata[M] =
    _mask(rdd, { case (tileExtent, tile) =>
      val tileExts = ext.intersection(tileExtent)
      tileExts match {
        case Some(intersected) if intersected.area != 0 => Some(tile.mask(tileExtent, intersected.toPolygon(), options.rasterizerOptions))
        case _ if options.filterEmptyTiles => None
        case _ => Some(tile.mask(tileExtent, Extent(0.0, 0.0, 0.0, 0.0), options.rasterizerOptions))
      }
    })

  def apply[
    K: SpatialComponent: ClassTag,
    V: * => TileMaskMethods[V],
    M: GetComponent[*, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], ext: Extent): RDD[(K, V)] with Metadata[M] = {
    val options = Options.DEFAULT
    _mask(rdd, { case (tileExtent, tile) =>
      val tileExts = ext.intersection(tileExtent)
      tileExts match {
        case Some(intersected) if intersected.area != 0 => Some(tile.mask(tileExtent, intersected.toPolygon(), options.rasterizerOptions))
        case _ if options.filterEmptyTiles => None
        case _ => Some(tile.mask(tileExtent, Extent(0.0, 0.0, 0.0, 0.0), options.rasterizerOptions))
      }
    })
  }
}
