package org.example.akka

import AskPattern.DonutStoreProtocol.Info

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object AskPattern extends App {

  println("Step 1: Create an actor system")
  val system = ActorSystem("DonutStoreActorSystem")

  println("\nStep 2: Define the message passing protocol for our DonutStoreActor")
  object DonutStoreProtocol {
    case class Info(name: String)
  }

  println("\nStep 3: Create DonutInfoActor")
  class DonutInfoActor extends Actor with ActorLogging {

    def receive: Receive = {
      case Info(name) if name == "vanilla" =>
        log.info(s"Found valid $name donut")
        sender ! Info(s"Found valid $name donut")

      case Info(name) =>
        log.info(s"$name donut is not supported")
        sender ! Info(s"$name donut is not supported")
    }
  }

  println("\nStep 4: Create DonutInfoActor")
  val donutInfoActor = system.actorOf(Props[DonutInfoActor], name = "DonutInfoActor")

  implicit val timeout = Timeout(5 second)

  val vanillaDonutFound: Future[Info] = (system.actorSelection("/user/DonutInfoActor") ? Info("vanilla")).mapTo[Info]
  for {
    found <- vanillaDonutFound
  } yield (println(s"Vanilla donut found = $found"))

  val glazedDonutFound: Future[Info] = (system.actorSelection("/user/*") ? Info("glazed")).mapTo[Info]
  for {
    found <- glazedDonutFound
  } yield (println(s"Glazed donut found = $found"))

  Thread.sleep(5000)

  println("\nStep 6: Close the actor system")
  val isTerminated = system.terminate()

}
