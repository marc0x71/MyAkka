package org.example.akka

import AkkaRoundRobinPool.DonutStoreProtocol.{CheckStock, WorkerFailedException}

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.ask
import akka.routing.{DefaultResizer, ScatterGatherFirstCompletedPool, TailChoppingPool}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

object AkkaTailChoppingPool extends App {

  println("Step 1: Create an actor system")
  val system = ActorSystem("DonutStoreActorSystem")

  println("\nStep 2: Define the message passing protocol for our DonutStoreActor")

  object DonutStoreProtocol {

    case class Info(name: String)

    case class CheckStock(name: String)

    case class WorkerFailedException(error: String) extends Exception(error)

  }

  println("\nStep 3: Create DonutStockActor")

  class DonutStockActor extends Actor with ActorLogging {

    override def supervisorStrategy: SupervisorStrategy =
      OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 5 seconds) {
        case _: WorkerFailedException =>
          log.error("Worker failed exception, will restart.")
          Restart

        case _: Exception =>
          log.error("Worker failed, will need to escalate up the hierarchy")
          Escalate
      }

    // We are using a resizable RoundRobinPool.
    val resizer = DefaultResizer(lowerBound = 5, upperBound = 10)
    val props = TailChoppingPool(
      nrOfInstances = 5,
      resizer = Some(resizer),
      within = 5 seconds,
      interval = 10 millis,
      supervisorStrategy = supervisorStrategy
    ).props(Props[DonutStockWorkerActor])
    val donutStockWorkerRouterPool: ActorRef = context.actorOf(props, "DonutStockWorkerRouter")

    def receive = {
      case checkStock@CheckStock(name) =>
        log.info(s"Checking stock for $name donut")
        donutStockWorkerRouterPool forward checkStock
    }
  }

  println("\ntep 4: Worker Actor called DonutStockWorkerActor")

  class DonutStockWorkerActor extends Actor with ActorLogging {

    override def postRestart(reason: Throwable): Unit = {
      log.info(s"restarting ${self.path.name} because of $reason")
    }

    def receive = {
      case CheckStock(name) =>
        sender ! findStock(name)
    }

    def findStock(name: String): Int = {
      val delay = Random.between(100, 1000)
      log.info(s"Finding stock for donut = $name, thread = ${Thread.currentThread().getId} ...")
      Thread.sleep(delay)
      log.info(s"Finding stock for donut = $name, thread = ${Thread.currentThread().getId} - delay=$delay")
      delay
    }
  }

  println("\nStep 5: Define DonutStockActor")
  val donutStockActor = system.actorOf(Props[DonutStockActor], name = "DonutStockActor")

  Thread.sleep(2000)

  println("\nStep 6: Use Akka Ask Pattern and send a bunch of requests to DonutStockActor")
  implicit val timeout = Timeout(5 second)

  val vanillaStockRequests = (1 to 10).map(i => (donutStockActor ? CheckStock("vanilla")).mapTo[Int])
  private val start: Long = System.currentTimeMillis()
  for {
    results <- Future.sequence(vanillaStockRequests)
  } yield println(s"vanilla stock results = $results sum=${results.sum} - ${System.currentTimeMillis() - start} ms")

  Thread.sleep(5000)

  val isTerminated = system.terminate()

}
