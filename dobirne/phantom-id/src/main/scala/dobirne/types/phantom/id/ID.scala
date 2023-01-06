package dobirne.types.phantom.id

case class ID[Tag, V](value: V) extends AnyVal {
  def convertToID[S](implicit transformer: Transformer[V, S]): ID[Tag, S] = ID(transformer.map(this.value))
  def asID[Tag2]: ID[Tag2, V] = ID[Tag2, V](value)
}

object ID {
  def fromString[Tag, V](s: String)(implicit parser: FromString[V]): Option[ID[Tag, V]] =
    parser.fromString(s).map(ID(_))

  def random[Tag, V](implicit gen: RandomGen[V]): ID[Tag, V] = ID(gen.random)

  def random[Tag, V, S](seed: S)(implicit gen: RandomSeedGen[S, V]): ID[Tag, V] = ID(gen.random(seed))

  def fromID[Tag, V, S](id: ID[_, S])(implicit transformer: Transformer[S, V]): ID[Tag, V] =
    ID(transformer.map(id.value))

}
