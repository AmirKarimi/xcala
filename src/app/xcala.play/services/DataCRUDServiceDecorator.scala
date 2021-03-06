package xcala.play.services

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import reactivemongo.bson._
import reactivemongo.core.commands.LastError
import xcala.play.models.{ QueryOptions, DataWithTotalCount }
import reactivemongo.api.collections.GenericQueryBuilder
import xcala.play.utils.WithExecutionContext

trait DataCrudServiceDecorator[A, B] 
	extends DataReadServiceDecorator[A, B] 
  with DataRemoveService 
  with DataSaveService[B]
  with WithExecutionContext {
  
  val service: DataDocumentHandler[A] 
		with DataReadService[A] 
		with DataRemoveService 
		with DataSaveService[A]

  def mapBackModel(source: B): Future[A]
  
  def copyBackModel(source: B, destination: A): Future[A]
  
	def remove(query: BSONDocument): Future[LastError] = service.remove(query)
  
  def insert(model: B) = mapBackModel(model).flatMap(service.insert)
	
  def save(model: B) = {
    getIdFromModel(model) match {
      case Some(id) =>
        service.findById(id).flatMap {
          case Some(originalModel) => copyBackModel(model, originalModel).flatMap(service.save)
          case None => Future.failed(new Throwable("Save error: Model ID not found to be updated."))
        }
      case _ => 
        mapBackModel(model).flatMap(service.save)
    }
  }
}
