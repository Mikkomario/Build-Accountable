package vf.buildaccountable.test

import org.kabeja.dxf.{DXFConstants, DXFLayer, DXFLine, DXFPolyline}
import org.kabeja.parser.ParserBuilder
import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.file.FileExtensions._

import java.nio.file.Paths
import scala.io.Codec
import scala.jdk.CollectionConverters._

/**
 * Extracts shape data from a dxf file
 *
 * @author Mikko Hilpinen
 * @since 03/01/2024, v0.1
 */
object ExtractShapesTest extends App
{
	implicit val codec: Codec = Codec.UTF8
	
	val parser = ParserBuilder.createDefaultParser()
	parser.parse("data/test-input/floor-plan-material-r12-format.dxf")
	val doc = parser.getDocument
	
	Paths.get("data/test-output/extracted-shapes.txt").writeUsing { writer =>
		doc.getDXFLayerIterator.asScala.foreach {
			case layer: DXFLayer =>
				// Attempts to read poly-lines
				val polyLines = Option(layer.getDXFEntities(DXFConstants.ENTITY_TYPE_POLYLINE)) match {
					case Some(polyLines) =>
						polyLines.asScala.iterator
							.flatMap {
								case pLine: DXFPolyline => Some(pLine)
								case other =>
									println(s"Unknown entity type ${ other.getClass.getSimpleName } in ${
										layer.getName} poly-lines")
									None
							}
							.toVector
					case None => Vector.empty
				}
				// Lists the vertices
				writer.println(s"\n---------\n\nLayer ${layer.getName} contains ${polyLines.size} poly-lines")
				polyLines.zipWithIndex.foreach { case (polyLine, index) =>
					writer.println(s"\t- Poly-line #${index + 1} (length = ${polyLine.getLength})")
					(0 until polyLine.getVertexCount).foreach { index =>
						val vertex = polyLine.getVertex(index)
						writer.println(s"\t\t- (${vertex.getX}, ${vertex.getY})")
					}
				}
				
				// Also reads lines, if present
				// WET WET
				val lines = Option(layer.getDXFEntities(DXFConstants.ENTITY_TYPE_LINE)) match {
					case Some(lines) =>
						lines.asScala.iterator
							.flatMap {
								case line: DXFLine => Some(line)
								case other =>
									println(s"Unknown entity type in ${ other.getClass.getSimpleName } in ${
										layer.getName } lines")
									None
							}
							.toVector
					case None => Vector.empty
				}
				writer.println(s"\nLayer ${layer.getName} contains ${lines.size} lines")
				lines.zipWithIndex.foreach { case (line, index) =>
					val lineStr = Pair(line.getStartPoint, line.getEndPoint)
						.map { p => s"(${p.getX}, ${p.getY})" }.mkString(" => ")
					writer.println(s"\t- Line #${ index + 1 } (length = ${ line.getLength }): $lineStr")
				}
			
			case other => println(s"Unknown layer type ${other.getClass.getSimpleName} in document")
		}
	}
	
	println("Done!")
}
