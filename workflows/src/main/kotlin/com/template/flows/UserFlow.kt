package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.GenderEnum
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class UserFlow (private val fname: String,
                private val lname: String,
                private val age: Int,
                private val gender: GenderEnum,
                private val counterParty: Party): FlowLogic<SignedTransaction>() {

    fun userState(): UserState {
        return UserState(
                firstname = fname,
                lastname = lname,
                age =age,
                gender = gender,
                node = ourIdentity,
                linearId = UniqueIdentifier(),
                participants = listOf(ourIdentity, counterParty)
        )
    }


    @Suspendable
    override fun call(): SignedTransaction {
        val transaction: TransactionBuilder = transaction()
        val signedTransaction: SignedTransaction = verifyAndSign(transaction)
        val sessions: List<FlowSession> = (userState().participants - ourIdentity).map { initiateFlow(it) }.toSet().toList()
        val transactionSignedByAllParties: SignedTransaction = collectSignature(signedTransaction, sessions)
        return recordTransaction(transactionSignedByAllParties, sessions)
    }

        private fun transaction(): TransactionBuilder {

            val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()
            val issueCommand = Command(UserContract.Commands.Issue(),userState().participants.map { it.owningKey })
            val builder = TransactionBuilder(notary = notary)
            builder.addOutputState(userState(), UserContract.ID)
            builder.addCommand(issueCommand)

            return builder
    }
    private fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }
        @Suspendable
    private fun collectSignature(
            transaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

    @Suspendable
    private fun recordTransaction(transaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction =
            subFlow(FinalityFlow(transaction, sessions))
}

@InitiatedBy(UserFlow::class)
class UserFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {

            }
        }
        val signedTransaction = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedTransaction.id))
    }
}
