import java.io.FileWriter

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import java.net.InetSocketAddress

import akka.event.Logging
import akka.util.ByteString



class HoneyPotServer extends Actor {
    val log = Logging(context.system, "HoneyPot")

    override def preStart {
        import context.system
        val manager = IO(Tcp)
        manager ! Tcp.Bind (self, new InetSocketAddress(443))
        manager ! Tcp.Bind (self, new InetSocketAddress(80))
  }

  def receive = {
      case Tcp.Connected(localAddress, remoteAddress) => {
          sender ! Tcp.Register(context.actorOf(HoneyPotConnectionHandler.props(remoteAddress, localAddress, sender)))
      }
  }
}


object HoneyPotConnectionHandler {
    def props(remote: InetSocketAddress, local: InetSocketAddress, connection: ActorRef): Props =
        Props(new HoneyPotConnectionHandler(remote, local, connection))
}

class HoneyPotConnectionHandler(remote: InetSocketAddress, local: InetSocketAddress, connection: ActorRef) extends Actor with ActorLogging{

    val now = System.currentTimeMillis / 1000

    //TODO: Read dump file folder from a config file

    val dumpFileName = s"$now-${normalize(local.toString)}-${normalize(remote.toString)}.dat"

    log.debug(s"Dumping to $dumpFileName")

    context.watch(connection)

    def normalize(aStr:String):String = aStr.replaceAll("\\.", "_").replaceAll("/", "").replaceAll(":", "-")

    def dumpData(data:ByteString) = {
        val fw = new FileWriter(dumpFileName, true)
        var targetHost:Option[String] = None

        try {
            val str = data.map(_.toChar).toArray
            fw.write(str)
            if (targetHost.isEmpty) targetHost = detectHostName(str.mkString)

        } finally {
            logConnection(remote, local, targetHost)

            fw.close
        }
    }

    private def logConnection(remote:InetSocketAddress, local: InetSocketAddress, targetHostname: Option[String]) = {
        log.info(s"NEW CONNECTION: [${local}] [${remote}] [${targetHostname.getOrElse("N/A")}]")

    }

    //trying to be greedy, if I find a "header" that looks like: Host: example.com ill return that if not ill go with all
    //"strings" in the payload
    private def detectHostName(str:String):Option[String] = {


        val HTTPHost = "Host:[\\s]+([A-Za-z0-9_\\.]+)".r
        val HTTPSHost = "((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\\\.)+[A-Za-z]{2,6}".r //"([A-Za-z0-9_\\.]+)".r

        val httphostname = HTTPHost.findFirstIn(str)

        if (httphostname.isDefined)
            httphostname
        else{
            val strings = HTTPSHost.findAllIn(str).mkString(";")
            if (strings.isEmpty) None
            else Some(strings)
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

