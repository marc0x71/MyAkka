package org.example.akka

import TellPattern.DonutStoreProtocol.Info

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object TellPatternForwardToChild extends App {

  println("Step 1: create an actor system")
  val system = ActorSystem("DonutStoreActorSystem")

  println("\nStep 2: Define the message passing protocol for our DonutStoreActor")
  object DonutStoreProtocol {
    case class Info(name: String)
  }

  println("\nStep 3a: Define a BakingActor and a DonutInfoActor")
  class BakingActor extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("prestart")

    override def postStop(): Unit = log.info("postStop")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("preRestart")

    override def postRestart(reason: Throwable): Unit = log.info("postRestart")

    def receive: Receive = {
      case Info(name) =>
        log.info(s"BakingActor baking $name donut")
    }
  }

  println("\nStep 3b: Define DonutInfoActor")
  class DonutInfoActor extends Actor with ActorLogging {

    val bakingActor: ActorRef = context.actorOf(Props[BakingActor], name = "BakingActor")

    override def preStart(): Unit = log.info("prestart")

    override def postStop(): Unit = log.info("postStop")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("preRestart")

    override def postRestart(reason: Throwable): Unit = log.info("postRestart")

    def receive: Receive = {
      case msg @Info(name) =>
        log.info(s"Found $name donut")
        bakingActor forward msg
    }
  }

  println("\nStep 4: Create DonutInfoActor")
  val donutInfoActor = system.actorOf(Props[DonutInfoActor], name = "DonutInfoActor")

  println("\nStep 5: Akka Tell Pattern")
  // donutInfoActor ! PoisonPill

  donutInfoActor ! Info("vanilla")

  println("\nStep 6: close the actor system")
  val isTerminated = system.terminate()

  println("\nStep 7: Check the status of the actor system")
  isTerminated.onComplete {
    case Success(result) => println("Successfully terminated actor system")
    case Failure(e)     => println("Failed to terminate actor system")
  }

  Thread.sleep(5000)

}
