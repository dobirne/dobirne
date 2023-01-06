package dobirne.types.phantom

import java.util.UUID

package object id {
  type IID[A] = ID[A, Int]
  type LID[A] = ID[A, Long]
  type SID[A] = ID[A, String]
  type UID[A] = ID[A, UUID]
}
