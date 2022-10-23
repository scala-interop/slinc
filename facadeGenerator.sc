import mill._, scalalib._

trait FacadeGenerationModule extends JavaModule {
  def adaptiveTypeList(arity: Int) = 
      typeList
  val typeList = List("int", "float", "double", "long", "byte", "Object")

  val typeShorthands = Map("int" -> "I", "float" -> "F", "double" -> "D", "short" -> "S", "long" -> "L", "byte" -> "B", "Object" -> "O")

  def buildArgumentList(arity: Int, typeList: List[String]): List[List[String]] =
    if(arity != 0)
      typeList.flatMap(a => buildArgumentList(arity - 1, typeList).map(b => a :: b))
    else 
      typeList.map(List(_))

  def callClassGen(arity: Int) = {
    s"""|package fr.hammons.slinc;
        |
        |import java.lang.invoke.MethodHandle;
        |
        |class MethodHandleArity${arity} {
        | ${callMethodGen(arity).mkString("\n\n")}
        |}""".stripMargin
  }

  def callMethodGen(arity: Int) = {

    val builtArgumentList = buildArgumentList(arity, adaptiveTypeList(arity)).take(14000)

    println(builtArgumentList.size)
    for {
      argumentList <- builtArgumentList
      returnType = argumentList.head
      argumentTypes = argumentList.tail 
      methodName = (argumentTypes :+ returnType).map(typeShorthands.apply).mkString
      parameterNames = argumentTypes.zipWithIndex.map{ case (_, index) => s"a$index"}
      parameterDefinitions = parameterNames.zip(argumentTypes).map{
        case (name, typ) => s"$typ $name"
      }.mkString(",")
      parameterSection = if(parameterDefinitions.nonEmpty) 
        s",$parameterDefinitions"
      else
        ""
      callSection = parameterNames.mkString(",")
      invoker = if(methodName.contains("O")) "invoke" else "invokeExact"
    } yield 
      s"""|public static $returnType $methodName(MethodHandle mh$parameterSection) throws Throwable {
          |  return ($returnType) mh.$invoker($callSection);
          |}""".stripMargin
  }

  def specializationArity: T[Int]

  def generateSpecializations: T[Seq[PathRef]] = T{
    for{
      i <- 0 until specializationArity()
    } yield {
      val dest = T.dest / s"MethodHandleArity$i.java"
      os.write(dest, callClassGen(i))
      PathRef(dest)
    }
  }

  override def generatedSources = T{
    super.generatedSources() ++ generateSpecializations()
  }
}