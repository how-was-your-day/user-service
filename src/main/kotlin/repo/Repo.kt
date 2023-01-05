package repo

import model.User

interface Repo<T, TempT, ID> {
    fun get(id: ID): User?
    fun all(): List<T>
    fun remove(id: ID): Boolean
    fun removeAll(): Boolean
    fun add(t: TempT) : T
}
