
package com.griddynamics.genesis.users.repository

import com.griddynamics.genesis.users.model.LocalUser
import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.api.User

trait UserGroupManagement{
    def usersForGroup(id: Int) : Option[List[User]]
    def addUserToGroup(id: Int, username: String)
    def removeUserFromGroup(id: Int, username: String)

}

trait LocalUserGroupManagement extends UserGroupManagement {
    def usersForGroup(id: Int) = {
        from(LocalUserSchema.groups)(group => where(group.id === id).select(group)).headOption.map(g => g.users.map(LocalUserRepository.convert(_)).toList)
    }

    def addUserToGroup(id: Int, username: String) {
        withSelectUser(username) {
            u => from(LocalUserSchema.groups)(group => where(group.id === id).select(group)).headOption.map(g => g.users.associate(u))
        }
    }

    def withSelectUser(user: String)(block: (LocalUser) => Any) = {
        from(LocalUserSchema.users)(u => where(u.username === user).select(u))
          .headOption.map(u => block(u))
    }

    def removeUserFromGroup(id: Int, username: String) {
        withSelectUser(username) {
            u => from(LocalUserSchema.groups)(group => where(group.id === id).select(group)).headOption.map(g => g.users.dissociate(u))
        }
    }
}
