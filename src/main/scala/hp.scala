import java.io.FileWriter

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import java.net.InetSocketAddress

import akka.event.Logging
import akka.util.ByteString



class TCPEchoServer(port: Int) extends Actor {
    val log = Logging(context.system, this)



    override def preStart {
      import context.system
      val manager = IO(Tcp)
      manager ! Tcp.Bind (self, new InetSocketAddress(4443))
      //manager ! Tcp.Bind (self, new InetSocketAddress(80))

  }

  def receive = {
      case Tcp.Connected(localAddress, remoteAddress) => {
          log.info(s"[New connection, local address, remote address] [${localAddress}] [${remoteAddress}]")
          sender ! Tcp.Register(context.actorOf(HoneyPotConnectionHandler.props(remoteAddress, sender)))
      }
  }
}


object HoneyPotConnectionHandler {
    def props(remote: InetSocketAddress, connection: ActorRef): Props =
        Props(new HoneyPotConnectionHandler(remote, connection))
}

class HoneyPotConnectionHandler(remote: InetSocketAddress, connection: ActorRef) extends Actor {
    val log = Logging(context.system, this)

    val now = System.currentTimeMillis / 1000
    val dumpFileName = s"$now-${normamlize(remote.toString)}.dat"

    log.debug(s"Dumping to $dumpFileName")

    context.watch(connection)

    def normamlize(aStr:String):String = aStr.replaceAll("\\.", "_").replaceAll("/", "").replaceAll(":", "-")

    def dumpData(data:ByteString) = {
        val fw = new FileWriter(dumpFileName, true)
        try {
            fw.write(data.map(_.toChar).toArray)

        } finally {
            fw.close
        }

    }
    def receive: Receive = {
        case Tcp.Received(data) => dumpData(data)
        case Tcp.Aborted =>  context.stop(self)

        case _: Tcp.ConnectionClosed => context.stop(self)
    }
}

object TCPEchoServer extends App {
  //val port = Option(System.getenv("PORT")).map(_.toInt).getOrElse(9999)
  ActorSystem().actorOf(Props(new TCPEchoServer(94242)))
}

