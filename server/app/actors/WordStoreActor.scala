package actors

import akka.actor.Props
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence.PersistentActor

import scala.collection.mutable

object WordStoreActor {

  case object RequestUpdate
  case class SendUpdate(words: Seq[String])
  case class WordUpdate(words: Seq[(String, Int)])

  def props() = Props(new WordStoreActor())
}

class WordStoreActor() extends PersistentActor {

  import WordStoreActor._

  override def persistenceId = "wordstore-actor"

  private val mediator = DistributedPubSubExtension(context.system).mediator

  private val words = mutable.Map[String, Int](
      "Scala" -> 1,
      "Scala.JS" -> 1,
      "Play" -> 1,
      "Akka" -> 1,
      "SBT" -> 1
    ).withDefaultValue(0)

  private var wordsCaseInsensitive = words.keySet.map(w => w.toLowerCase -> w).toMap

  override def receiveCommand =  {
    case RequestUpdate => sender ! WordUpdate(normalisedWords)

    case su: SendUpdate => persist(su)(handleUpdate)
  }

  override def receiveRecover = {
    case su: SendUpdate => handleUpdate(su)
  }

  private def handleUpdate(sendUpdate: SendUpdate) = {
    sendUpdate.words foreach { w =>
      wordsCaseInsensitive += (w.toLowerCase -> wordsCaseInsensitive.getOrElse(w.toLowerCase, w))
      words(wordsCaseInsensitive(w.toLowerCase)) += 1
    }
    mediator ! Publish("word-updates", WordUpdate(normalisedWords))
  }

  private def normalisedWords = {
    val maxValue = words.values.max
    words.toSeq.map { case (word: String, count: Int) => (word, count*100/maxValue) }
  }
}
