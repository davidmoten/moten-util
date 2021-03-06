package org.moten.david.sowpods

object DbCreator {

  def main(args: Array[String]) {
    create
  }

  import scala.io.Source
  import java.sql._
  import java.io.File

  def exec(sql: String)(implicit stmt: Statement) {
    stmt.executeUpdate(sql)
  }

  def create {
    println("loading words")
    val words = Source
      .fromInputStream(DbCreator.getClass().getResourceAsStream("/sowpods.txt"))
      .getLines
      .map(_.trim).filter(_.length > 0)
      .toSet.toList.sorted

    new File("target/generated-resources").mkdirs

    println("creating database")
    Class.forName("org.sqlite.JDBC")
    val connection = DriverManager.getConnection("jdbc:sqlite:target/generated-resources/word.db")
    implicit val stmt = connection.createStatement
    exec("drop table if exists android_metadata")
    exec("create table android_metadata (locale TEXT DEFAULT 'en_US');")
    exec("insert into android_metadata VALUES ('en_US');")
    exec("drop table if exists word;")
    exec("create table word(_id integer primary key autoincrement,word text,word_sorted text);")
    val pstmt = connection.prepareStatement("insert into word(word,word_sorted) values (?,?);")
    connection.setAutoCommit(false)
    var count = 0
    words.foreach(w => {
      count += 1
      //      pstmt.setInt(1 count)
      pstmt.setString(1, w)
      pstmt.setString(2, w.sorted)
      pstmt.addBatch
    })
    pstmt.executeBatch
    connection.commit
    exec("create unique index idx_word on word(word asc);")
    exec("create index idx_word_sorted on word(word_sorted asc);")
    connection.commit
    connection.close

    println("created database")
  }

}