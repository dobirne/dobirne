package dobirne.types.phantom.id

trait FromString[T] {
  def fromString(s: String): Option[T]
}
