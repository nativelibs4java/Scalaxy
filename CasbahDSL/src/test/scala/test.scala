import org.junit._
import org.junit.Assert._

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.query.dsl._

import scalaxy.casbah._

class QueryTest {

  def sameQuery(expected: DBObject, actual: DBObject) =
    assertEquals(expected, actual)

  @Test
  def testQuery {
    sameQuery(
      $and("blah" $lt 10, "foo" $eq "bar"),
      query(d => d.blah < 10 && d.foo == "bar"))

    sameQuery(
      "a.b.c" $eq 10,
      query(_.a.b.c == 10))

    sameQuery(
      "blah" $mod (2, 1),
      query(d => d.blah % 2 == 1))

    sameQuery(
      $or("blah" $exists true, "foo" $exists false),
      query(d => d.contains("blah") || !d.contains("foo"))) 

    sameQuery(
      $or("blah" $mod (2, 1), "foo" $gt "bar"),
      query(d => d.blah % 2 == 1 || d.foo > d.bar))

    sameQuery(
      "foo" $lt "bar",
      query(_.foo < "bar"))

    sameQuery(
      $inc("foo" -> 10),
      update(_.foo += 10))

    sameQuery(
      $set("a" -> 10, "b" -> 20),
      update(d => d.set(a = 10, b = 20)))

    sameQuery(
      $set("a" -> 10),
      update(d => d.a = 10))

    sameQuery(
      $setOnInsert("a" -> 10, "b" -> 20),
      update(d => d.setOnInsert(a = 10, b = 20)))

    // sameQuery(
    //   $set("a" -> 10, "" -> 20),
    //   update(d => d.set(a = 10, 20)))

    sameQuery(
      $rename("a" -> "blah", "b" -> "hooh"),
      update(d => d.rename(a = "blah", b = "hooh")))

    sameQuery(
      "a".$type[BasicDBList],
      update(d => d.a.isInstanceOf[BasicDBList]))

    sameQuery(
      $or("foo" $lt "bar", "foo" $size 4),
      query(d => d.foo < "bar" || d.foo.size == 4))
  }

  @Ignore 
  @Test
  def testConnection {
    // val client =  MongoClient()

    def makeEntry(word: String) =
      $set ( (word + "_").iterator.mkString(".") -> word )

    val dict = MongoConnection("localhost")("appdb")("dict")
    val index = MongoDBObject("_id" -> "_index")
    dict.update(index, makeEntry("applaud"), true, false)
    dict.update(index, makeEntry("apple"), true, false)
    dict.update(index, makeEntry("bad"), true, false)
    dict.update(index, makeEntry("banana"), true, false)

    val entry = dict.findOne(index ++ ("a.p.p" $exists true))
    println(entry)
  }
}
