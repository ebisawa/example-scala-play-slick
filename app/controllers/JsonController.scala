package controllers

import javax.inject.Inject
import models.Tables._
import play.api.mvc._
import play.api.db.slick._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._


class JsonController @Inject()(val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
                              (implicit ec: ExecutionContext) extends AbstractController(cc)
  with HasDatabaseConfigProvider[JdbcProfile] {

  case class UserForm(id: Option[Int], name: String, companyId: Option[Int])

  implicit val usersRowWritesFormat = new Writes[UsersRow]{
    def writes(user: UsersRow): JsValue = {
      Json.obj(
        "id"        -> user.id,
        "name"      -> user.name,
        "companyId" -> user.companyId
      )
    }
  }

  implicit val userFormFormat = Json.format[UserForm]


  /**
    * 一覧表示
    */
  def list = Action.async { implicit rs =>
    // IDの昇順にすべてのユーザ情報を取得
    db.run(Users.sortBy(t => t.id).result).map { users =>
      // ユーザの一覧をJSONで返す
      Ok(Json.obj("users" -> users))
    }
  }

  /**
    * ユーザ登録
    */
  def create = Action.async(parse.json) { implicit rs =>
    rs.body.validate[UserForm].map { form =>
      // OKの場合はユーザを登録
      val user = UsersRow(0, form.name, form.companyId)
      db.run(Users += user).map { _ =>
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future {
        BadRequest(Json.obj("result" ->"failure", "error" -> JsError.toJson(e)))
      }
    }
  }

  /**
    * ユーザ更新
    */
  def update = Action.async(parse.json) { implicit rs =>
    rs.body.validate[UserForm].map { form =>
      // OKの場合はユーザ情報を更新
      val user = UsersRow(form.id.get, form.name, form.companyId)
      db.run(Users.filter(t => t.id === user.id.bind).update(user)).map { _ =>
        Ok(Json.obj("result" -> "success"))
      }
    }.recoverTotal { e =>
      // NGの場合はバリデーションエラーを返す
      Future {
        BadRequest(Json.obj("result" ->"failure", "error" -> JsError.toJson(e)))
      }
    }
  }

  /**
    * ユーザ削除
    */
  def remove(id: Int) = Action.async { implicit rs =>
    // ユーザを削除
    db.run(Users.filter(t => t.id === id.bind).delete).map { _ =>
      Ok(Json.obj("result" -> "success"))
    }
  }
}
