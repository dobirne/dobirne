package dobirne.types.phantom.id

trait RandomGen[T] {
  def random: T
}
