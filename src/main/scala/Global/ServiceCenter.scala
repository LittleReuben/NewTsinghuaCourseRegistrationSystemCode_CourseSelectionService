package Global

object ServiceCenter {
  val projectName: String = "NewTsinghuaCourseRegistrationSystem"
  val dbManagerServiceCode = "A000001"
  val tongWenDBServiceCode = "A000002"
  val tongWenServiceCode = "A000003"

  val CourseSelectionServiceCode = "A000010"
  val SystemLogServiceCode = "A000011"
  val UserAuthServiceCode = "A000012"
  val CourseManagementServiceCode = "A000013"
  val SemesterPhaseServiceCode = "A000014"
  val UserAccountServiceCode = "A000015"
  val CourseEvaluationServiceCode = "A000016"

  val fullNameMap: Map[String, String] = Map(
    tongWenDBServiceCode -> "DB-Manager（DB-Manager）",
    tongWenServiceCode -> "Tong-Wen（Tong-Wen）",
    CourseSelectionServiceCode -> "CourseSelectionService（CourseSelectionService)",
    SystemLogServiceCode -> "SystemLogService（SystemLogService)",
    UserAuthServiceCode -> "UserAuthService（UserAuthService)",
    CourseManagementServiceCode -> "CourseManagementService（CourseManagementService)",
    SemesterPhaseServiceCode -> "SemesterPhaseService（SemesterPhaseService)",
    UserAccountServiceCode -> "UserAccountService（UserAccountService)",
    CourseEvaluationServiceCode -> "CourseEvaluationService（CourseEvaluationService)"
  )

  def serviceName(serviceCode: String): String = {
    fullNameMap(serviceCode).toLowerCase
  }
}
