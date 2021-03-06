package org.example.akka

import TellPattern.DonutStoreProtocol.Info

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object TellPattern extends App {

  println("Step 1: create an actor system")
  val system = ActorSystem("DonutStoreActorSystem")

  println("\nStep 2: Define the message passing protocol for our DonutStoreActor")

  object DonutStoreProtocol {

    case class Info(name: String)

  }

  println("\nStep 3: Define DonutInfoActor")

  class DonutInfoActor extends Actor with ActorLogging {

    def receive: Receive = {
      case Info(name) =>
        log.info(s"Found $name donut")
    }
  }

  println("\nStep 4: Create DonutInfoActor")
  val donutInfoActor = system.actorOf(Props[DonutInfoActor], name = "DonutInfoActor")

  println("\nStep 5: Akka Tell Pattern")
  donutInfoActor ! Info("vanilla")

  println("\nStep 6: close the actor system")
  val isTerminated = system.terminate()

  println("\nStep 7: Check the status of the actor system")
  isTerminated.onComplete {
    case Success(result) => println("Successfully terminated actor system")
    case Failure(e) => println("Failed to terminate actor system")
  }

  Thread.sleep(5000)

}
