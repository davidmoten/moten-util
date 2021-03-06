import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.io.IOException
import java.net.Socket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.io.File
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
package checkable {

  import scala.collection.JavaConversions._
  import java.net.URL
  import scala.collection.immutable.Map

  object PropertiesUtil {

    def propertiesToMap(url: URL) = {
      val p = new java.util.Properties();
      val is = url.openStream();
      p.load(is);
      is.close
      p.entrySet()
        .map(x => (x.getKey.toString, x.getValue.toString))
        .toMap
    }

    type Properties = Map[String, String]
    type BooleanExpression = Function0[Boolean]
    type PropertiesProvider = Function0[Properties]
    type ReturnsResult = Function0[Result]
  }

  import PropertiesUtil._

  object NumericExpression {
    def now = NumericExpression(() => System.currentTimeMillis(), () => "now")
  }

  case class NumericExpression(f: () => BigDecimal, string: () => String) extends Function0[BigDecimal] {

    def apply = f()

    def >(n: NumericExpression): BooleanExpression =
      BooleanExpression(() => apply > n.apply, () => this + " > " + n)
    def <(n: NumericExpression): BooleanExpression =
      BooleanExpression(() => apply < n.apply, () => this + " < " + n)
    def >=(n: NumericExpression): BooleanExpression =
      BooleanExpression(() => apply >= n.apply, () => this + " >= " + n)
    def <=(n: NumericExpression): BooleanExpression =
      BooleanExpression(() => apply <= n.apply, () => this + " <= " + n)
    def *(n: NumericExpression): NumericExpression =
      NumericExpression(() => apply * n.apply, () => this + " * " + n)
    def +(n: NumericExpression): NumericExpression =
      NumericExpression(() => apply + n.apply, () => this + " + " + n)
    def -(n: NumericExpression): NumericExpression =
      NumericExpression(() => apply - n.apply, () => this + " - " + n)
    def /(n: NumericExpression): NumericExpression =
      NumericExpression(() => apply / n.apply, () => this + " / " + n)
    def equals(n: NumericExpression, precision: BigDecimal) =
      BooleanExpression(() => (apply - n.apply).abs <= precision, () => this + " = " + n)
    def ==(n: NumericExpression, precision: BigDecimal) = equals(n, precision)
    def empty: BooleanExpression =
      BooleanExpression(() => apply() == null, () => this + " is empty")
    def notEmpty: BooleanExpression =
      BooleanExpression(() => apply() != null, () => this + " is not empty")
    def milliseconds = NumericExpression(f, () => this + " ms")
    def seconds = NumericExpression(() => 1000 * this(), () => this + " seconds")
    def minutes = NumericExpression(() => 60 * seconds(), () => this + " minutes")
    def hours = NumericExpression(() => 60 * minutes(), () => this + " hours")
    def days = NumericExpression(() => 24 * hours(), () => this + " days")
    def weeks = NumericExpression(() => 7 * days(), () => this + " weeks")
    override def toString = string()
  }

  case class BooleanExpression(f: () => Boolean, string: () => String) extends Function0[Boolean] {

    def apply = f.apply()
    def or(e: BooleanExpression) =
      BooleanExpression(() => f.apply || e.apply, () => toString + " or " + e.toString)
    def and(e: BooleanExpression) =
      BooleanExpression(() => f.apply || e.apply, () => toString + " and " + e.toString)
    def not() =
      BooleanExpression(() => !f.apply, () => "not " + toString)
    def && = and _
    def || = or _
    override def toString = string()
  }

  object BooleanExpression {

    def urlAvailable(url: String) = BooleanExpression(
      () =>
        try {
          val u = new URL(url)
          val con = u.openConnection()
          con match {
            case http: HttpURLConnection => {
              http.setConnectTimeout(2000)
              http.connect()
              val code = http.getResponseCode()
              val ok = (code >= 200 && code <= 299) ||
                (code >= 300 && code <= 399)
              http.disconnect()
              ok
            }
            case _ => {
              con.getInputStream().close()
              true
            }
          }
        } catch {
          case e: MalformedURLException =>
            throw new RuntimeException(e)
          case e: IOException =>
            false
        }, () => "UrlAvailable(" + url + ")")

    def socketAvailable(host: String, port: Int, timeoutMs: Long) = BooleanExpression(
      () => {
        val socket = new Socket()
        try {
          val inetAddress = InetAddress.getByName(host)
          val socketAddress = new InetSocketAddress(inetAddress,
            port)

          // this method will block no more than timeout ms.
          socket.connect(socketAddress, timeoutMs.intValue())
          //socket available
          true
        } catch {
          case e: UnknownHostException => false
          case e: IOException => false
        } finally {
          if (socket != null)
            try {
              socket.close();
            }
        }
      },
      () => "SocketAvailable(" + host + ":" + port + ")")

    def fileExists(file: File) =
      BooleanExpression(() => file.exists, () => "FileExists(" + file + ")")
  }

  trait Level

  trait Result

  case class Passed extends Result
  case class Failed extends Result
  case class Unknown extends Result
  case class ExceptionOccurred(throwable: Throwable) extends Unknown

  trait Policy

  trait Checkable extends ReturnsResult {
    val name: String
    val description: String
    val level: Level
    val policies: Set[Policy]
  }

  class UrlPropertiesProvider(url: URL) extends PropertiesProvider {
    def apply(): Properties = PropertiesUtil.propertiesToMap(url)
  }

  trait PropertiesFunction extends ReturnsResult {

    val properties: () => Properties

    implicit def toNumeric(key: String) =
      NumericExpression(() => properties().get(key) match {
        case None => null
        case x: Option[String] => BigDecimal(x.get)
      }, () => key)

    implicit def bigDecimalToNumeric(x: BigDecimal) =
      NumericExpression(() => x, () => x.toString)

    implicit def doubleToNumeric(x: Double) =
      NumericExpression(() => BigDecimal(x), () => x.toString)

    implicit def integerToNumericExpression(x: Int) =
      NumericExpression(() => BigDecimal(x), () => x.toString)

    def apply = {
      try {
        if (expression())
          Passed()
        else
          Failed()
      } catch {
        case e: Throwable => ExceptionOccurred(e)
      }
    }

    def expression: BooleanExpression

    override def toString = expression.toString
  }

  trait WebAppPropertiesFunction
    extends PropertiesFunction {
    def webappBase: String
    def webapp: String
    val propertiesUrl = new URL(webappBase + "/" + webapp + "/properties")
    val properties = new UrlPropertiesProvider(propertiesUrl)
  }

}

package monitoring {

  import scala.actors.Actor
  import scala.actors.Actor._

  class MonitoringProperties {
    import MonitoringPropertiesActor._

    private val actor = new MonitoringPropertiesActor
    private def unexpected = throw new RuntimeException("unexpected")
    def put(key: String, value: String) = actor ! Put(key, value)
    def get(key: String) = actor !? Get(key) match {
      case x: String => x
      case _ => unexpected
    }
    def put(key: String, value: java.util.Date): Unit = put(key, value.getTime())
    def put(key: String, value: Any): Unit = put(key, value.toString)
    def put(map: Map[String, String]): Unit = { actor ! PutAll(map) }
    def getNumber(key: String) = BigDecimal(get(key))
    def reset = {
      actor ! Reset()
      put("application.started.epoch.ms",
        System.currentTimeMillis())
    }
    def getAll = actor !? GetAll() match {
      case x: Map[String, String] => x
      case _ => unexpected
    }
  }
  object MonitoringProperties {
    val instance = new MonitoringProperties
  }

  object MonitoringPropertiesActor {
    case class Put(key: String, value: String)
    case class PutAll(map: Map[String, String])
    case class Get(key: String)
    case class GetAll
    case class Reset
    case class Initialize
  }

  class MonitoringPropertiesActor extends Actor {
    //use an Actor to handle concurrent access to the internal properties object safely
    import MonitoringPropertiesActor._

    def act =
      {
        var properties = Map[String, String]()
        loop {
          react {
            case x: Put => properties += x.key -> x.value
            case x: PutAll => properties ++= x.map
            case x: Get => reply(properties.get(x.key))
            case x: Reset => properties = Map()
            case x: GetAll => reply(properties)
          }
        }
      }
  }

  class MonitoringPropertiesServlet extends HttpServlet {
    override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
      val properties = MonitoringProperties.instance.getAll
      val p = new java.util.Properties()
      properties.foreach { case (k, v) => p.setProperty(k, v) }
      p.store(response.getOutputStream(), null)
    }
  }

}

package amsa {

  import checkable._
  import BooleanExpression._

  object AmsaCheckable {
    def instance = this
    val lastProcessDuration = "last.process.duration.ms"
    val lastRunTime = "last.run.time";
    val applicationStartedTime = "application.started.time"
  }

  trait AmsaCheckable extends Checkable {
    val wikiBase = "http://wiki.amsa.gov.au/index.php?title="
    val wikiTitle: String
    def infoUrl: String = wikiBase + wikiTitle
  }

  trait AmsaWebAppCheckable extends WebAppPropertiesFunction
    with AmsaCheckable {
    def time(s: String): NumericExpression = {
      (s + ".epoch.ms")
    }
    override def webappBase = "http://sardevc.amsa.gov.au:8080"
    def webappAvailable(webapp: String) =
      urlAvailable(webappBase + "/" + webapp)
  }

  case class Ok extends Level
  case class Warning extends Level
  case class Failure extends Level

  case class FixNextWorkingDay extends Policy
  case class FixImmediate extends Policy
  case class NotifyOncall extends Policy
  case class NotifyOperators extends Policy
  case class NotifyAdministrator extends Policy

  import NumericExpression._
  import AmsaCheckable._

  object MyPropertiesProvider
    extends UrlPropertiesProvider(
      PropertiesUtil.getClass().getResource("/test.properties"))

  class SampleWebAppCheckable extends AmsaWebAppCheckable() {
    val wikiTitle = "Sample_Web_App"
    override def webapp = "sample"
    val name = "sample last processing duration time"
    val description = "processing duration time is acceptable"
    val level: Level = Warning()
    val policies: Set[Policy] = Set(FixNextWorkingDay(),
      NotifyAdministrator(), NotifyOncall())
    val expression =
      ((lastProcessDuration empty)
        or ((lastProcessDuration hours) < 0.5)) and
        (((time(lastRunTime) empty)
          and
          (time(applicationStartedTime) < (now - (1 hours))))
          or
          (time(lastRunTime) > (now - (2 days))))
  }

  class CtsAvailCheckable extends AmsaWebAppCheckable {
    val wikiTitle = "CTS"
    override def webapp = "cts"
    val name = "cts"
    val description = "cts base url is available"
    val level: Level = Failure()
    val policies: Set[Policy] = Set(FixImmediate(), NotifyOncall())
    val expression = webappAvailable(webapp)
  }

  class GoogleCheckable extends AmsaWebAppCheckable {
    val wikiTitle = "Google"
    override def webappBase = "http://www.google.com"
    override def webapp = ""
    val name = "Google"
    val description = "Google base url is available"
    val level: Level = Failure()
    val policies: Set[Policy] = Set(FixImmediate(), NotifyOncall())
    val expression = socketAvailable("localhost", 22, 20)
  }
}