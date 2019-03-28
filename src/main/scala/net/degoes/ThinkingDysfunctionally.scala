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
  class InvitationService(logging: Logging, social: Social, auth: Auth, email: Email) {
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
      val userId = auth.login(token)

      val promise = Promise[Unit]()

      // Count how many friends we sent email to:
      val counter = new AtomicInteger(0)

      // Try to log the user into the platform:
      userId match {
        case Failure(t) => 
          logging.log(s"Failed to log in $userId")

          throw t 

        case Success(userId) =>
          logging.log(s"Logged in $userId")

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
     * The logging service.
     */
    trait Logging {
      def log(message: String): Unit
    }
    trait LoggingLive extends Logging {
      def log(message: String): Unit = Common.log(message)
    }
    object LoggingLive extends LoggingLive
  }
  object auth {
    /**
     * The authentication service.
     */
    trait Auth {
      def login(token: AuthToken): Try[UserID]
    }
    trait AuthLive extends Auth {
      def login(token: AuthToken): Try[UserID] = Common.login(token)
    }
    object AuthLive extends AuthLive
  }

  object social {
    /**
     * The social service.
     */
    trait Social {
      def getProfile(id: UserID)(onSuccess: UserProfile => Unit, onFailure: Throwable => Unit): Unit

      def getFriends(id: UserID)(implicit ec: ExecutionContext): Future[List[UserID]]
    }
    trait SocialLive extends Social {
      def getProfile(id: UserID)(onSuccess: UserProfile => Unit, onFailure: Throwable => Unit): Unit =
        Common.getProfile(id)(onSuccess, onFailure)

      def getFriends(id: UserID)(implicit ec: ExecutionContext): Future[List[UserID]] = 
        Common.getFriends(id)
    }
    object SocialLive extends SocialLive
  }

  object email {
    /**
     * The email service.
     */
    trait Email {
      def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Unit
    }
    trait EmailLive extends Email {
      def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Unit = 
        Common.sendEmail(from, to)(subject, message)
    }
    object EmailLive extends EmailLive
  }
}