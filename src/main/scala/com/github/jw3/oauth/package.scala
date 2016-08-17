package com.github.jw3

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext


package object oauth {
  implicit def sys2ec(implicit system: ActorSystem): ExecutionContext = system.dispatcher
}
