package net.degoes

import scalaz.zio._
import scalaz.zio.console._

import Common._

/**
 * ZIO's only functional effect type: 
 * 
 *    ZIO[-R, +E, +A]
 * 
 * Similar to a purely functional, but effectful version of this function:
 * 
 *    R => Either[E, A]
 * 
 * Type aliases:
 * 
 *    type   UIO[+A] = ZIO[Any,   Nothing, A]
 *    type  Task[+A] = ZIO[Any, Throwable, A]
 * 
 *    type TaskR[-R, +A] = ZIO[  R, Throwable, A]
 *    type    IO[+E, +A] = ZIO[Any,         E, A]
 */
object ThinkingFunctionally extends App {
  import functional._
  import logging._
  import social._
  import auth._
  import email._

  def run(args: List[String]) = putStrLn("Hello World!").fold(_ => 1, _ => 0)

  type Services = Logging with Social with Auth with Email

  /**
   * Your mission, should you choose to accept it:
   * 
   * 1. Refactor the procedural/OOP code to its functional equivalent.
   * 2. Fix thread pool exhaustion for HTTP requests.
   * 4. Ensure errors that happen here always propagate upward.
   * 5. Rate limit the social / email services per invocation.
   * 
   */
  def inviteFriends(token: AuthToken): ZIO[Services, Throwable, Boolean] = 
    ???
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
      trait Live extends Service {
        def log(message: String): UIO[Unit] = ???
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
      trait Live extends Service {
        def login(token: AuthToken): Task[UserID] = ???
      }
      object Live extends Live
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
      trait Live extends Service {
        def getProfile(id: UserID): Task[UserProfile] = ???

        def getFriends(id: UserID): Task[List[UserID]] = ???
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
      trait Live extends Service {
        def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Task[Unit] = 
          ???
      }
    }

    def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): ZIO[Email, Throwable, Unit] = 
      ZIO.accessM(_.email.sendEmail(from, to)(subject, message))
  }
}