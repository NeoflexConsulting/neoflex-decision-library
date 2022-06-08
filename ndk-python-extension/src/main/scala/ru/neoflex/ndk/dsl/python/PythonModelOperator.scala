package ru.neoflex.ndk.dsl.python

import cats.implicits.catsSyntaxOptionId
import org.graalvm.polyglot.{ Context, Source }
import ru.neoflex.ndk.dsl.ActionBase

import java.io.InputStreamReader
import java.nio.file.{ Files, Paths }
import scala.util.Using
import PythonModelOperator._

import java.util.concurrent.ConcurrentHashMap

class PythonModelOperator(
  override val id: String,
  filename: String,
  modelInput: () => Any,
  modelResultsCollector: Any => Unit,
  sourceDirPath: String = "/",
  userModelClassName: String = UserModelClassName,
  modelsCache: ModelsCache = staticModelsCache,
  override val name: Option[String] = None)
    extends ActionBase(id, name.orElse(filename.some)) {

  private val modelFilePath = Paths.get(getClass.getResource(s"$sourceDirPath$filename").toURI)

  private def initializeModel(): Model = {
    val context = Context
      .newBuilder(Language)
      .allowAllAccess(true)
      .option("python.ForceImportSite", "true")
      .build()

    val pyModelFileIr = new InputStreamReader(Files.newInputStream(modelFilePath))
    val evalResult = Using(pyModelFileIr) { r =>
      val source = Source.newBuilder(Language, r, filename).build()
      context.eval(source)
    }

    def instantiateModel(): Model = {
      val pyUserModelClass = context.getPolyglotBindings.getMember(userModelClassName)
      pyUserModelClass.newInstance().as(classOf[Model])
    }

    evalResult.fold(throw _, _ => instantiateModel())
  }

  override val f: () => Unit = () => {
    val modelCacheKey = ModelCacheKey(id, modelFilePath.toString, userModelClassName)
    val model = modelsCache.get(modelCacheKey).getOrElse {
      val modelInstance = initializeModel()
      modelsCache.put(modelCacheKey, modelInstance).getOrElse(modelInstance)
    }

    val results = model.predict(modelInput())
    modelResultsCollector(results)
  }
}

object PythonModelOperator {
  val Language           = "python"
  val UserModelClassName = "UserModel"

  private[python] val staticModelsCache = new ModelsCache {
    private val cache = new ConcurrentHashMap[ModelCacheKey, Model]()

    override def get(key: ModelCacheKey): Option[Model]               = Option(cache.get(key))
    override def put(key: ModelCacheKey, model: Model): Option[Model] = Option(cache.putIfAbsent(key, model))
    override def invalidate(): Unit                                   = cache.clear()
  }
}

trait ModelsCache {
  def get(key: ModelCacheKey): Option[Model]
  def put(key: ModelCacheKey, model: Model): Option[Model]
  def invalidate(): Unit
}

final case class ModelCacheKey(operatorId: String, modelFilePath: String, modelClassName: String)

trait Model {
  def predict(input: Any): Any
}
