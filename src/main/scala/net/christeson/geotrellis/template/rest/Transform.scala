package net.christeson.geotrellis.template

import geotrellis._
import geotrellis.feature._

import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.opengis.referencing.crs.{CoordinateReferenceSystem => Crs}
import org.opengis.referencing.operation.MathTransform

import scala.collection.mutable

import com.vividsolutions.jts.{ geom => jts }

object Projections {
  val RRVUTM = CRS.parseWKT(
    """
      PROJCS["WGS 84 / UTM zone 14N",
          GEOGCS["WGS 84",
              DATUM["WGS_1984",
                  SPHEROID["WGS 84",6378137,298.257223563,
                      AUTHORITY["EPSG","7030"]],
                  AUTHORITY["EPSG","6326"]],
              PRIMEM["Greenwich",0],
              UNIT["degree",0.0174532925199433],
              AUTHORITY["EPSG","4326"]],
          PROJECTION["Transverse_Mercator"],
          PARAMETER["latitude_of_origin",0],
          PARAMETER["central_meridian",-99],
          PARAMETER["scale_factor",0.9996],
          PARAMETER["false_easting",500000],
          PARAMETER["false_northing",0],
          UNIT["metre",1,
              AUTHORITY["EPSG","9001"]],
          AUTHORITY["EPSG","32614"]]
    """)
/*
   val ChattaAlbers = CRS.parseWKT("""
PROJCS["Albers_Conical_Equal_Area",
    GEOGCS["NAD83",
        DATUM["North_American_Datum_1983",
            SPHEROID["GRS 1980",6378137,298.2572221010002,
                AUTHORITY["EPSG","7019"]],
            AUTHORITY["EPSG","6269"]],
        PRIMEM["Greenwich",0],
        UNIT["degree",0.0174532925199433],
        AUTHORITY["EPSG","4269"]],
    PROJECTION["Albers_Conic_Equal_Area"],
    PARAMETER["standard_parallel_1",29.5],
    PARAMETER["standard_parallel_2",45.5],
    PARAMETER["latitude_of_center",23],
    PARAMETER["longitude_of_center",-96],
    PARAMETER["false_easting",0],
    PARAMETER["false_northing",0],
    UNIT["metre",1,
        AUTHORITY["EPSG","9001"]]]
""")
*/
    val WebMercator = CRS.decode("EPSG:3857")

   val LongLat = CRS.decode("EPSG:4326",true)
}

object Transformer {
  private val transformCache:mutable.Map[(Crs,Crs),MathTransform] = 
    new mutable.HashMap[(Crs,Crs),MathTransform]()
  
  def cacheTransform(crs1:Crs,crs2:Crs) = {
    transformCache((crs1,crs2)) = CRS.findMathTransform(crs1,crs2,true)
  }

  private def initCache() = {
    cacheTransform(Projections.LongLat,Projections.RRVUTM)
    cacheTransform(Projections.RRVUTM,Projections.LongLat)
    cacheTransform(Projections.LongLat,Projections.WebMercator)
    cacheTransform(Projections.WebMercator,Projections.LongLat)
    cacheTransform(Projections.WebMercator,Projections.RRVUTM)
  }

  initCache()

  def transform[D](feature:Geometry[D],fromCRS:Crs,toCRS:Crs):Geometry[D] = {
    if(!transformCache.contains((fromCRS,toCRS))) { cacheTransform(fromCRS,toCRS) }
    feature.mapGeom( geom => 
      JTS.transform(feature.geom, transformCache((fromCRS,toCRS)))
    )
  }
}