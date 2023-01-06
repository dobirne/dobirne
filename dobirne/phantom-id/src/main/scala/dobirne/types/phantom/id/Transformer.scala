package dobirne.types.phantom.id

trait Transformer[FROM, TO] {
  def map(from: FROM): TO
}
