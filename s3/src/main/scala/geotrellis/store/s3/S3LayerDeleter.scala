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

package geotrellis.store.s3

import geotrellis.store._

import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.S3Client
import org.log4s._

import scala.collection.JavaConverters._

class S3LayerDeleter(
  val attributeStore: AttributeStore,
  s3Client: => S3Client,
  val requestLimit: Int = 1000
  ) extends LayerDeleter[LayerId] {
  @transient private[this] lazy val logger = getLogger

  def delete(id: LayerId): Unit = {
    try {
      val header = attributeStore.readHeader[S3LayerHeader](id)
      val bucket = header.bucket
      val prefix = header.key + "/"
      val listRequest = ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(prefix)
        .build()

      val iter = s3Client
        .listObjectsV2Paginator(listRequest)
        .contents
        .asScala

      if (iter.isEmpty) throw new LayerDeleteError(id)

      iter
        .map { s3obj => ObjectIdentifier.builder.key(s3obj.key).build() }
        .grouped(requestLimit).foreach{ objIdentifiersChunk =>
          val deleteDefinition =
            Delete.builder()
              .objects(objIdentifiersChunk.toSeq: _*)
              .build()
          val deleteRequest =
            DeleteObjectsRequest.builder()
              .bucket(bucket)
              .delete(deleteDefinition)
              .build()
          s3Client.deleteObjects(deleteRequest)
      }
      attributeStore.delete(id)
    } catch {
      case e: AttributeNotFoundError =>
        logger.info(s"Metadata for $id was not found. Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
      case e: NoSuchBucketException =>
        logger.info(s"Metadata for $id was not found (no such bucket). Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
    }
  }
}

object S3LayerDeleter {
  def apply(attributeStore: AttributeStore, s3Client: => S3Client): S3LayerDeleter =
    new S3LayerDeleter(attributeStore, s3Client)

  def apply(attributeStore: AttributeStore, s3Client: => S3Client, requestLimit: Int): S3LayerDeleter =
    new S3LayerDeleter(attributeStore, s3Client, requestLimit)

  def apply(bucket: String, prefix: String, s3Client: => S3Client): S3LayerDeleter = {
    val attStore = S3AttributeStore(bucket, prefix, s3Client)
    apply(attStore, s3Client)
  }
}
