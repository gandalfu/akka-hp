import java.io.FileWriter

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import java.net.InetSocketAddress

import akka.event.Logging
import akka.util.ByteString



class HoneyPotServer extends Actor {
    val log = Logging(context.system, this)



    override def preStart {
      import context.system
      val manager = IO(Tcp)
      manager ! Tcp.Bind (self, new InetSocketAddress(443))
      manager ! Tcp.Bind (self, new InetSocketAddress(80))

  }

  def receive = {
      case Tcp.Connected(localAddress, remoteAddress) => {
          log.info(s"[New connection, local address, remote address] [${localAddress}] [${remoteAddress}]")
          sender ! Tcp.Register(context.actorOf(HoneyPotConnectionHandler.props(remoteAddress, localAddress, sender)))
      }
  }
}


object HoneyPotConnectionHandler {
    def props(remote: InetSocketAddress, local: InetSocketAddress, connection: ActorRef): Props =
        Props(new HoneyPotConnectionHandler(remote, local, connection))
}

class HoneyPotConnectionHandler(remote: InetSocketAddress, local: InetSocketAddress, connection: ActorRef) extends Actor {
    val log = Logging(context.system, this)

    val now = System.currentTimeMillis / 1000
    val dumpFileName = s"$now-${normalize(local.toString)}-${normalize(remote.toString)}.dat"

    log.debug(s"Dumping to $dumpFileName")

    context.watch(connection)

    def normalize(aStr:String):String = aStr.replaceAll("\\.", "_").replaceAll("/", "").replaceAll(":", "-")

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

object HoneyPotServer extends App {
  ActorSystem().actorOf(Props(new HoneyPotServer))
}

