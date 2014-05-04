package bio4j.model.test.titan

// import org.scalatest._

import com.thinkaurelius.titan.example.GraphOfTheGodsFactory
import com.thinkaurelius.titan.core._
import java.io.File

import GodsSchema._
import GodsImplementation._
import MakeKeys._

import bio4j.model._

class TitanSchemaSuite extends org.scalatest.FunSuite with org.scalatest.BeforeAndAfterAll {

  val graphLocation = new File("/tmp/titanSchemaTest")
  var g: TitanGraph = null

  // Creating a new titan instance
  override def beforeAll() {
    def cleanDir(f: File) {
      if (f.isDirectory) f.listFiles.foreach(cleanDir(_))
      else { println(f.toString); f.delete }
    }
    cleanDir(graphLocation)

    g = TitanFactory.open(graphLocation.getAbsolutePath)
    println("Created Titan graph")
  }

  override def afterAll() {
    if(g != null) {
      g.shutdown
      println("Shutdown Titan graph")
    }
  }

  test("create property keys") {

    // g.addPropertyKey(age)
    g.addPropertyKey(name)
    g.commit

    // val ageType: TitanType = g.getType(age.label)
    val nameType: TitanType = g.getType(name.label)
    assert(nameType.getName === name.label)
    assert(nameType.isPropertyKey)

    // we checked that it's a property key, so we can cast:
    // val ageKey: TitanKey = ageType.asInstanceOf[TitanKey]
    val nameKey: TitanKey = nameType.asInstanceOf[TitanKey]
    // FIXME: the data type is set to some crap
    // PRIMITIVES that's the issue
    // assert(ageKey.getDataType.getName === classOf[age.Rep].getName)
    assert(nameKey.getDataType.getName === classOf[name.Rep].getName)

  }

  test("create edge labels") {

    g.addEdgeLabel(pet)
    g.commit

    val petType: TitanType = g.getType(pet.tpe.label)
    assert(petType.getName === pet.tpe.label)
    assert(petType.isEdgeLabel)

    // we checked that it's an edge label, so we can cast:
    val petLabel: TitanLabel = petType.asInstanceOf[TitanLabel]
    assert(petLabel.isDirected)

    // Don't know how to check arity for a TitanLabel
  }

}
