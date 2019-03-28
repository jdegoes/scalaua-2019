package net.degoes

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import java.util.concurrent.atomic.AtomicInteger

import Common.{ EmailAddress, UserID, UserProfile, AuthToken }

object ThinkingDysfunctionally extends App {
  import dysfunctional._
  import logging._
  import social._
  import auth._
  import email._

  type Services = Logging with Social with Auth with Email

  /**
   * The invitation service. Uses constructor-based dependency injection to 
   * inject the dependencies.
   * 
   * The following problems have been reported in connection with the 
   * `InvitationService`:
   * 
   * 1. Two thread pools are becoming exhausted non-deterministically.
   * 2. Errors that happen in the service don't always make it higher.
   * 3. The social and email services are randomly overloaded with requests.
   */
  class InvitationService(services: Services) {
    // Import all modules used by this service:
    import services._

    /**
     * This function will authenticate the user using the specified `AuthToken`,
     * lookup the profile of the user, find all friends of the user, and for 
     * each friend, lookup their profile and send them an email inviting them
     * to use our super cool application!
     * 
     * The function will return when it is done sending all the emails. If there 
     * is an error along the way, the function will throw some exception.
     * 
     * Returns the number of friends invited to the application.
     */
    def inviteFriends(token: AuthToken)(implicit ec: ExecutionContext): Int = {
      val userIdTry = auth.login(token)

      val promise = Promise[Unit]()

      // Count how many friends we sent email to:
      val counter = new AtomicInteger(0)

      // Try to log the user into the platform:
      userIdTry match {
        case Failure(t) => 
          logging.log(s"Failed to log in $token")

          throw t 

        case Success(userId) =>
          logging.log(s"Logged in $token")

          // Get the profile of the user:
          social.getProfile(userId)(userProfile =>
            // Get the friends of the user:
            social.getFriends(userId).foreach { friends =>
              // For each friend, get their profile and try to send them an email:
              friends.foreach { friendId =>
                // Get profiles & send emails in parallel:
                Future {
                  if (counter.incrementAndGet() == friends.length) {
                    // Complete the promise if we notified all the friends:
                    promise.complete(Success(()))
                  }

                  social.getProfile(friendId)({ friendProfile =>
                    email.sendEmail(userProfile.email, friendProfile.email)(
                      s"A Message from ${userProfile.name}",
                      "Your friend has invited you to use our cool app!"
                    )
                  }, _ => ()) // Ignore error
                }
              }
            }
          , t => throw t)
      }

      Await.result(promise.future, 60.seconds)

      counter.get
    }
  }
}

object dysfunctional {
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
        def log(message: String): Unit
      }
      /**
       * Production implementation of the logging service.
       */
      trait Live extends Logging {
        val logging = new Service {
          def log(message: String): Unit = Common.log(message)
        }
      }
      object Live extends Live
    }
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
        def login(token: AuthToken): Try[UserID]
      }
      /**
       * Production implementation of the auth service.
       */
      trait Live extends Auth {
        val auth = new Service {
          def login(token: AuthToken): Try[UserID] = Common.login(token)
        }
      }
      object Live extends Live
    }
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
        def getProfile(id: UserID)(onSuccess: UserProfile => Unit, onFailure: Throwable => Unit): Unit

        def getFriends(id: UserID)(implicit ec: ExecutionContext): Future[List[UserID]]
      }
      /**
       * Production implementation of the social service.
       */
      trait Live extends Social {
        val social = new Service {
          def getProfile(id: UserID)(onSuccess: UserProfile => Unit, onFailure: Throwable => Unit): Unit =
            Common.getProfile(id)(onSuccess, onFailure)

          def getFriends(id: UserID)(implicit ec: ExecutionContext): Future[List[UserID]] = 
            Common.getFriends(id)
        }
      }
      object Live extends Live
    }
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
        def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Unit
      }
      /**
       * Production implementation of the email service.
       */
      trait Live extends Email {
        val email = new Service {
          def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Unit = 
            Common.sendEmail(from, to)(subject, message)
        }
      }
    }
  }
}