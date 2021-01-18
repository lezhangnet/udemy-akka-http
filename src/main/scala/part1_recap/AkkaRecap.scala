package part1_recap

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, PoisonPill, Props, Stash, SupervisorStrategy}
import akka.util.Timeout

object AkkaRecap extends App {

  class SimpleActor extends Actor with ActorLogging with Stash {
    override def receive: Receive = {
      case "createChild" =>
        val childActor = context.actorOf(Props[SimpleActor], "myChild")
        childActor ! "child hello"

      case "stashThis" =>
        stash()
      case "change handler NOW" =>
        unstashAll()
        context.become(anotherHandler)

      case "change" =>
        println("changing handler...")
        context.become(anotherHandler)

      case message => println(s"I received: $message")
    }

    def anotherHandler: Receive = {
      case message => println(s"In another receive handler: $message")
    }

    // lifecycle hooks
    override def preStart(): Unit = {
      log.info("I'm starting")
      // [INFO] [04/04/2020 15:23:43.560] [AkkaRecap-akka.actor.default-dispatcher-2] [akka://AkkaRecap/user/anotherSimpleActor] I'm starting
    }

    // what to do when child fails
    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: RuntimeException => Restart
      case _ => Stop
    }
  }

  // actor encapsulation
  val system = ActorSystem("AkkaRecap")
  // #1: you can only instantiate an actor through the actor system
  val actor = system.actorOf(Props[SimpleActor], "simpleActor")
  // #2: sending messages
  actor ! "hello" // ! = "tell"
  /*
    - messages are sent asynchronously
    - many actors (in the millions) can share a few dozen threads
    - each message is processed/handled ATOMICALLY - no race conditions inside actor!
    - no need for locks
   */

  // actors can spawn other actors
  actor ! "createChild"

  // changing actor behavior + stashing
  actor ! "change"
  actor ! "hello again" // this will be handled in changed handler
  actor ! "createChild" // no longer generating new child

  // guardians: /system, /user, / = root guardian // top-level actor system

  // actors have a defined lifecycle: they can be started, stopped, suspended, resumed, restarted

  // logging
  // supervision

  // configure Akka infrastructure: dispatchers, routers, mailboxes

  // schedulers
  import system.dispatcher

  import scala.concurrent.duration._
  system.scheduler.scheduleOnce(2 seconds) {
    actor ! "delayed happy birthday!"
  }

  // Akka patterns including FSM + ask pattern
  import akka.pattern.ask
  implicit val timeout = Timeout(3 seconds)

  val future = actor ? "question" // ? = "ask" // this seems will not be answered

  // the pipe pattern
  import akka.pattern.pipe
  val anotherActor = system.actorOf(Props[SimpleActor], "anotherSimpleActor")
  future.mapTo[String].pipeTo(anotherActor)
  // I received: Failure(akka.pattern.AskTimeoutException: Ask timed out on [Actor[akka://AkkaRecap/user/simpleActor#1362043602]] after [3000 ms].
  // Message of type [java.lang.String]. A typical reason for `AskTimeoutException` is that the recipient actor didn't send a reply.)

  Thread.sleep(6000)

  println("killing actors")
  // stopping actors - context.stop
  actor ! PoisonPill
  anotherActor ! PoisonPill

  // it is not exiting ?!

}
