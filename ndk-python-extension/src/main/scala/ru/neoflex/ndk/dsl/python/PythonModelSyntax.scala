package ru.neoflex.ndk.dsl.python

trait PythonModelSyntax {
  def callModel(
    id: String,
    filename: String,
    modelInput: => Any,
    userModelClassName: String = PythonModelOperator.UserModelClassName
  )(
    resultsCollector: Any => Unit
  ): PythonModelOperator = {
    new PythonModelOperator(id, filename, () => modelInput, resultsCollector, userModelClassName = userModelClassName)
  }
}
