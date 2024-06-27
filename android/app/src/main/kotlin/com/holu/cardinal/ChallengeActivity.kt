package com.holu.cardinal

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver
import org.json.JSONObject

class ChallengeActivity : FragmentActivity() {
  private lateinit var cardinalValidateReceiver: CardinalValidateReceiver
  private lateinit var cardinalChallengeObserver: CardinalChallengeObserver
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val transactionId = intent.getStringExtra("transactionId") ?: return
    val payload = intent.getStringExtra("payload") ?: return

    val challengeObserver =
        CardinalChallengeObserver(
            this,
            object : CardinalValidateReceiver {
              override fun onValidated(
                  context: Context?,
                  validateResponse: ValidateResponse,
                  responseString: String?
              ) {
                when (validateResponse.getActionCode()!!) {
                  CardinalActionCode.SUCCESS -> {

                    println("Challenge SUCCESS")
                    finish()
                  }
                  CardinalActionCode.CANCEL -> {

                    println("Challenge CANCEL")
                    finish()
                  }
                  CardinalActionCode.NOACTION -> {

                    println("Challenge NOACTION")
                    finish()
                  }
                  CardinalActionCode.FAILURE -> {

                    println("Challenge FAILURE")
                    finish()
                  }
                  CardinalActionCode.ERROR -> {

                    println("Challenge ERROR")
                    finish()
                  }
                  CardinalActionCode.TIMEOUT -> {

                    println("Challenge TIMEOUT")
                    finish()
                  }
                }
              }
            }
        )

    try {
      Cardinal.getInstance().cca_continue(transactionId, payload, challengeObserver)
    } catch (e: Exception) {
      // Manejar excepción
      println("Exception during cca_continue: ${e.message}")
      finish()
    }
  }
}
