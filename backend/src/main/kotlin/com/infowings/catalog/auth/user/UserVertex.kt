package com.infowings.catalog.auth.user

import com.infowings.catalog.common.User
import com.infowings.catalog.common.UserRole
import com.infowings.catalog.storage.OrientClass
import com.infowings.catalog.storage.checkClass
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.OVertex

const val HISTORY_USER_EDGE = "UserHistoryEdge"

fun OVertex.toUserVertex(): UserVertex {
    checkClass(OrientClass.USER)
    return UserVertex(this)
}

data class UserVertex(private val vertex: OVertex) : OVertex by vertex {
    var username: String
        get() = this["username"]
        set(value) {
            this["username"] = value
        }

    var password: String
        get() = this["password"]
        set(value) {
            this["password"] = value
        }

    var role: String
        get() = this["role"]
        set(value) {
            this["role"] = value
        }

    var blocked: Boolean
        get() = this["blocked"] ?: false
        set(value) {
            this["blocked"] = value
        }

    fun toUser(): User = User(username, password, UserRole.valueOf(role), blocked)
}