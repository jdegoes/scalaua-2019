package net.degoes

import scalaz.zio._
import scalaz.zio.console._

import Common._

object ThinkingFunctionally extends App {

  def run(args: List[String]) =
    myAppLogic.fold(_ => 1, _ => 0)

  val myAppLogic =
    for { 
      _ <- putStrLn("Hello! What is your name?")
      n <- getStrLn
      _ <- putStrLn(s"Hello, ${n}, welcome to ZIO!")
    } yield ()
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
        def log(message: String): UIO[Unit] = ???
      }
    }
    def log(message: String): ZIO[Logging, Nothing, Unit] = 
      ZIO.accessM(_.logging.log(message))
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
        def login(token: AuthToken): Task[UserID] = ???
      }
    }
    def login(token: AuthToken): ZIO[Auth, Throwable, UserID] = 
      ZIO.accessM(_.auth.login(token))
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
        def getProfile(id: UserID): Task[UserProfile]

        def getFriends(id: UserID): Task[List[UserID]]
      }
    }
    def getProfile(id: UserID): ZIO[Social, Throwable, UserProfile] = 
      ZIO.accessM(_.social.getProfile(id))

    def getFriends(id: UserID): ZIO[Social, Throwable, List[UserID]] =
      ZIO.accessM(_.social.getFriends(id))
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
        def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Task[Unit]
      }
    }

    def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): ZIO[Email, Throwable, Unit] = 
      ZIO.accessM(_.email.sendEmail(from, to)(subject, message))
  }
}