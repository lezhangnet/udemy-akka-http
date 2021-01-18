package part1_recap

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AkkaStreamsRecap extends App {

  implicit val system = ActorSystem("AkkaStreamsRecap")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val source = Source(1 to 100)
  val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)
  val flow = Flow[Int].map(x => x + 1)

  val runnableGraph: RunnableGraph[NotUsed] = source.via(flow).to(sink)
  runnableGraph.run()

  val simpleMaterializedValue: NotUsed = runnableGraph.run() // this will run again and MIX with the run above
  // .run() // materialization
  println("simpleMaterializedValue:" + simpleMaterializedValue) // NotUsed

  // MATERIALIZED VALUE
  val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)((currentSum, element) => currentSum + element)
  val sumFuture: Future[Int] = source.runWith(sumSink)
  sumFuture.onComplete {
    case Success(sum) => println(s"The sum of all the numbers from the simple source is: $sum")
    case Failure(ex) => println(s"Summing all the numbers from the simple source FAILED: $ex")
  }

  println("anotherMaterializedValue")
  val anotherMaterializedValue = source.viaMat(flow)(Keep.right).toMat(sink)(Keep.left).run()
    // .run()
  /*
    1 - materializing a graph means materializing ALL the components
    2 - a materialized value can be ANYTHING AT ALL
   */

  /*
    Backpressure actions: slower consumer

    - buffer elements
    - apply a strategy in case the buffer overflows
    - fail the entire stream
   */

  val bufferedFlow = Flow[Int].buffer(10, OverflowStrategy.dropHead) // drop old

  source.async
    .via(bufferedFlow).async
    .runForeach { e =>
      // a slow consumer
      Thread.sleep(100)
      println(e)
    }


}
