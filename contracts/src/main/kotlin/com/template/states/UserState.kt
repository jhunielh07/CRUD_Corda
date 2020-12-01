package com.template.states

import com.template.GenderEnum
import com.template.contracts.UserContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(UserContract::class)
class UserState(
        val firstname: String,
        val lastname: String,
        val age: Int,
        val gender: GenderEnum,
        val node: Party,
        val delete: Boolean = false,
        override val linearId: UniqueIdentifier,
        override val participants: List<Party>
        ) : LinearState
