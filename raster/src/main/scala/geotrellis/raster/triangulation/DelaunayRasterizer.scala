package geotrellis.raster.triangulation

import geotrellis.raster._
import geotrellis.vector.triangulation._

object DelaunayRasterizer {
  /**
   * Produces a Tile with shape given by a RasterExtent where pixels are linearly interpolated
   * values of the z-coordinates of each triangle vertex.
   */
  def rasterizeTriangles[V,F](re: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType)(triangles: Traversable[((V,V,V), HalfEdge[V,F])])(implicit verts: V => LightPoint): MutableArrayTile = {
    val w = re.cellwidth
    val h = re.cellheight
    val cols = re.cols
    val rows = re.rows
    val result = ArrayTile.empty(cellType, cols, rows)

    def xAtYForEdge(yval: Double)(e: HalfEdge[V, F]): Double = {
      val src = verts(e.src)
      val dest = verts(e.vert)
      val t = (yval - dest.y) / (src.y - dest.y)
      dest.x + t * (src.x - dest.x)
    }

    def rasterizeTriangle(tri: HalfEdge[V, F]): Unit = {
      import Predicates._

      if (!isCCW(tri.vert, tri.next.vert, tri.next.next.vert)) {
        println(s"Triangle ${(tri.vert, tri.next.vert, tri.next.next.vert)} does not have a CCW winding!")
        return ()
      }

      val es = List(tri, tri.next, tri.next.next)
      val v1 :: v2 :: v3 :: _ = es.map{ e => verts(e.vert) }
      val leftes  = es.filter { e => verts(e.vert).y < verts(e.src).y }
      val rightes = es.filter { e => verts(e.vert).y > verts(e.src).y }
      val ys = es.map { e => verts(e.vert).y }
      val (ymin, ymax) = (ys.min, ys.max)

      val scanrow0 = math.max(math.ceil((ymin - re.extent.ymin) / h - 0.5), 0)
      var scany = re.extent.ymin + scanrow0 * h + h / 2
      while (scany < re.extent.ymax && scany < ymax) {
        val xmin = leftes.map(xAtYForEdge(scany)(_)).max
        val xmax = rightes.map(xAtYForEdge(scany)(_)).min

        val scancol0 = math.max(math.ceil((xmin - re.extent.xmin) / w - 0.5), 0)
        var scanx = re.extent.xmin + scancol0 * w + w / 2
        while (scanx < re.extent.xmax && scanx < xmax) {
          val z = {
            val determinant = (v2.y - v3.y) * (v1.x - v3.x) + (v3.x - v2.x) * (v1.y - v3.y)
            val lambda1 = ((v2.y - v3.y) * (scanx - v3.x) + (v3.x - v2.x) * (scany - v3.y)) / determinant
            val lambda2 = ((v3.y - v1.y) * (scanx - v3.x) + (v1.x - v3.x) * (scany - v3.y)) / determinant
            val lambda3 = 1.0 - lambda1 - lambda2

            lambda1 * v1.z + lambda2 * v2.z + lambda3 * v3.z
          }
          val col = ((scanx - re.extent.xmin) / re.cellwidth).toInt
          val row = ((re.extent.ymax - scany) / re.cellheight).toInt
          if(0 <= col && col < cols &&
             0 <= row && row < rows) {
            result.setDouble(col, row, z)
          }

          scanx += w
        }

        scany += h
      }
    }

    triangles.foreach { case (idx, tri) =>
      rasterizeTriangle(tri)
    }

    result
  }

  def rasterizeFastDelaunay(re: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType)(dt: FastDelaunay) = {
    implicit val trans = dt.verts(_)
    rasterizeTriangles(re, cellType)(dt.triangles)
  }

  // def rasterizeStitchedDelaunay(re: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType)(dt: StitchedDelaunay) = {
  //   implicit val trans = { key: dt.VKey => dt.vertMap(key.dir)(key.idx) }
  //   rasterizeTriangles(re, cellType)(dt.triangles.filter{ case (idx, _) => {
  //     val (a, b, c) = idx
  //     a.dir != b.dir || b.dir != c.dir
  //   }})
  // }
}
