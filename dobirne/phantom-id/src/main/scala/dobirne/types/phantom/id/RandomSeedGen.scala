package dobirne.types.phantom.id

trait RandomSeedGen[S, T] {
  def random(seed: S): T
}
