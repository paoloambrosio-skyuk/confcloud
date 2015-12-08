package controllers

import actors.WordStoreActor.{CurrentWords, SendUpdate, WordUpdate}
import actors.{WordStoreActor, WordWebSocketActor}
import akka.util.Timeout
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc._
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import akka.actor._
import akka.pattern.ask
import javax.inject._
import scala.concurrent._
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class Application @Inject() (system: ActorSystem) extends Controller {

  lazy val wordStore = system.actorOf(WordStoreActor.props, "word-store-actor")

  val wordForm = Form(
    single(
      "word" -> nonEmptyText
    )
  )

  def index = Action {
    Ok(views.html.index())
  }

  implicit val wordUpdateFormat = new Format[WordUpdate] {
    override def writes(wu: WordUpdate): JsValue = Json.toJson(wu.words.map(w => Json.obj(
      "text" -> w._1,
      "size" -> w._2
    )))
    override def reads(json: JsValue): JsResult[WordUpdate] = ???
  }
  implicit val wordUpdateFrameFormatter = FrameFormatter.jsonFrame[WordUpdate]

  implicit val timeout = Timeout(3 seconds)

  def words = WebSocket.acceptWithActor[String, WordUpdate] { request => out =>
    WordWebSocketActor.props(out, wordStore)
  }

  def vote = Action.async { implicit request =>
    currentWords map { cw =>
      Ok(views.html.vote(wordForm, cw))
    }
  }

  def makeVote = Action.async { implicit request =>
    wordForm.bindFromRequest.fold(
      formWithErrors => {
        currentWords map { cw =>
          BadRequest(views.html.vote(formWithErrors, cw))
        }
      },
      word => Future { sendVote(word) }
    )
  }

  private def sendVote(word: String)(implicit request: Request[AnyContent]) = {
    wordStore ! SendUpdate(Seq(word))
    Redirect("/vote").flashing(
      "success" -> s"You voted for $word!"
    )
  }

  def currentWords = {
    val currentWordsF = wordStore ? CurrentWords
    for {
      cw <- currentWordsF.mapTo[Seq[String]]
    } yield cw
  }
}
