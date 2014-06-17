package com.example.learnedreversegeocoding.cassandra

/**
 * Source: https://github.com/eigengo/activator-akka-cassandra
 */

import com.datastax.driver.core.BoundStatement

private[cassandra] trait CassandraResultSetOperations {

  import com.datastax.driver.core.{ResultSet, ResultSetFuture}
  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

  private case class ExecutionContextExecutor(executionContext: ExecutionContext) extends java.util.concurrent.Executor {

    def execute(command: Runnable): Unit = { executionContext.execute(command) }
  }

  protected class RichResultSetFuture(resultSetFuture: ResultSetFuture) extends Future[ResultSet] {

    import scala.concurrent.CanAwait
    import scala.util.{Success, Try}
    import scala.concurrent.duration.Duration
    import java.util.concurrent.TimeUnit

    @throws(classOf[InterruptedException])
    @throws(classOf[scala.concurrent.TimeoutException])
    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {

      resultSetFuture.get(atMost.toMillis, TimeUnit.MILLISECONDS)
      this
    }

    @throws(classOf[Exception])
    def result(atMost: Duration)(implicit permit: CanAwait): ResultSet = {

      resultSetFuture.get(atMost.toMillis, TimeUnit.MILLISECONDS)
    }

    def onComplete[U](func: (Try[ResultSet]) => U)(implicit executionContext: ExecutionContext): Unit = {

      if (resultSetFuture.isDone) {

        func(Success(resultSetFuture.getUninterruptibly))
      } else {

        resultSetFuture.addListener(new Runnable {

          def run() {
            func(Try(resultSetFuture.get()))
          }
        }, ExecutionContextExecutor(executionContext))
      }
    }

    def isCompleted: Boolean = resultSetFuture.isDone

    def value: Option[Try[ResultSet]] = if (resultSetFuture.isDone) Some(Try(resultSetFuture.get())) else None
  }

  implicit def toFuture(resultSetFuture: ResultSetFuture): Future[ResultSet] = new RichResultSetFuture(resultSetFuture)
}

private[cassandra] trait Binder[-A] {

  def bind(value: A, boundStatement: BoundStatement): Unit
}

private[cassandra] trait BoundStatementOperations {

  implicit class RichBoundStatement[A : Binder](boundStatement: BoundStatement) {

    val binder = implicitly[Binder[A]]

    def bindFrom(value: A): BoundStatement = {

      binder.bind(value, boundStatement)
      boundStatement
    }
  }
}

object cassandra {

  object resultset extends CassandraResultSetOperations
  object boundstatement extends BoundStatementOperations
}