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

package geotrellis.spark.store.hadoop

import geotrellis.layer.SpatialComponent
import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render.{Jpg, Png}
import geotrellis.raster.resample._
import geotrellis.store._
import geotrellis.store.avro._
import geotrellis.store.hadoop._
import geotrellis.util.MethodExtensions

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark._
import org.apache.spark.rdd._
import _root_.io.circe._

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class HadoopSparkContextMethodsWrapper(val sc: SparkContext) extends HadoopSparkContextMethods
  implicit class withSaveBytesToHadoopMethods[K](rdd: RDD[(K, Array[Byte])]) extends SaveBytesToHadoopMethods[K](rdd)
  implicit class withSaveToHadoopMethods[K,V](rdd: RDD[(K,V)]) extends SaveToHadoopMethods[K, V](rdd)

  implicit class withJpgHadoopSparkWriteMethods(val self: Jpg) extends JpgHadoopSparkWriteMethods(self)

  implicit class withPngHadoopSparkWriteMethods(val self: Png) extends PngHadoopSparkWriteMethods(self)

  implicit class withGeoTiffSparkHadoopWriteMethods[T <: CellGrid[Int]](val self: GeoTiff[T]) extends GeoTiffHadoopSparkWriteMethods[T](self)

  implicit class withHadoopAttributeStoreMethods(val self: HadoopAttributeStore.type) extends MethodExtensions[HadoopAttributeStore.type] {
    def apply(rootPath: String)(implicit sc: SparkContext): HadoopAttributeStore =
      apply(new Path(rootPath))(sc)

    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopAttributeStore =
      HadoopAttributeStore(rootPath, sc.hadoopConfiguration)
  }

  implicit class withHadoopLayerCopierMethods(val self: HadoopLayerCopier.type) extends MethodExtensions[HadoopLayerCopier.type] {
    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerCopier =
      HadoopLayerCopier.apply(rootPath, HadoopAttributeStore(rootPath, sc.hadoopConfiguration))
  }

  implicit class withHadoopLayerDeleterMethods(val self: HadoopLayerDeleter.type) extends MethodExtensions[HadoopLayerDeleter.type] {
    def apply(attributeStore: AttributeStore)(implicit sc: SparkContext): HadoopLayerDeleter =
      HadoopLayerDeleter(attributeStore, sc.hadoopConfiguration)

    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerDeleter =
      HadoopLayerDeleter(HadoopAttributeStore(rootPath, new Configuration), sc.hadoopConfiguration)
  }

  implicit class withHadoopCollectionLayerReaderMethods(val self: HadoopCollectionLayerReader.type) extends MethodExtensions[HadoopCollectionLayerReader.type] {
    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCollectionLayerReader =
      new HadoopCollectionLayerReader(HadoopAttributeStore(rootPath), sc.hadoopConfiguration)

    def apply(attributeStore: AttributeStore)(implicit sc: SparkContext): HadoopCollectionLayerReader =
      new HadoopCollectionLayerReader(attributeStore, sc.hadoopConfiguration)
  }

  implicit class withHadoopValueReaderMethods(val self: HadoopValueReader.type) extends MethodExtensions[HadoopValueReader.type] {
    def apply[K: AvroRecordCodec: Decoder: ClassTag, V: AvroRecordCodec](
      attributeStore: AttributeStore,
      layerId: LayerId
    )(implicit sc: SparkContext): Reader[K, V] =
      new HadoopValueReader(attributeStore, sc.hadoopConfiguration).reader[K, V](layerId)

    def apply[K: AvroRecordCodec: Decoder: SpatialComponent: ClassTag, V <: CellGrid[Int]: AvroRecordCodec: * => TileResampleMethods[V]](
      attributeStore: AttributeStore,
      layerId: LayerId,
      resampleMethod: ResampleMethod
    )(implicit sc: SparkContext): Reader[K, V] =
      new HadoopValueReader(attributeStore, sc.hadoopConfiguration).overzoomingReader[K, V](layerId, resampleMethod)

    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopValueReader =
      HadoopValueReader(HadoopAttributeStore(rootPath, sc.hadoopConfiguration))
  }
}
