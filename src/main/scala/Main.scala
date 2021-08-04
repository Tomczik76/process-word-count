import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import scala.sys.process._
import cats.implicits._
import java.nio.file.Paths
import cats.effect.std.Queue
import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax._
import fs2.io.readOutputStream
import scala.concurrent.duration._

import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.blaze.server._
import org.http4s.circe._
import org.http4s.circe.CirceEntityEncoder._

import cats.effect.kernel.Ref
object Main extends IOApp {

  def httpService(ref: Ref[IO, Map[String, Int]]) =
    BlazeServerBuilder[IO](this.runtime.compute)
      .bindHttp(8080, "localhost")
      .withHttpApp(
        HttpRoutes
          .of[IO] { case GET -> Root =>
            ref.get.map(_.asJson).flatMap(Ok(_))
          }
          .orNotFound
      )
      .serve

  def processStream(path: String, ref: Ref[IO, Map[String, Int]]) =
    readOutputStream[IO](10)(outputStream => IO(path.#>(outputStream).!))
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .map(parse)
      .collect { case Right(json) => json.asObject }
      .collect { case Some(obj) => (obj("event_type"), obj("data")).tupled }
      .collect { case Some((typeJson, dataJson)) => (typeJson.asString, dataJson.asString).tupled }
      .collect { case Some((eventType, data)) => Map(eventType -> data.split("\\s+").size) }
      .groupWithin(Int.MaxValue, 10.seconds)
      .map(_.fold)
      .evalMap(ref.set)

  def app(path: String) =
    Ref
      .of[IO, Map[String, Int]](Map())
      .flatMap(ref =>
        processStream(path, ref)
          .concurrently(httpService(ref))
          .compile
          .drain
          .as(ExitCode.Success)
      )

  override def run(args: List[String]): IO[ExitCode] =
    args match {
      case path :: Nil => 
        IO(Paths.get(path).toFile().exists())
          .flatMap {
            case true => 
              val executablePath =
                if (path.contains("/")) path
                else "./" + path
              app(executablePath) 
            case false => IO.println("Path doesn't exist").as(ExitCode.Error)
          }
      case _ => IO.println("Usage: <path to program>").as(ExitCode.Error)
    }

}
