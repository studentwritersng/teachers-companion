package com.example.api

import android.app.Activity
import android.util.Log
import com.example.BuildConfig
import co.paystack.android.Paystack
import co.paystack.android.Transaction
import co.paystack.android.model.Charge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PaystackManager(private val activity: Activity) {

    private val publicKey: String
        get() = try {
            BuildConfig.PAYSTACK_PUBLIC_KEY
        } catch (_: Exception) { "" }

    fun isConfigured(): Boolean {
        return publicKey.isNotBlank() && publicKey != "YOUR_PAYSTACK_PUBLIC_KEY"
    }

    fun generateReference(): String {
        val datePart = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val uuidPart = UUID.randomUUID().toString().take(8)
        return "TC-$datePart-$uuidPart"
    }

    fun chargeCard(
        email: String,
        amountInKobo: Int,
        reference: String,
        onSuccess: (Transaction) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isConfigured()) {
            onError("Paystack not configured. Add PAYSTACK_PUBLIC_KEY to .env")
            return
        }

        try {
            Paystack.setPublicKey(publicKey)

            val charge = Charge()
            charge.email = email
            charge.amount = amountInKobo
            charge.reference = reference

            Paystack.chargeCard(activity, charge, object : Paystack.TransactionCallback {
                override fun onSuccess(transaction: Transaction?) {
                    Log.i("PaystackManager", "Payment success: ${transaction?.reference}")
                    if (transaction != null) {
                        onSuccess(transaction)
                    } else {
                        onError("Payment completed but no reference returned")
                    }
                }

                override fun onError(error: Throwable?, transaction: Transaction?) {
                    Log.e("PaystackManager", "Payment error: ${error?.message}")
                    onError(error?.message ?: "Payment failed")
                }
            })
        } catch (e: Exception) {
            Log.e("PaystackManager", "Paystack init error", e)
            onError(e.message ?: "Failed to initialize payment")
        }
    }
}
