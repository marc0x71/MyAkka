package org.example.akka

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.Future

object AkkaStream {

  implicit val system: ActorSystem = ActorSystem() // "thread pool"

  val source: Source[Int, NotUsed] = Source(1 to 1000)
  val flow: Flow[Int, Int, NotUsed] = Flow[Int].map(x => x * 2)
  val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)
  val graph: RunnableGraph[NotUsed] = source.via(flow).to(sink)

  def main(args: Array[String]): Unit = {
    graph.run()
  }
}
