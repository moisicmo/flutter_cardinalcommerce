package com.holu.cardinal

import android.content.Intent
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalRenderType
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import com.cardinalcommerce.shared.userinterfaces.*
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.*
import org.json.JSONArray
// import org.json.JSONObject

class MainActivity : FlutterFragmentActivity() {
  private val CHANNEL = "com.holu.cardinal/cardinal"

  private lateinit var cardinal: Cardinal

  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)

    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
        call,
        result ->
      when (call.method) {
        "configureCardinal" -> {
          val serverJwt = call.argument<String>("serverJwt")
          if (serverJwt != null && serverJwt.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
              try {
                val consumerSessionId = configureCardinal(serverJwt)
                result.success(consumerSessionId)
              } catch (e: Exception) {
                result.error("CONFIGURATION_ERROR", "Failed to configure Cardinal", e.message)
              }
            }
          } else {
            result.error("INVALID_ARGUMENT", "serverJwt is required", null)
          }
        }
        "handleAuthenticationResponse" -> {
          val threeDSVersion = call.argument<String>("threeDSVersion")
          val enrolled = call.argument<String>("enrolled")
          val paResStatus = call.argument<String>("paResStatus")
          val transactionId = call.argument<String>("transactionId")
          val payload = call.argument<String>("payload")

          if (threeDSVersion != null && enrolled != null && paResStatus != null) {
            handleAuthenticationResponse(
                threeDSVersion,
                enrolled,
                paResStatus,
                transactionId,
                payload
            )
            result.success(null)
          } else {
            result.error("INVALID_ARGUMENT", "Missing required fields", null)
          }
        }
        else -> result.notImplemented()
      }
    }
  }

  private suspend fun configureCardinal(serverJwt: String): String =
      suspendCoroutine { continuation ->
        cardinal = Cardinal.getInstance()

        val cardinalConfigurationParameters = CardinalConfigurationParameters()
        cardinalConfigurationParameters.environment = CardinalEnvironment.STAGING
        cardinalConfigurationParameters.requestTimeout = 8000
        cardinalConfigurationParameters.challengeTimeout = 5

        val rTYPE = JSONArray()
        rTYPE.put(CardinalRenderType.OTP)
        rTYPE.put(CardinalRenderType.SINGLE_SELECT)
        rTYPE.put(CardinalRenderType.MULTI_SELECT)
        rTYPE.put(CardinalRenderType.OOB)
        rTYPE.put(CardinalRenderType.HTML)
        cardinalConfigurationParameters.renderType = rTYPE

        cardinalConfigurationParameters.uiType = CardinalUiType.BOTH
        cardinalConfigurationParameters.isLocationDataConsentGiven = true

        val yourUICustomizationObject = UiCustomization()
        cardinalConfigurationParameters.uiCustomization = yourUICustomizationObject

        cardinal.configure(this, cardinalConfigurationParameters)

        cardinal.init(
            serverJwt,
            object : CardinalInitService {

              override fun onSetupCompleted(consumerSessionId: String) {
                println("Cardinal init setup completed. Consumer session ID: $consumerSessionId")
                continuation.resume(consumerSessionId)
              }

              override fun onValidated(validateResponse: ValidateResponse, serverJwt: String?) {
                if (serverJwt == null) {
                  continuation.resumeWithException(
                      Exception("Validation failed: ${validateResponse.errorDescription}")
                  )
                } else {
                  println(
                      "Cardinal init validated. Server JWT: $serverJwt, validateResponse: $validateResponse"
                  )
                }
              }
            }
        )
      }

  private fun handleAuthenticationResponse(
      threeDSVersion: String,
      enrolled: String,
      paResStatus: String,
      transactionId: String?,
      payload: String?
  ) {
    // Verificar que enrolled sea "Y" y que threeDSVersion empiece con "2"
    if (enrolled != "Y" || !threeDSVersion.startsWith("2")) {
      println("Authentication response does not meet requirements. Canceling operation.")
      return
    }
    if (paResStatus == "Y") {
      // Autenticación exitosa, procede con el siguiente paso en el flujo de pago
      proceedWithPayment()
    } else if (paResStatus == "C") {
      // Se requiere un desafío adicional, inicia el desafío
      if (transactionId != null && payload != null) {
        startChallenge(transactionId, payload)
      } else {
        println("Missing transactionId or payload for challenge")
      }
    } else {
      // Manejar otros casos de PAResStatus si es necesario
      handleOtherPAResStatus(paResStatus)
    }
  }

  private fun proceedWithPayment() {
    // Implementa la lógica para proceder con el pago
    println("Procediendo con el pago...")
  }

  private fun startChallenge(transactionId: String, payload: String) {
    val intent =
        Intent(this, ChallengeActivity::class.java).apply {
          putExtra("transactionId", transactionId)
          putExtra("payload", payload.toString())
        }
    startActivity(intent)
  }

  private fun handleOtherPAResStatus(paResStatus: String) {

    println("Manejo de otras PAResStatus: $paResStatus")
  }
}
