package org.moten.david.lifty;
package snippet {

  import _root_.net.liftweb.http._
  import S._
  import _root_.net.liftweb.util._
  import Helpers._
  import _root_.scala.xml._
  import js._
  import JsCmds._
  import JE._

  class Times {
    object date extends RequestVar("1 Jan 2010")
    object start extends RequestVar("0830")
    object finish extends RequestVar("1230")

    def add(xhtml: NodeSeq): NodeSeq = {
      def processEntryAdd() {
      }
      bind("entry", xhtml,
        "date" -%> SHtml.text(date, date(_)),
        "start" -%> SHtml.text(start, start(_)),
        "finish" -%> SHtml.text(finish, finish(_)),
        "submit" -> SHtml.submit("Add", processEntryAdd))
    }
  }

  object Times {
    var number: Double = 0
  }
}
