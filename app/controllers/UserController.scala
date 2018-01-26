package controllers

import javax.inject.Inject
import models.Tables._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.i18n.{I18nSupport, MessagesApi}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._


class UserController @Inject()(val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
                              (implicit ec: ExecutionContext) extends AbstractController(cc)
  with HasDatabaseConfigProvider[JdbcProfile] with I18nSupport {

  import UserController._

  /**
    * 一覧表示
    */
  def list = Action.async { implicit request =>
    // IDの昇順にすべてのユーザ情報を取得
    db.run(Users.sortBy(t => t.id).result).map { users =>
      // 一覧画面を表示
      Ok(views.html.user.list(users))
    }
  }

  /**
    * 編集画面表示
    */

  def edit(id: Option[Int]) = Action.async { implicit rs =>
    // リクエストパラメータにIDが存在する場合
    val form = if (id.isDefined) {
      // IDからユーザ情報を1件取得
      db.run(Users.filter(t => t.id === id.get.bind).result.head).map { user =>
        // 値をフォームに詰める
        userForm.fill(UserForm(Some(user.id), user.name, user.companyId))
      }
    } else {
      // リクエストパラメータにIDが存在しない場合
      Future { userForm }
    }

    form.flatMap { form =>
      // 会社一覧を取得
      db.run(Companies.sortBy(_.id).result).map { companies =>
        Ok(views.html.user.edit(form, companies))
      }
    }
  }

  /**
    * 登録実行
    */
  def create = Action.async { implicit rs =>
    // リクエストの内容をバインド
    userForm.bindFromRequest.fold(
      // エラーの場合
      error => {
        db.run(Companies.sortBy(t => t.id).result).map { companies =>
          BadRequest(views.html.user.edit(error, companies))
        }
      },
      // OKの場合
      form  => {
        // ユーザを登録
        val user = UsersRow(0, form.name, form.companyId)
        db.run(Users += user).map { _ =>
          // 一覧画面へリダイレクト
          Redirect(routes.UserController.list)
        }
      }
    )
  }

  /**
    * 更新実行
    */
  def update = Action.async { implicit rs =>
    // リクエストの内容をバインド
    userForm.bindFromRequest.fold(
      // エラーの場合は登録画面に戻す
      error => {
        db.run(Companies.sortBy(t => t.id).result).map { companies =>
          BadRequest(views.html.user.edit(error, companies))
        }
      },
      // OKの場合は登録を行い一覧画面にリダイレクトする
      form  => {
        // ユーザ情報を更新
        val user = UsersRow(form.id.get, form.name, form.companyId)
        db.run(Users.filter(t => t.id === user.id.bind).update(user)).map { _ =>
          // 一覧画面にリダイレクト
          Redirect(routes.UserController.list)
        }
      }
    )
  }

  /**
    * 削除実行
    */
  def remove(id: Int) = Action.async { implicit rs =>
    // ユーザを削除
    db.run(Users.filter(t => t.id === id.bind).delete).map { _ =>
      // 一覧画面へリダイレクト
      Redirect(routes.UserController.list)
    }
  }
}

object UserController {
  // フォームの値を格納するケースクラス
  case class UserForm(id: Option[Int], name: String, companyId: Option[Int])

  // formから送信されたデータ ⇔ ケースクラスの変換を行う
  val userForm = Form(
    mapping(
      "id"        -> optional(number),
      "name"      -> nonEmptyText(maxLength = 20),
      "companyId" -> optional(number)
    )(UserForm.apply)(UserForm.unapply)
  )
}
