# akka-hp

Simple Akka based honey pot. Should be combined with fw rules to redirect all ports to the hp listening port. By default it will listen on 80 and 443.

- Logs all connection attempts.
- Dumps the bytes sent over the connection.
- Replies with nothing.

# Usage

````sbt run````

# Building

````sbt assembly````
