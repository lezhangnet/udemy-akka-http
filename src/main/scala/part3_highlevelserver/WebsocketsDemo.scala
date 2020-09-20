package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.CompactByteString
import akka.http.scaladsl.server.Directives._

import scala.concurrent.duration._

object WebsocketsDemo extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // Message: TextMessage vs BinaryMessage

  val textMessage = TextMessage(Source.single("hello via a text message"))
  val binaryMessage = BinaryMessage(Source.single(CompactByteString("hello via a binary message")))

  // ref src/main/html/websockets.html
  val html =
    """
      |<html>
      |    <head>
      |        <script>
      |            var exampleSocket = new WebSocket("ws://localhost:8080/greeter");
      |            console.log("starting websocket...");
      |
      |            // listener
      |            exampleSocket.onmessage = function(event) {
      |                console.log("onmessage...")
      |                var newChild = document.createElement("div");
      |                newChild.innerText = event.data;
      |                document.getElementById("1").appendChild(newChild);
      |            };
      |
      |            exampleSocket.onopen = function(event) {
      |                console.log("onopen...")
      |                exampleSocket.send("socket seems to be open...");
      |            };
      |
      |            console.log("sending...")
      |            exampleSocket.send("socket says: hello, server!");
      |            // ERROR: Uncaught DOMException: Failed to execute 'send' on 'WebSocket': Still in CONNECTING state.
      |            // i.e. web socket NOT open yet
      |        </script>
      |    </head>
      |
      |    <body>
      |        Starting websocket...
      |        <div id="1">
      |        </div>
      |    </body>
      |
      |</html>
    """.stripMargin

  // flow from incoming message to output message
  def websocketFlow: Flow[Message, Message, Any] = Flow[Message].map {
    case tm: TextMessage =>
      println("received TextMessage: " + tm)
      TextMessage(Source.single("Server says back: ") ++ tm.textStream ++ Source.single("!"))
    case bm: BinaryMessage =>
      println("received BinaryMessage")
      bm.dataStream.runWith(Sink.ignore)
      TextMessage(Source.single("Server received a binary message..."))
  }

  val websocketRoute =
    (pathEndOrSingleSlash & get) {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          html
        )
      )
    } ~
    path("greeter") {
      println("/greeter...")
      handleWebSocketMessages(websocketFlow)
      // handleWebSocketMessages(socialFlow)
    }

  Http().bindAndHandle(websocketRoute, "localhost", 8080)


  case class SocialPost(owner: String, content: String)

  val socialFeed = Source(
    List(
      SocialPost("Martin", "Scala 3 has been announced!"),
      SocialPost("Daniel", "A new Rock the JVM course is open!"),
      SocialPost("Martin", "I killed Java.")
    )
  )

  val socialMessages = socialFeed
    .throttle(1, 2 seconds)
    .map(socialPost => TextMessage(s"${socialPost.owner} said: ${socialPost.content}"))

  val socialFlow: Flow[Message, Message, Any] = Flow.fromSinkAndSource(
    Sink.foreach[Message](println),
    socialMessages
  )


}
