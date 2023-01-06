package dobirne.types.phantom.id

import java.util.UUID
import scala.util.Random

trait RandomGenInstances {

  def randomStringLengthGen(length: Int): RandomGen[String] =
    new RandomGen[String] {
      override def random: String = {
        import java.nio.charset.Charset
        val array = new Array[Byte](length)
        new Random().nextBytes(array)
        new String(array, Charset.forName("UTF-8"))
      }
    }

  implicit val intGen: RandomGen[Int] = new RandomGen[Int] {
    override def random: Int = math.random().toInt
  }

  implicit val uuidGen: RandomGen[UUID] = new RandomGen[UUID] {
    override def random: UUID = UUID.randomUUID()
  }

  implicit val longGen: RandomGen[Long] = new RandomGen[Long] {
    override def random: Long = math.random().toLong
  }

  implicit val tenCharStringGen: RandomGen[String] = randomStringLengthGen(10)
}

trait TransformerIdentity {
  implicit def identity[T]: Transformer[T, T] = (from: T) => from
}

object implicits extends RandomGenInstances with TransformerIdentity
