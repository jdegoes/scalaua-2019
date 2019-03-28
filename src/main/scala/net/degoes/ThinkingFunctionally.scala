package net.degoes

import scalaz.zio._
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
   *   1. Which issues were fixed automatically? Which required intention?
   *   2. For those that required attention to fix, how are the solutions
   *      an improvement over the same solutions in the procedural version?
   *   3. How easy is will it be to change the functional version, versus
   *      the procedural version?
   */
  def inviteFriends(token: AuthToken): ZIO[Services, Throwable, Receipt] = 
    ???

  /**
   * Describes the result of an email invitation. Either it failed with some
   * errors, or succeeded.
   */
  case class Receipt(value: Map[UserID, Option[Throwable]]) { self =>
    final def append(that: Receipt): Receipt = 
      Receipt(self.value ++ that.value)
  }
  object Receipt {
    // Constructs an empty receipt:
    def empty: Receipt = Receipt(Map())
    // Constructs a receipt that tracks a successful send to the specified user:
    def success(userId: UserID): Receipt = Receipt(Map(userId -> None))
    // Constructs a receipt that tracks a failed send to the specified user:
    def failure(userId: UserID, t: Throwable): Receipt = Receipt(Map(userId -> Some(t)))
  }
}

object functional {
  object logging {
    /**
     * The logging module.
     */
    trait Logging {
      def logging: Logging.Service 
    }
    object Logging {
      /**
       * The logging service.
       */
      trait Service {
        // Old: def log(message: String): Unit 
        def log(message: String): UIO[Unit]
      }
      trait Live extends Logging {
        val logging = new Service {
          def log(message: String): UIO[Unit] = ???
        }
      }
      object Live extends Live
    }
    def log(message: String): ZIO[Logging, Nothing, Unit] = 
      ZIO.accessM(_.logging log message)
  }

  object auth {
    /**
     * The auth module.
     */
    trait Auth {
      def auth: Auth.Service
    }
    object Auth {
      /**
       * The auth service.
       */
      trait Service {
        // def login(token: AuthToken): Try[UserID]
        def login(token: AuthToken): Task[UserID]
      }
      trait Live extends Auth with Blocking {
        val auth = new Service {
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
    /**
     * The social module.
     */
    trait Social {
      def social: Social.Service
    }
    object Social {
      /**
       * The social service.
       */
      trait Service {
        // def getProfile(id: UserID)(onSuccess: UserProfile => Unit, onFailure: Throwable => Unit): Unit
        def getProfile(id: UserID): Task[UserProfile]

        // def getFriends(id: UserID)(implicit ec: ExecutionContext): Future[List[UserID]]
        def getFriends(id: UserID): Task[List[UserID]]
      }
      trait Live extends Social {
        val social = new Service {
          // Hint: ZIO.effectAsync
          def getProfile(id: UserID): Task[UserProfile] = ???

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
    /**
     * The email module.
     */
    trait Email {
      def email: Email.Service 
    }
    object Email {
      /**
       * The email service.
       */
      trait Service {
        // def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Unit
        def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Task[Unit]
      }
      trait Live extends Email with Blocking {
        val email = new Service {
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