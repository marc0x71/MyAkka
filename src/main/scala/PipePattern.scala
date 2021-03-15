package org.example.akka

import PipePattern.Infrastructure.asyncRetrievePhoneNumberFromDb

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object PipePattern {

  object Infrastructure {

    private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))

    private val db: Map[String, Int] = Map(
      "Daniel" -> 123,
      "Alice" -> 456,
      "Bob" -> 999
    )

    // external API
    def asyncRetrievePhoneNumberFromDb(name: String): Future[Int] =
      Future(db(name))
  }


  trait PhoneCallProtocol

  case class FindAndCallPhoneNumber(name: String) extends PhoneCallProtocol

  case class InitiatePhoneCall(number: Int) extends PhoneCallProtocol

  case class LogPhoneCallFailure(reason: Throwable) extends PhoneCallProtocol

  val phoneCallInitiatorV1: Behavior[PhoneCallProtocol] =
    Behaviors.setup { context =>
      var nPhoneCalls = 0
      var nFailures = 0

      implicit val sc: ExecutionContextExecutor = context.executionContext
      Behaviors.receiveMessage {
        case FindAndCallPhoneNumber(name) =>
          val futureNumber: Future[Int] = asyncRetrievePhoneNumberFromDb(name)
          futureNumber.onComplete {
            case Success(number) =>
              // actually perform the phone call here
              context.log.info(s"Initiating phone call to $number")
              nPhoneCalls += 1 // please cringe here
            case Failure(ex) =>
              context.log.error(s"Phone call to $name failed: $ex")
              nFailures += 1 // please cringe here
          }
          Behaviors.same
      }
    }

  val phoneCallInitiatorV2: Behavior[PhoneCallProtocol] =
    Behaviors.setup { context =>
      var nPhoneCalls = 0
      var nFailures = 0

      Behaviors.receiveMessage {
        case FindAndCallPhoneNumber(name) =>
          val futureNumber: Future[Int] = asyncRetrievePhoneNumberFromDb(name)
          // pipe makes all the difference
          // transform the result of the future into a message
          context.pipeToSelf(futureNumber) {
            case Success(phoneNumber) =>
              // messages that will be sent to myself
              InitiatePhoneCall(phoneNumber)
            case Failure(ex) =>
              LogPhoneCallFailure(ex)
          }
          Behaviors.same
        case InitiatePhoneCall(number) =>
          // perform the phone call
          context.log.info(s"Starting phone call to $number")
          nPhoneCalls += 1 // no more cringing
          Behaviors.same
        case LogPhoneCallFailure(ex) =>
          context.log.error(s"Calling number failed: $ex")
          nFailures += 1 // no more cringing
          Behaviors.same
      }
    }

  def phoneCallInitiatorV3(nPhoneCalls: Int = 0, nFailures: Int = 0): Behavior[PhoneCallProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case FindAndCallPhoneNumber(name) =>
          val futureNumber: Future[Int] = asyncRetrievePhoneNumberFromDb(name)
          // pipe makes all the difference
          // transform the result of the future into a message
          context.pipeToSelf(futureNumber) {
            case Success(phoneNumber) =>
              // messages that will be sent to myself
              InitiatePhoneCall(phoneNumber)
            case Failure(ex) =>
              LogPhoneCallFailure(ex)
          }
          Behaviors.same
        case InitiatePhoneCall(number) =>
          // perform the phone call
          context.log.info(s"Starting phone call to $number")
          // change behavior
          phoneCallInitiatorV3(nPhoneCalls + 1, nFailures)
        case LogPhoneCallFailure(ex) =>
          // log failure
          context.log.error(s"Calling number failed: $ex")
          // change behavior
          phoneCallInitiatorV3(nPhoneCalls, nFailures + 1)
      }
    }

  def main(args: Array[String]): Unit = {
    val root = ActorSystem(phoneCallInitiatorV3(), "PhoneCallActor")

    root ! FindAndCallPhoneNumber("Alice")
    root ! FindAndCallPhoneNumber("Akka")

    Thread.sleep(2000)
    root.terminate()
  }
}
