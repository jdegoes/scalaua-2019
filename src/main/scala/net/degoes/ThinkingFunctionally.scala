package net.degoes

import scalaz.zio._
import scalaz.zio.duration._
import scalaz.zio.console._
import scalaz.zio.blocking._

import Common._

/**
 * Procedural effects *do*; functional effects *describe*.
 * 
 * Procedural effect:
 * 
 * {{{
 *   def println(line: String): Unit 
 * }}}
 * 
 * Functional effect:
 * 
 * {{{
 *   def println(line: String): Task[Unit]
 * }}}
 * 
 * where, for example, `case class Task[A](unsafeRun: () => A)`
 * 
 * ZIO's only functional effect type: 
 * 
 * {{{
 *    ZIO[-R, +E, +A]
 * }}}
 * 
 * Similar to a purely functional, but effectful version of this function:
 * 
 * {{{
 *    R => Either[E, A]
 * }}}
 * 
 * ZIO has type aliases to simplify common use cases:
 * 
 * {{{
 *    type   UIO[+A] = ZIO[Any,   Nothing, A]
 *    type  Task[+A] = ZIO[Any, Throwable, A]
 * 
 *    type TaskR[-R, +A] = ZIO[  R, Throwable, A]
 *    type    IO[+E, +A] = ZIO[Any,         E, A]
 * }}}
 */
object ThinkingFunctionally extends App {
  import functional._
  import logging._
  import social._
  import auth._
  import email._

  def run(args: List[String]) = putStrLn("Hello World!").fold(_ => 1, _ => 0)

  type Services = Logging with Social with Auth with Email with Blocking

  /**
   * Your mission, should you choose to accept it:
   * 
   * 1. Refactor the procedural/OOP code to its functional equivalent.
   * 2. Fix the two cases of thread pool exhaustion.
   * 4. Ensure errors that happen here always propagate upward.
   * 5. Locally rate limit the social / email services to prevent overload.
   * 
   * Questions for Thought:
   * 
   *   1. Which issues were fixed automatically? Which required attention?
   *   2. For those that required attention to fix, how are the solutions
   *      an improvement over the same solutions in the procedural version?
   *   3. How easy is will it be to change the functional version, versus
   *      the procedural version?
   *   4. How many styles of computation are there in the procedural version?
   *      How many in the functional version?
   *   5. How much logic is devoted to error handling in the procedural version?
   *      How much in the functional version?
   */
  def inviteFriends(token: AuthToken): ZIO[Services, Throwable, Receipt] =     
    ???

  // Superpower #1: Composable timeouts
  lazy val invitation1 = inviteFriends(???).timeout(60.seconds)

  // Superpower #2: Lossless & localized errors w/auto propagation:
  invitation1.flatMap(receipt => putStrLn(s"Receipt is $receipt"))

  /**
   * Describes the result of an email invitation. Either it failed with some
   * errors, or succeeded.
   */
  case class Receipt(value: Map[UserID, Option[Throwable]]) { self =>
    final def |+| (that: Receipt): Receipt = 
      Receipt(self.value ++ that.value)

    final def succeeded: Int = value.values.filter(_ == None).size
    final def failures: List[Throwable] = value.values.collect { case Some(t) => t }.toList
  }
  object Receipt {
    def empty: Receipt = Receipt(Map())
    def success(userId: UserID): Receipt = Receipt(Map(userId -> None))
    def failure(userId: UserID, t: Throwable): Receipt = Receipt(Map(userId -> Some(t)))
  }
}

object functional {
  object logging {
    trait Logging {
      def logging: Logging.Service 
    }
    object Logging {
      trait Service {
        def log(message: String): UIO[Unit]
      }
      trait Live extends Logging {
        val logging = new Service {
          // Implement this using Common.log:
          def log(message: String): UIO[Unit] = ???
        }
      }
      object Live extends Live
    }

    def log(message: String): ZIO[Logging, Nothing, Unit] = 
      ZIO.accessM(_.logging log message)
  }

  object auth {
    trait Auth {
      def auth: Auth.Service
    }
    object Auth {
      trait Service {
        def login(token: AuthToken): Task[UserID]
      }
      trait Live extends Auth with Blocking {
        val auth = new Service {
          // Implement this using Common.login:
          // Hint: blocking.interruptible
          def login(token: AuthToken): Task[UserID] = ???
        }
      }
      object Live extends Live with Blocking.Live
    }

    def login(token: AuthToken): ZIO[Auth, Throwable, UserID] = 
      ZIO.accessM(_.auth login token)
  }

  object social {
    trait Social {
      def social: Social.Service
    }
    object Social {
      trait Service {
        def getProfile(id: UserID): Task[UserProfile]
        def getFriends(id: UserID): Task[List[UserID]]
      }
      trait Live extends Social {
        val social = new Service {
          // Implement this using Common.getProfile:
          // Hint: ZIO.effectAsync
          def getProfile(id: UserID): Task[UserProfile] = ???

          // Implement this using Common.getFriends:
          // Hint: ZIO.fromFuture
          def getFriends(id: UserID): Task[List[UserID]] = ???
        }
      }
      object Live extends Live
    }

    def getProfile(id: UserID): ZIO[Social, Throwable, UserProfile] = 
      ZIO.accessM(_.social getProfile id)

    def getFriends(id: UserID): ZIO[Social, Throwable, List[UserID]] =
      ZIO.accessM(_.social getFriends id)
  }
  
  object email {
    trait Email {
      def email: Email.Service 
    }
    object Email {
      trait Service {
        def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Task[Unit]
      }
      trait Live extends Email with Blocking {
        val email = new Service {
          // Implement this using Common.sendEmail:
          // Hint: blocking.interruptible
          def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Task[Unit] = 
            ???
        }
      }
    }

    def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): ZIO[Email, Throwable, Unit] = 
      ZIO.accessM(_.email.sendEmail(from, to)(subject, message))
  }
}