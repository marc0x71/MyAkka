package org.example.akka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip}
import akka.stream.{ClosedShape, Graph}

object AkkaStreamsGraphs {
  implicit val system: ActorSystem = ActorSystem("GraphBasics")

  // step 1 - setting up the fundamentals for the graph
  val graph: Graph[ClosedShape.type, NotUsed] =
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope

      // step 2 - add the necessary components of this graph
      val input = builder.add(Source(1 to 1000))
      val incrementer = builder.add(Flow[Int].map(x => x + 1)) // hard computation
      val multiplier = builder.add(Flow[Int].map(x => x * 10)) // hard computation
      val output = builder.add(Sink.foreach[(Int, Int)](println))

      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip = builder.add(Zip[Int, Int]) // fan-in operator

      // step 3 - tying up the components
      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      // step 4 - return a closed shape
      ClosedShape
    }

  def main(args: Array[String]): Unit = {
    RunnableGraph.fromGraph(graph).run()
  }

}
