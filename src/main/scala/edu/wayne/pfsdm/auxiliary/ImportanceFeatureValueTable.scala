package edu.wayne.pfsdm.auxiliary

import java.io._

import edu.wayne.pfsdm.Util
import edu.wayne.pfsdm.feature.importance.ImportanceFeature
import org.lemurproject.galago.core.retrieval.{Retrieval, RetrievalFactory}
import org.lemurproject.galago.utility.tools.Arguments

import scala.collection.JavaConversions._
import scala.io.Source

/**
  * Created by fsqcds on 6/10/15.
  */
object ImportanceFeatureValueTable extends App {
  val parameters = Arguments.parse(args)
  val fields: Seq[String] = parameters.getList("fields", classOf[String])
  val fieldFeatureName = parameters.get("tableFeature", "")
  val queriesPath = parameters.get("queries", "")

  val queries: Seq[(String, String)] = Source.fromFile(queriesPath).getLines().
    map { line => line.split("\t") match {
      case Array(qId, qText) => (qId, qText)
    }
    }.toSeq
  val tokenizedQueries: Seq[(String, Seq[String])] = queries.map { case (qId: String, qText: String) =>
    (qId, Util.filterTokens(qText))
  }
  val uniBiGrams: Seq[(String, Seq[Seq[String]])] = tokenizedQueries.map { case (qId, qTokens) =>
    (qId, qTokens.map(Seq(_)) union (if (qTokens.size >= 2) qTokens.sliding(2).toSeq else Seq.empty))
  }

  parameters.set("fieldFeatures", List())
  val retrieval: Retrieval = RetrievalFactory.create(parameters)
  val feature: ImportanceFeature = ImportanceFeature(fieldFeatureName, retrieval)

  val output = new PrintWriter(parameters.getString("output"))

  for ((qId, grams) <- uniBiGrams; gram <- grams) {
    val ngramtype = gram.length match {
      case 1 => "unigram";
      case 2 => "bigram"
    }
      val phi = feature.getPhi(gram, qId)
      output.print(qId)
      output.print("\t")
      output.print(gram.mkString(" "))
      output.print("\t")
      output.println(phi)
  }
  output.close()
}
