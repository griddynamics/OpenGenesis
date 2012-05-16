
package com.griddynamics.genesis.users.repository

import com.griddynamics.genesis.users.model.LocalUser
import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.api.{UserGroup, User}

trait UserGroupManagement {
    def usersForGroup(id: Int) : Option[List[User]]
    def groupsForUser(username: String) : Iterable[UserGroup]
    def addUserToGroup(id: Int, username: String)
    def removeUserFromGroup(id: Int, username: String)
    def removeAllUsersFromGroup(id: Int)
    def search(nameLike: String): List[UserGroup]
}

trait LocalUserGroupManagement extends UserGroupManagement {

    def groupsForUser(username: String) : Iterable[UserGroup] = {
      from(LocalUserSchema.users)(u => where(u.username === username).select(u))
        .headOption
        .map(_.groups.map(LocalGroupRepository.convert(_)))
        .getOrElse(List())
    }

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

    def removeAllUsersFromGroup(id: Int) {
        LocalUserSchema.userGroupsRelation.deleteWhere(ug => ug.groupId === id)
    }
}
