package net.degoes

import scala.concurrent._
import scala.util.Try

/**
 * A bunch of random functions and data types from third-party libraries, some 
 * Java, some Scala. These have been minimally cleaned up so they don't return 
 * null, but they can throw exceptions.
 */
object Common {
  trait AuthToken
  trait UserID
  trait LoginException extends Exception
  trait EmailException extends Exception
  trait EmailAddress
  trait UserProfile {
    def name: String
    def email: EmailAddress
  }

  def log(message: String): Unit = ???

  def login(token: AuthToken): Try[UserID] = ???

  def getProfile(id: UserID)(onSuccess: UserProfile => Unit, onFailure: Throwable => Unit): Unit = ???

  def getFriends(id: UserID)(implicit ec: ExecutionContext): Future[List[UserID]] = ???

  def sendEmail(from: EmailAddress, to: EmailAddress)(subject: String, message: String): Unit = ???
}