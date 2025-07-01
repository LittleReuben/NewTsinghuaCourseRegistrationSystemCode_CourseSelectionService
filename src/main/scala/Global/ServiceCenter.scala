package Global

import Global.GlobalVariables.serviceCode
import cats.effect.IO
import com.comcast.ip4s.Port
import org.http4s.Uri

object ServiceCenter {
  val projectName: String = "TongWen"

  val dbManagerServiceCode = "A000001"
  val tongWenDBServiceCode = "A000002"
  val tongWenServiceCode = "A000003"

  val fullNameMap: Map[String, String] = Map(
    tongWenDBServiceCode -> "数据库（DB-Manager）",
    tongWenServiceCode -> "同文（Tong-Wen）"
  )
}
